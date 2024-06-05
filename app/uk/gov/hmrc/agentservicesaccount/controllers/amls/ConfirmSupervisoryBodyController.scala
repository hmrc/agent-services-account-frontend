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

import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ToFuture
import uk.gov.hmrc.agentservicesaccount.forms.YesNoForm
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.confirm_supervisory_body
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ConfirmSupervisoryBodyController @Inject()(actions: Actions,
                                                 val updateAmlsJourneyRepository: UpdateAmlsJourneyRepository,
                                                 confirmSupervisoryBody: confirm_supervisory_body,
                                                 cc: MessagesControllerComponents
                                                )(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {


  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      actions.withCurrentAmlsDetails(request.agentInfo.arn){ amlsDetails =>
        withUpdateAmlsJourney { amlsJourney =>
          val form = amlsJourney.isAmlsBodyStillTheSame.fold(YesNoForm.form(""))(YesNoForm.form().fill)
          Ok(confirmSupervisoryBody(form, amlsDetails.supervisoryBody)).toFuture
        }
      }
    }
  }


  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
      actions.withCurrentAmlsDetails(request.agentInfo.arn){ amlsDetails =>
        withUpdateAmlsJourney { amlsJourney =>
          YesNoForm.form(Messages("amls.confirm-supervisory-body.error", amlsDetails.supervisoryBody))
            .bindFromRequest()
            .fold(
              formWithError => Future successful BadRequest(confirmSupervisoryBody(formWithError, amlsDetails.supervisoryBody)),
              data => {
                val maybeCopySupervisoryBody = if(data) Option(amlsDetails.supervisoryBody) else amlsJourney.newAmlsBody
                saveAmlsJourney(amlsJourney.copy(
                  isAmlsBodyStillTheSame = Option(data),
                  newAmlsBody = maybeCopySupervisoryBody)
                ).map(_ =>
                  Redirect(nextPage(data)(amlsJourney)))
              }
            )
        }
      }
    }
  }

  private def nextPage(confirm: Boolean)(journey: UpdateAmlsJourney): String =
    if(confirm) if(journey.isUkAgent) routes.ConfirmRegistrationNumberController.showPage.url
    else routes.EnterRegistrationNumberController.showPage().url
    else routes.AmlsNewSupervisoryBodyController.showPage().url

}
