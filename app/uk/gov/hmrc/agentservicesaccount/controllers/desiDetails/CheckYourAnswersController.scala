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
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util._
import uk.gov.hmrc.agentservicesaccount.models.{PendingChangeOfDetails, PendingChangeRequest}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesignatoryDetails, YourDetails}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(actions: Actions,
                                           val sessionCache: SessionCacheService,
                                           acaConnector: AgentClientAuthorisationConnector,
                                           agentAssuranceConnector: AgentAssuranceConnector,
                                           checkUpdatedDetailsView: check_updated_details,
                                           summary_pdf: summaryPdf
                                          )(implicit appConfig: AppConfig,
                                            cc: MessagesControllerComponents,
                                            ec: ExecutionContext,
                                            pcodRepository: PendingChangeRequestRepository
                                          ) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport with Logging {


  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      for {
        submittedBy <- sessionCache.get[YourDetails](DRAFT_SUBMITTED_BY)
        selectChanges <- sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES)
        desiDetailsData <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
      } yield desiDetailsData match {
        case Some(desiDetails) => Ok(checkUpdatedDetailsView(
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
      sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).flatMap {
        case None => // graceful redirect in case of expired session data etc.
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
        case Some(details) => for {
          selectChanges <- sessionCache.get[Set[String]](CURRENT_SELECTED_CHANGES)
          optUtr <- acaConnector.getAgentRecord().map(_.uniqueTaxReference)
          submittedBy <- sessionCache.get[YourDetails](DRAFT_SUBMITTED_BY)
          oldContactDetails <- CurrentAgencyDetails.get(acaConnector)
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
          result <- agentAssuranceConnector.postDesignatoryDetails(arn, java.util.Base64.getEncoder.encodeToString(htmlForPdf.getBytes()))
          _ <- pcodRepository.insert(PendingChangeRequest(arn, pendingChange.timeSubmitted))
          _ <- sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)
          _ <- sessionCache.delete(DRAFT_SUBMITTED_BY)
        } yield {
          Redirect(desiDetails.routes.ContactDetailsController.showChangeSubmitted)
        }
      }
    }
  }
}
