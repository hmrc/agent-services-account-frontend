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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.apply_sa_code_changes
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ApplySACodeChangesController @Inject() (
  actions: Actions,
  val sessionCache: SessionCacheService,
  draftDetailsService: DraftDetailsService,
  applySaCodeChangesView: apply_sa_code_changes,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext,
  pcodRepository: PendingChangeRequestRepository
)
extends FrontendController(cc)
with DesiDetailsJourneySupport
with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      isOtherServicesPageRequestValid().flatMap {
        case true => Future.successful(Ok(applySaCodeChangesView(UpdateDetailsForms.applySaCodeChangesForm)))
        case _ => Future.successful(Redirect(routes.ViewContactDetailsController.showPage))
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { newDesiDetails =>
        UpdateDetailsForms.applySaCodeChangesForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(applySaCodeChangesView(formWithErrors))),
            applySaCodeChanges =>
              draftDetailsService.updateDraftDetails(
                _.copy(otherServices = newDesiDetails.otherServices.copy(saChanges = SaChanges(applySaCodeChanges.apply, None)))
              ).flatMap { _ =>
                if (applySaCodeChanges.apply)
                  Future.successful(Redirect(routes.EnterSACodeController.showPage))
                else
                  isJourneyComplete().map(journey =>
                    if (journey.journeyComplete)
                      Redirect(routes.CheckYourAnswersController.showPage)
                    else
                      Redirect(routes.ApplyCTCodeChangesController.showPage)
                  )
              }
          )
      }
    }
  }

}
