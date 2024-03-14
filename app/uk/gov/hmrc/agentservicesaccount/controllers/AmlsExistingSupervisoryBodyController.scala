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

package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.repository.AmlsJourneySessionRepository
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.existing_supervisory_body
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AmlsExistingSupervisoryBodyController @Inject()(
                                                      actions: Actions,
                                                      val amlsJourneySessionRepository: AmlsJourneySessionRepository,
                                                      existing_supervisory_body: existing_supervisory_body)(implicit val appConfig: AppConfig,
                                                      val cc: MessagesControllerComponents, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {

  def showExistingSupervisoryBody: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody){
      actions.withCurrentAmlsDetails(request.agentInfo.arn){ amlsDetails =>
        testOnlyInitialiseAmlsJourneySession.flatMap(_ =>
          withAmlsJourneySession { amlsJourney =>
            Future successful Ok(existing_supervisory_body(YesNoForm.form(""), amlsDetails.supervisoryBody))
          }
        )
      }
    }
  }

  def submitConfirmExistingSupervisoryBody: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request  =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody){
      actions.withCurrentAmlsDetails(request.agentInfo.arn){ amlsDetails =>
        withAmlsJourneySession { amlsJourney =>
          YesNoForm.form(Messages("amls.use-existing-registration-number.error", amlsDetails.supervisoryBody))
            .bindFromRequest()
            .fold(
              formWithError => Future successful Ok(existing_supervisory_body(formWithError, amlsDetails.supervisoryBody)),
              data =>
                saveAmlsJourneySession(amlsJourney.copy(useExistingAmlsSupervisoryBody = Option(data))).map(_ =>
                if(data.booleanValue()) // TODO - link to APB-7793 confirm existing registration number
                  Redirect(s"/confirm-reg-number")
                else
                  Redirect(routes.AmlsNewSupervisoryBodyController.showNewSupervisoryBody)
            )
            )
        }
        }
      }
  }

}
