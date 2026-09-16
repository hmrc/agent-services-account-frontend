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

import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.changeAddressPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.updateAddressPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.ChangeSubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.CountryResolver
import uk.gov.hmrc.agentservicesaccount.utils.RequestAwareLogging
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.change_address
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
  change_address: change_address,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with RequestAwareLogging {

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

  def showPage(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val asaDetailsAgencyAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")

    val initialForm = SubscriptionAddressForm.form(agentRegime)
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
        asaDetailsAgencyAddress,
        agentRegime
      ))
    )
  }

  def onSubmit(agentRegime: AgentRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    SubscriptionAddressForm.form(agentRegime).bindFromRequest().fold(
      formWithErrors => {
        val asaDetailsAgencyAddress = journey.asaDetails.agencyAddress.map(formatAddress).getOrElse("")
        Future.successful(
          BadRequest(update_address(
            formWithErrors,
            asaDetailsAgencyAddress,
            agentRegime
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
            .put(subscriptionJourneyKey(agentRegime), updatedJourney)
            .map(_ =>
              Redirect(getNextPage(
                updateAddressPage,
                Some(updatedJourney),
                agentRegime
              ))
            )
        }
        else {
          Future.successful(Redirect(routes.AddressLookupController.startAddressLookup(agentRegime)))
        }
      }
    )
  }

  def showChange(
                  agentRegime: AgentRegime,
                  isInvalid: Boolean
  ): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val address =
      journey.useCustomAddress match {
        case Some(true) => journey.addressAnswer
        case Some(false) => journey.asaDetails.agencyAddress
        case _ => None
      }

    address match {
      case None => Future.successful(Redirect(routes.UpdateAddressController.showPage(agentRegime)))
      case Some(address) =>
        val form =
          if (address.isUk) {
            ChangeSubscriptionAddressForm.ukForm(agentRegime).fill(address)
          }
          else {
            ChangeSubscriptionAddressForm.nonUkForm(agentRegime).fill(address)
          }
        Future.successful(
          Ok(change_address(
            form,
            agentRegime,
            address.isUk,
            isInvalid
          ))
        )
    }
  }

  def onSubmitChange(
                      agentRegime: AgentRegime,
                      isInvalid: Boolean
  ): Action[AnyContent] = actions.authActionWithSubscriptionJourney(agentRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val address =
      journey.useCustomAddress match {
        case Some(true) => journey.addressAnswer
        case Some(false) => journey.asaDetails.agencyAddress
        case _ => None
      }

    address match {
      case None => Future.successful(Redirect(routes.UpdateAddressController.showPage(agentRegime)))
      case Some(address) =>
        val form =
          if (address.isUk) {
            ChangeSubscriptionAddressForm.ukForm(agentRegime)
          }
          else {
            ChangeSubscriptionAddressForm.nonUkForm(agentRegime)
          }
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(change_address(
              formWithErrors,
              agentRegime,
              address.isUk,
              isInvalid
            ))),
          newAddress => {
            val updatedJourney = journey.copy(
              useCustomAddress = Some(true),
              addressAnswer = Some(newAddress)
            )

            sessionCacheService
              .put(subscriptionJourneyKey(agentRegime), updatedJourney)
              .map(_ =>
                Redirect(getNextPage(
                  changeAddressPage,
                  Some(updatedJourney),
                  agentRegime
                ))
              )
          }
        )
    }
  }

}
