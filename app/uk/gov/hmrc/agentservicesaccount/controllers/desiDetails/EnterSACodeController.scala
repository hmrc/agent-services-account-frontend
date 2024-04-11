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
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterSACodeController @Inject()(
                                       actions: Actions,
                                       val sessionCache: SessionCacheService,
                                       val acaConnector: AgentClientAuthorisationConnector,
                                       enterSaCodeView: enter_sa_code,
                                       pcodRepository: PendingChangeOfDetailsRepository
                                     )(implicit appConfig: AppConfig,
                                       cc: MessagesControllerComponents,
                                       ec: ExecutionContext) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport {

  def showPage: Action[AnyContent]= actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(enterSaCodeView(UpdateDetailsForms.saCodeForm)))
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { desiDetails =>
        UpdateDetailsForms.saCodeForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(enterSaCodeView(formWithErrors))),
            saCode => {
              updateDraftDetails(_.copy(otherServices = desiDetails.otherServices.copy(saChanges = SaChanges(true, Some(SaUtr(saCode)))) )).map(_ =>
                Redirect (uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.ApplyCTCodeChangesController.showPage))
            }
          )
      }
    }
  }

  def continueWithoutSaCode: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { desiDetails =>
        updateDraftDetails(_.copy(otherServices = desiDetails.otherServices.copy(saChanges = SaChanges(false, None)) )).map(_ =>
          Redirect (uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.ApplyCTCodeChangesController.showPage))
      }
    }
  }

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
}

