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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ToFuture
import uk.gov.hmrc.agentservicesaccount.forms.NewRegistrationNumberForm
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.enter_registration_number
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class EnterRegistrationNumberController @Inject()(actions: Actions,
                                           val updateAmlsJourneyRepository: UpdateAmlsJourneyRepository,
                                           enterRegistrationNumber: enter_registration_number,
                                           cc: MessagesControllerComponents
                                          )(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {

  private def registrationNumberForm(isHmrc: Boolean) = NewRegistrationNumberForm.form(isHmrc)

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { amlsJourney =>
        val form = amlsJourney
          .newRegistrationNumber
          .fold(registrationNumberForm(amlsJourney.isHmrc))(registrationNumberForm(amlsJourney.isHmrc).fill)
        Ok(enterRegistrationNumber(form, backLink(amlsJourney))).toFuture
      }
    }
  }


  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      withUpdateAmlsJourney { amlsJourney =>
        registrationNumberForm(amlsJourney.isHmrc)
          .bindFromRequest()
          .fold(
            formWithError => Ok(enterRegistrationNumber(formWithError, backLink(amlsJourney))).toFuture,
            data =>
              saveAmlsJourney(amlsJourney.copy(newRegistrationNumber = Option(data))).map(_ =>
                Redirect(nextPage(amlsJourney))
              )
          )
      }
    }
  }


  private def backLink(journey: UpdateAmlsJourney): String = {
    if(journey.isChange) "/cya"
    else if (journey.isRegistrationNumberStillTheSame.contains(false)) routes.ConfirmRegistrationNumberController.showPage.url
    else routes.AmlsNewSupervisoryBodyController.showPage.url
  }

  private def nextPage(journey: UpdateAmlsJourney): String = {
    if(journey.isChange | !journey.isUkAgent) "/cya"
    else routes.EnterRenewalDateController.showPage.url
  }
}

