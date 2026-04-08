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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.emailVerificationFinish
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.util.CtNextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsAlreadyVerified
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailNeedsVerifying
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.services.EmailVerificationService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class CtEmailVerificationEndpointController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext,
  ev: EmailVerificationService
)
extends FrontendController(cc)
with I18nSupport
with Logging {

  /* This is the callback endpoint (return url) from the email-verification service and not for use of our own frontend. */
  val finishEmailVerification: Action[AnyContent] = actions.authActionWithCtJourney.async {
    implicit request =>
      val legacyRegime = LegacyRegime.CT
      sessionCacheService.get(emailPendingVerificationKey).flatMap {
        case Some(email) =>
          val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
          ev.getEmailVerificationStatus(email, credId).flatMap {
            case EmailIsAlreadyVerified =>
              val journey = request.ctSubscriptionJourney
              val updatedJourney = journey.copy(
                useCustomEmail = Some(true),
                emailAnswer = Some(email)
              )

              sessionCacheService
                .put(ctJourneyKey, updatedJourney)
                .map(_ => Redirect(getNextPage(emailVerificationFinish, Some(updatedJourney))))

            case EmailNeedsVerifying =>
              for {
                _ <- sessionCacheService.put(emailPendingVerificationKey, email)
                redirectUri <- ev.initialiseEmailVerificationJourney(
                  credId,
                  email,
                  messagesApi.preferred(request).lang,
                  routes.CtEmailVerificationEndpointController.finishEmailVerification,
                  routes.CtUpdateEmailAddressController.showPage
                )
              } yield Redirect(redirectUri)
          }
        case None => Future.successful(Redirect(routes.CtUpdateEmailAddressController.showPage))
      }
  }
}
