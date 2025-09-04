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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.i18n.Messages
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ToFuture
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.confirm_registration_number
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ConfirmRegistrationNumberController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  confirmRegistrationNumber: confirm_registration_number,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with AmlsJourneySupport
with I18nSupport
with Logging {

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.withCurrentAmlsDetails(request.agentInfo.arn) { amlsDetails =>
      amlsDetails.membershipNumber match {
        case Some(registrationNumber) =>
          withUpdateAmlsJourney { amlsJourney =>
            val form = amlsJourney.isRegistrationNumberStillTheSame.fold(YesNoForm.form(""))(x => YesNoForm.form().fill(x))
            Ok(confirmRegistrationNumber(form, registrationNumber)).toFuture
          }
        case None =>
          logger.info("No AMLS registration number found, redirecting to 'Enter Registration Number'")
          Redirect(routes.EnterRegistrationNumberController.showPage()).toFuture
      }
    }
  }

  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.withCurrentAmlsDetails(request.agentInfo.arn) { amlsDetails =>
      amlsDetails.membershipNumber match {
        case Some(registrationNumber) =>
          withUpdateAmlsJourney { amlsJourney =>
            YesNoForm.form(Messages("amls.confirm-registration-number.error", registrationNumber))
              .bindFromRequest()
              .fold(
                formWithError => Future successful BadRequest(confirmRegistrationNumber(formWithError, registrationNumber)),
                data => {
                  val maybeCopyRegistrationNumber =
                    if (data)
                      amlsDetails.membershipNumber
                    else
                      amlsJourney.newRegistrationNumber
                  saveAmlsJourney(amlsJourney.copy(
                    isRegistrationNumberStillTheSame = Option(data),
                    newRegistrationNumber = maybeCopyRegistrationNumber
                  )).map(_ =>
                    Redirect(nextPage(data))
                  )
                }
              )
          }
        case None => Redirect(routes.EnterRegistrationNumberController.showPage()).toFuture
      }
    }
  }

  private def nextPage(confirm: Boolean): String =
    if (confirm)
      routes.EnterRenewalDateController.showPage.url
    else
      routes.EnterRegistrationNumberController.showPage().url

}
