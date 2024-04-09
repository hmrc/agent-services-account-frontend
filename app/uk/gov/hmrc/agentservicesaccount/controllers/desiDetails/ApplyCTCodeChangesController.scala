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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.apply_ct_code_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ApplyCTCodeChangesController @Inject()(
                                              actions: Actions,
                                              val sessionCache: SessionCacheService,
                                              val acaConnector: AgentClientAuthorisationConnector,
                                              applyCtCodeChangesView: apply_ct_code_changes
                                            )(implicit appConfig: AppConfig,
                                              cc: MessagesControllerComponents,
                                              ec: ExecutionContext,
                                              pcodRepository: PendingChangeOfDetailsRepository)
  extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      Future.successful(Ok(applyCtCodeChangesView(UpdateDetailsForms.applyCtCodeChangesForm)))
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { desiDetails =>
        UpdateDetailsForms.applyCtCodeChangesForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(Ok(applyCtCodeChangesView(formWithErrors))),
            applySaCodeChanges => {
              updateDraftDetails(_.copy(otherServices = desiDetails.otherServices.copy(ctChanges = CtChanges(applySaCodeChanges.apply, None)) )).map(_ =>
                Redirect (uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.EnterCTCodeController.showPage)
              )
            }
          )
      }
    }
  }
}
