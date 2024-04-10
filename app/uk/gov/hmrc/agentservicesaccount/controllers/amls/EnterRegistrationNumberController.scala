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
import uk.gov.hmrc.agentservicesaccount.forms.NewRegistrationNumberForm.{form => registrationNumberForm}
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

  def showPage(cya: Boolean): Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
        withUpdateAmlsJourney { amlsJourney =>
          val form = amlsJourney.newRegistrationNumber match {
            case Some(number) => registrationNumberForm(amlsJourney.isHmrc).fill(number)
            case _ => registrationNumberForm(amlsJourney.isHmrc)
          }
          Ok(enterRegistrationNumber(form, cya)).toFuture
        }
      }
  }

  def onSubmit(cya: Boolean): Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
        withUpdateAmlsJourney { amlsJourney =>
          registrationNumberForm(amlsJourney.isHmrc)
            .bindFromRequest()
            .fold(
              formWithError => Ok(enterRegistrationNumber(formWithError, cya)).toFuture,
              data =>
                saveAmlsJourney(amlsJourney.copy(
                  newRegistrationNumber = Option(data),
                  isRegistrationNumberStillTheSame = for {
                    hasSameRegNum <- amlsJourney.isRegistrationNumberStillTheSame
                    inputEqualsRegNum = amlsJourney.newRegistrationNumber.contains(data)
                  } yield hasSameRegNum && inputEqualsRegNum
                )).map(_ =>
                  Redirect(nextPage(cya, amlsJourney))
                )
            )
        }
      }
  }

  private def nextPage(cya: Boolean, journey: UpdateAmlsJourney): String = {
    if (cya | !journey.isUkAgent) routes.CheckYourAnswersController.showPage.url
    else routes.EnterRenewalDateController.showPage.url
  }
}

