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
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.NextPageSelector.updateBusinessNamePage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.PayeSubscriptionContactNameForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PayeContactNameFormValues
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.paye_update_contact_name
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class PayeUpdateContactNameController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  paye_update_contact_name: paye_update_contact_name,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {
//  TODO: 11186: Build this into correct design and saving mechanism

  def showPage: Action[AnyContent] = actions.authActionWithSubscriptionJourney(PAYE).async { implicit request =>
    val journey = request.subscriptionJourney

    val subscriptionBusinessName = journey.asaDetails.agencyName.getOrElse("")

    val initialForm = PayeSubscriptionContactNameForm.form(PAYE)
    val form =
      journey.useCustomBusinessName match {

        case Some(useCustom) =>
          initialForm.fill(
            PayeContactNameFormValues(
              useAsaData = !useCustom,
              newBusinessName = journey.businessNameAnswer
            )
          )

        case None => initialForm
      }

    Future.successful(
      Ok(paye_update_contact_name(
        form,
        subscriptionBusinessName
      ))
    )
  }

  def onSubmit: Action[AnyContent] = actions.authActionWithSubscriptionJourney(PAYE).async { implicit request =>
    val journey = request.subscriptionJourney

    PayeSubscriptionContactNameForm.form(PAYE).bindFromRequest().fold(
      formWithErrors => {
        val subscriptionBusinessName = journey.asaDetails.agencyName.getOrElse("")
        Future.successful(
          BadRequest(paye_update_contact_name(
            formWithErrors,
            subscriptionBusinessName
          ))
        )
      },
      data => {
        val updatedJourney = journey.copy(
          useCustomBusinessName = Some(!data.useAsaData),
          businessNameAnswer =
            if (data.useAsaData)
              None
            else
              data.newBusinessName
        )

        sessionCacheService
          .put(subscriptionJourneyKey(PAYE), updatedJourney)
          .map(_ =>
            Redirect(getNextPage(
              updateBusinessNamePage,
              Some(updatedJourney),
              PAYE
            ))
          )
      }
    )
  }

}
