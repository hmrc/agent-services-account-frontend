/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.updatePhoneNumberPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionPhoneNumberForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtPhoneNumberFormValues
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtUpdatePhoneNumberController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  ct_update_phone_number: ct_update_phone_number,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  val showPage: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val journey = request.ctSubscriptionJourney

    val subscriptionPhoneNumber = journey.asaDetails.agencyTelephone.getOrElse("")

    val form =
      journey.useCustomPhoneNumber match {

        case Some(useCustom) =>
          CtSubscriptionPhoneNumberForm.form.fill(
            CtPhoneNumberFormValues(
              useAsaData = !useCustom,
              newPhoneNumber = journey.phoneNumberAnswer
            )
          )

        case None => CtSubscriptionPhoneNumberForm.form
      }

    Future.successful(
      Ok(ct_update_phone_number(form, subscriptionPhoneNumber))
    )
  }

  def onSubmit: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val journey = request.ctSubscriptionJourney

    CtSubscriptionPhoneNumberForm.form.bindFromRequest().fold(
      formWithErrors => {
        val subscriptionPhoneNumber = journey.asaDetails.agencyTelephone.getOrElse("")
        Future.successful(
          BadRequest(ct_update_phone_number(formWithErrors, subscriptionPhoneNumber))
        )
      },
      data => {
        val updatedJourney = journey.copy(
          useCustomPhoneNumber = Some(!data.useAsaData),
          phoneNumberAnswer =
            if (data.useAsaData)
              None
            else
              data.newPhoneNumber
        )

        sessionCacheService
          .put(ctJourneyKey, updatedJourney)
          .map(_ => Redirect(getNextPage(updatePhoneNumberPage, Some(updatedJourney))))
      }
    )
  }

}
