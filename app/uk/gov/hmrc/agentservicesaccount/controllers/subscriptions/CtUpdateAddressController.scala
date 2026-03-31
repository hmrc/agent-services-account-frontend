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

package uk.gov.hmrc.agentservicesaccount.controllers.subscriptions

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.updateAddressPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtAddressFormValues
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtUpdateAddressController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
//  emailVerificationService: AddressVerificationService,
  ct_update_address: ct_update_address,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  private def formatAddress(address: BusinessAddress): String = List(
    Some(address.addressLine1),
    address.addressLine2,
    address.addressLine3,
    address.addressLine4,
    address.postalCode,
    Some(address.countryCode)
  ).flatten.map(play.twirl.api.HtmlFormat.escape)
    .map(_.body)
    .mkString(", ")

  val showPage: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val journey = request.ctSubscriptionJourney

    val subscriptionAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")

    val form =
      journey.useCustomAddress match {

        case Some(useCustom) =>
          CtSubscriptionAddressForm.form.fill(
            CtAddressFormValues(
              useAsaData = !useCustom
            )
          )

        case None => CtSubscriptionAddressForm.form
      }

    Future.successful(
      Ok(ct_update_address(form, subscriptionAddress))
    )
  }

  def onSubmit: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val journey = request.ctSubscriptionJourney

    CtSubscriptionAddressForm.form.bindFromRequest().fold(
      formWithErrors => {
        val subscriptionAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")
        Future.successful(
          BadRequest(ct_update_address(formWithErrors, subscriptionAddress))
        )
      },
      data => {
        if (data.useAsaData) {
          val updatedJourney = journey.copy(
            useCustomAddress = Some(false)
          )

          sessionCacheService
            .put(ctJourneyKey, updatedJourney)
            .map { _ =>
              Redirect(getNextPage(updateAddressPage, Some(updatedJourney)))
            }
        }
        else {
//          TODO: 10906: Add ALF journey here
          Future.successful(Redirect(routes.CtCheckYourAnswersController.showPage))
        }
      }
    )
  }

}
