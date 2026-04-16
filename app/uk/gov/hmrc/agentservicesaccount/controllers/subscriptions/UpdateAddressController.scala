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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.updateAddressPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.CountryResolver
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.update_address
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpdateAddressController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  countryResolver: CountryResolver,
  update_address: update_address,
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
    Some(countryResolver.countryName(address.countryCode))
  ).flatten.map(play.twirl.api.HtmlFormat.escape)
    .map(_.body)
    .mkString(", ")

  def showPage(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val subscriptionAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")

    val initialForm = SubscriptionAddressForm.form(legacyRegime)
    val form =
      journey.useCustomAddress match {

        case Some(useCustom) =>
          initialForm.fill(
            AddressFormValues(
              useAsaData = !useCustom
            )
          )

        case None => initialForm
      }

    Future.successful(
      Ok(update_address(
        form,
        subscriptionAddress,
        legacyRegime
      ))
    )
  }

  def onSubmit(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    SubscriptionAddressForm.form(legacyRegime).bindFromRequest().fold(
      formWithErrors => {
        val subscriptionAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")
        Future.successful(
          BadRequest(update_address(
            formWithErrors,
            subscriptionAddress,
            legacyRegime
          ))
        )
      },
      data => {
        if (data.useAsaData) {
          val updatedJourney = journey.copy(
            useCustomAddress = Some(false),
            addressAnswer = None
          )

          sessionCacheService
            .put(subscriptionJourneyKey(legacyRegime), updatedJourney)
            .map(_ =>
              Redirect(getNextPage(
                updateAddressPage,
                Some(updatedJourney),
                legacyRegime
              ))
            )
        }
        else {
          Future.successful(Redirect(routes.AddressLookupController.startAddressLookup(legacyRegime)))
        }
      }
    )
  }

}
