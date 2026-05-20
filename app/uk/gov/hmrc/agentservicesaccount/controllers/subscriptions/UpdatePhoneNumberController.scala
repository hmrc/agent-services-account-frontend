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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.updatePhoneNumberPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionPhoneNumberForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PhoneNumberFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.update_phone_number
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpdatePhoneNumberController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  update_phone_number: update_phone_number,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  def showPage(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val asaDetailsAgencyName = journey.asaDetails.agencyName.getOrElse("")
    val asaDetailsAgencyTelephone: Option[String] = journey.asaDetails.agencyTelephone

    val initialForm = SubscriptionPhoneNumberForm.form(legacyRegime, asaDetailsAgencyName)
    val form =
      journey.useCustomPhoneNumber match {

        case Some(useCustom) =>
          initialForm.fill(
            PhoneNumberFormValues(
              useAsaData = !useCustom,
              newPhoneNumber = journey.phoneNumberAnswer
            )
          )

        case None => initialForm
      }

    Future.successful(
      Ok(update_phone_number(
        form,
        asaDetailsAgencyTelephone,
        legacyRegime
      ))
    )
  }

  def onSubmit(legacyRegime: LegacyRegime): Action[AnyContent] = actions.authActionWithSubscriptionJourney(legacyRegime).async { implicit request =>
    val journey = request.subscriptionJourney

    val asaDetailsAgencyName = journey.asaDetails.agencyName.getOrElse("")

    SubscriptionPhoneNumberForm.form(legacyRegime, asaDetailsAgencyName).bindFromRequest().fold(
      formWithErrors => {
        val asaDetailsAgencyTelephone: Option[String] = journey.asaDetails.agencyTelephone
        Future.successful(
          BadRequest(update_phone_number(
            formWithErrors,
            asaDetailsAgencyTelephone,
            legacyRegime
          ))
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
          .put(subscriptionJourneyKey(legacyRegime), updatedJourney)
          .map(_ =>
            Redirect(getNextPage(
              updatePhoneNumberPage,
              Some(updatedJourney),
              legacyRegime
            ))
          )
      }
    )
  }

}
