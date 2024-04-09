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
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.models.{PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util._
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesignatoryDetails, YourDetails}

import java.time.Instant
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(actions: Actions,
                                           sessionCache: SessionCacheService,
                                           acaConnector: AgentClientAuthorisationConnector,
                                           pcodRepository: PendingChangeOfDetailsRepository,
                                           checkUpdatedDetailsView: check_updated_details,
                                        )(implicit appConfig: AppConfig,
                                          cc: MessagesControllerComponents,
                                          ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with Logging {

  private def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  private def ifFeatureEnabledAndNoPendingChanges(action: => Future[Result])(implicit request: AuthRequestWithAgentInfo[_]): Future[Result] = {
    ifFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          Future.successful(Redirect(desiDetails.routes.ContactDetailsController.showCurrentContactDetails))
      }
    }
  }

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
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
        case None => Redirect(desiDetails.routes.ContactDetailsController.showCurrentContactDetails)
      }
    }
  }

  val onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      val arn = request.agentInfo.arn
      sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS).flatMap {
        case None => // graceful redirect in case of expired session data etc.
          Future.successful(Redirect(desiDetails.routes.ContactDetailsController.showCurrentContactDetails))
        case Some(details) => for {
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
          //
          // TODO actual connector call to submit the details goes here...
          //
          _ <- pcodRepository.insert(pendingChange)
          _ <- sessionCache.delete(DRAFT_NEW_CONTACT_DETAILS)
          _ <- sessionCache.delete(DRAFT_SUBMITTED_BY)
        } yield Redirect(desiDetails.routes.ContactDetailsController.showChangeSubmitted)
      }
    }
  }
}
