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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthRequestWithAgentInfo}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.controllers.{ToFuture, routes}
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details.apply_sa_code_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplySACodeChangesController @Inject()(
                                              actions: Actions,
                                              val sessionCache: SessionCacheService,
                                              val acaConnector: AgentClientAuthorisationConnector,
                                              applySaCodeChangesView: apply_sa_code_changes,
                                              pcodRepository: PendingChangeOfDetailsRepository
                                            )(implicit appConfig: AppConfig,
                                              cc: MessagesControllerComponents,
                                              ec: ExecutionContext) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      println(s"${Console.MAGENTA} Wojciech showPage ifFeatureEnabledAndNoPendingChanges pass  ${Console.RESET}")
      Future.successful(Ok(applySaCodeChangesView(UpdateDetailsForms.applySaCodeChangesForm)))
    }
  }

  def  onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
//      withUpdateAmlsJourney { amlsJourney =>
        UpdateDetailsForms.applySaCodeChangesForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(applySaCodeChangesView(formWithErrors))),
//            applySaCodeChanges => {
//              updateDraftDetails(_.copy(agencyName = Some(newAgencyName))).map(_ =>
//                Redirect (uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.EnterSACodeController.showPage)
//              )
//            }

            _ => Redirect (uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.EnterSACodeController.showPage).toFuture
          )
      }
//    }
  }


  private def ifFeatureEnabled(action: => Future[Result]): Future[Result] = {
    if (appConfig.enableChangeContactDetails) action else Future.successful(NotFound)
  }

  private def ifFeatureEnabledAndNoPendingChanges(action: => Future[Result])(implicit request: AuthRequestWithAgentInfo[_]): Future[Result] = {
    ifFeatureEnabled {
      pcodRepository.find(request.agentInfo.arn).flatMap {
        case None => // no change is pending, we can proceed
          println(s"${Console.MAGENTA} Wojciech ifFeatureEnabledAndNoPendingChanges None ${Console.RESET}")
          action
        case Some(_) => // there is a pending change, further changes are locked. Redirect to the base page
          println(s"${Console.MAGENTA} Wojciech ifFeatureEnabledAndNoPendingChanges: Some  ${Console.RESET}")
          Future.successful(Redirect(routes.ContactDetailsController.showCurrentContactDetails))
      }
    }
  }
}

