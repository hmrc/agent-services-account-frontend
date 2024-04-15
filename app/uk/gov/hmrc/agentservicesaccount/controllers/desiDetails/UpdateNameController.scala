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
import uk.gov.hmrc.agentservicesaccount.connectors.{AddressLookupConnector, AgentClientAuthorisationConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers._
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateNameController @Inject()(actions: Actions,
                                     sessionCache: SessionCacheService,
                                     acaConnector: AgentClientAuthorisationConnector,
                                     alfConnector: AddressLookupConnector,
                                     evConnector: EmailVerificationConnector,
                                     pcodRepository: PendingChangeOfDetailsRepository,
                                     agentClientAuthorisationConnector: AgentClientAuthorisationConnector,
                                     //views
                                     update_name: update_name,
                                     update_phone: update_phone,
                                     update_email: update_email,
                                     change_submitted: change_submitted,
                                     email_locked: email_locked,
                                     beforeYouStartPage: before_you_start_page
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
          Future.successful(Redirect(desiDetails.routes.ViewContactDetailsController.showPage))
      }
    }
  }

  // utility function.
  private def updateDraftDetails(f: DesignatoryDetails => DesignatoryDetails)(implicit request: Request[_]): Future[Unit] = for {
    mDraftDetailsInSession <- sessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)
    draftDetails <- mDraftDetailsInSession match {
      case Some(details) => Future.successful(details)
      // if there is no 'draft' new set of details in session, get a fresh copy of the current stored details
      case None =>
        acaConnector.getAgentRecord()
        .map(agencyDetails=>
          DesignatoryDetails(
            agencyDetails = agencyDetails.agencyDetails.getOrElse(throw new RuntimeException("No agency details on agent record")),
            otherServices = OtherServices(
              saChanges = SaChanges(
                applyChanges = false,
                saAgentReference = None),
              ctChanges = CtChanges(
                applyChanges = false,
                ctAgentReference = None
              ))))
    }
    updatedDraftDetails = f(draftDetails)
    _ <- sessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, updatedDraftDetails)
  } yield ()

  val showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(update_name(UpdateDetailsForms.businessNameForm)))
    }
  }

  val onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      UpdateDetailsForms.businessNameForm
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(Ok(update_name(formWithErrors))),
          newAgencyName => {
              updateDraftDetails(desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyName = Some(newAgencyName)))).flatMap(_ =>
                getNextPage(sessionCache, "businessName")
            )
          }
        )
    }
  }
}
