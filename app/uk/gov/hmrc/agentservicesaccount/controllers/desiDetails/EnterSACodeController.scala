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
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterSACodeController @Inject()(actions: Actions,
                                      val sessionCache: SessionCacheService,
                                      draftDetailsService: DraftDetailsService,
                                      enterSaCodeView: enter_sa_code,
                                      cc: MessagesControllerComponents
                                     )(implicit appConfig: AppConfig,
                                       ec: ExecutionContext,
                                       pcodRepository: PendingChangeRequestRepository,
                                       acaConnector: AgentClientAuthorisationConnector
                                     ) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      isOtherServicesPageRequestValid().flatMap {
        case true => Future.successful(Ok(enterSaCodeView(UpdateDetailsForms.saCodeForm)))
        case _ => Future.successful(Redirect(routes.ViewContactDetailsController.showPage))
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { desiDetails =>
        UpdateDetailsForms.saCodeForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(enterSaCodeView(formWithErrors))),
            saCode => {
              draftDetailsService.updateDraftDetails(
                _.copy(otherServices = desiDetails.otherServices.copy(saChanges = SaChanges(true, Some(SaUtr(saCode)))))
              ).flatMap {
                _ =>
                  isJourneyComplete().map(journey =>
                    if(journey.journeyComplete) Redirect(routes.CheckYourAnswersController.showPage)
                    else Redirect(routes.ApplyCTCodeChangesController.showPage)
                  )
              }
            }
          )
      }
    }
  }

  def continueWithoutSaCode: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    ifChangeContactFeatureEnabledAndNoPendingChanges {
      withUpdateDesiDetailsJourney { desiDetails =>
        draftDetailsService
          .updateDraftDetails(
            _.copy(otherServices = desiDetails.otherServices.copy(saChanges = SaChanges(true, None)))
          ).map {
            _ =>
              Redirect(uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.routes.ApplyCTCodeChangesController.showPage)
          }
      }
    }
  }

}

