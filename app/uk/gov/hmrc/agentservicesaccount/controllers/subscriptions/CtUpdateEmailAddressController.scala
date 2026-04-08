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
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.updateEmailAddressPage
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionEmailAddressForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtEmailAddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.services.EmailVerificationService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtUpdateEmailAddressController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  emailVerificationService: EmailVerificationService,
  ct_update_email_address: ct_update_email_address,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  val showPage: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val legacyRegime = LegacyRegime.CT
    val journey = request.ctSubscriptionJourney

    val subscriptionEmailAddress = journey.asaDetails.agencyEmail.getOrElse("")

    val form =
      journey.useCustomEmail match {

        case Some(useCustom) =>
          CtSubscriptionEmailAddressForm.form.fill(
            CtEmailAddressFormValues(
              useAsaData = !useCustom,
              newEmailAddress = journey.emailAnswer
            )
          )

        case None => CtSubscriptionEmailAddressForm.form
      }

    Future.successful(
      Ok(ct_update_email_address(form, subscriptionEmailAddress))
    )
  }

  def onSubmit: Action[AnyContent] = actions.authActionWithCtJourney.async { implicit request =>
    val legacyRegime = LegacyRegime.CT
    val journey = request.ctSubscriptionJourney

    CtSubscriptionEmailAddressForm.form.bindFromRequest().fold(
      formWithErrors => {
        val subscriptionEmailAddress = journey.asaDetails.agencyEmail.getOrElse("")
        Future.successful(
          BadRequest(ct_update_email_address(formWithErrors, subscriptionEmailAddress))
        )
      },
      data => {
        if (data.useAsaData) {
          val updatedJourney = journey.copy(
            useCustomEmail = Some(false),
            emailAnswer = None
          )

          sessionCacheService
            .put(ctJourneyKey, updatedJourney)
            .map(_ => Redirect(getNextPage(updateEmailAddressPage, Some(updatedJourney))))
        }
        else {
          data.newEmailAddress.map(newEmail => {
            val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
            for {
              _ <- sessionCacheService.put(emailPendingVerificationKey, newEmail)
              redirectUri <- emailVerificationService.initialiseEmailVerificationJourney(
                credId,
                newEmail,
                messagesApi.preferred(request).lang,
                routes.CtEmailVerificationEndpointController.finishEmailVerification,
                routes.CtUpdateEmailAddressController.showPage
              )
            } yield Redirect(redirectUri)
          }).getOrElse(Future.successful(Redirect(routes.CtUpdateEmailAddressController.showPage)))
        }
      }
    )
  }

}
