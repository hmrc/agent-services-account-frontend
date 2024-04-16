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
import uk.gov.hmrc.agentservicesaccount.forms.RenewalDateForm
import uk.gov.hmrc.agentservicesaccount.repository.UpdateAmlsJourneyRepository
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.enter_renewal_date
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.agentservicesaccount.models.ModelExtensionMethods._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class EnterRenewalDateController @Inject()(actions: Actions,
                                                 val updateAmlsJourneyRepository: UpdateAmlsJourneyRepository,
                                           enterRenewalDate: enter_renewal_date,
                                                 cc: MessagesControllerComponents
                                                )(implicit appConfig: AppConfig, ec: ExecutionContext) extends FrontendController(cc) with AmlsJourneySupport with I18nSupport {

  private val renewalDateForm = RenewalDateForm.form

  def showPage: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
        withUpdateAmlsJourney { amlsJourney =>
          if(amlsJourney.status.isUkAgent()) {
          val form = amlsJourney.newExpirationDate.fold(renewalDateForm)(renewalDateForm.fill)
          Ok(enterRenewalDate(form)).toFuture
        } else {
            Forbidden.toFuture
          }}
    }
  }


  def onSubmit: Action[AnyContent] = actions.authActionCheckSuspend.async { implicit request =>
    actions.ifFeatureEnabled(appConfig.enableNonHmrcSupervisoryBody) {
        withUpdateAmlsJourney { amlsJourney =>
            renewalDateForm
            .bindFromRequest()
            .fold(
              formWithError => BadRequest(enterRenewalDate(formWithError)).toFuture,
              data =>
                saveAmlsJourney(amlsJourney.copy(newExpirationDate = Option(data))).map(_ =>
                    Redirect(routes.CheckYourAnswersController.showPage)
                )
            )
      }
    }
  }
}

