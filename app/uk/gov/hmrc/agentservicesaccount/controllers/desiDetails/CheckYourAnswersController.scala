/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util._
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeOfDetails
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.AuditService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.Instant
import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  agentAssuranceConnector: AgentAssuranceConnector,
  checkUpdatedDetailsView: check_updated_details,
  auditService: AuditService,
  summary_pdf: summaryPdf
)(implicit
  appConfig: AppConfig,
  cc: MessagesControllerComponents,
  val ec: ExecutionContext,
  pcodRepository: PendingChangeRequestRepository
)
extends FrontendController(cc)
with DesiDetailsJourneySupport
with I18nSupport
with Logging {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      for {
        submittedBy <- sessionCacheService.get[YourDetails](draftSubmittedByKey)
        selectChanges <- sessionCacheService.get[Set[String]](currentSelectedChangesKey)
        desiDetailsData <- sessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)
      } yield desiDetailsData match {
        case Some(desiDetails) =>
          Ok(checkUpdatedDetailsView(
            desiDetails.agencyDetails,
            request.agentInfo.isAdmin,
            desiDetails.otherServices,
            submittedBy.get,
            selectChanges.get
          ))
        case None => Redirect(desiDetails.routes.ViewContactDetailsController.showPage)
      }
    }
  }

  val onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      val arn = request.agentInfo.arn
      sessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey).flatMap {
        case None => // graceful redirect in case of expired session data etc.
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
        case Some(details) =>
          for {
            selectChanges <- sessionCacheService.get[Set[String]](currentSelectedChangesKey)
            optUtr <- agentAssuranceConnector.getAgentRecord.map(_.uniqueTaxReference)
            submittedBy <- sessionCacheService.get[YourDetails](draftSubmittedByKey)
            oldContactDetails <- agentAssuranceConnector.getAgentRecord.map(_.agencyDetails.getOrElse {
              throw new RuntimeException(s"Could not retrieve current agency details for ${request.agentInfo.arn} from the backend")
            })
            pendingChange = PendingChangeOfDetails(
              arn = arn,
              oldDetails = oldContactDetails,
              newDetails = details.agencyDetails,
              otherServices = details.otherServices,
              timeSubmitted = Instant.now(),
              submittedBy = submittedBy.getOrElse(throw new RuntimeException("Cannot submit without submittedBy details"))
            )
            htmlForPdf: String = summary_pdf(
              optUtr,
              pendingChange,
              selectChanges.getOrElse(throw new RuntimeException("Cannot submit without select changes details"))
            ).toString()
            _ = auditService.auditUpdateContactDetailsRequest(optUtr, pendingChange)
            result <- agentAssuranceConnector.postDesignatoryDetails(arn, java.util.Base64.getEncoder.encodeToString(htmlForPdf.getBytes()))
            _ <- pcodRepository.insert(PendingChangeRequest(arn, pendingChange.timeSubmitted))
            _ <- sessionCacheService.delete(draftNewContactDetailsKey)
            _ <- sessionCacheService.delete(draftSubmittedByKey)
          } yield {
            Redirect(desiDetails.routes.ContactDetailsController.showChangeSubmitted)
          }
      }
    }
  }

}
