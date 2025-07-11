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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailHasNotChanged
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsAlreadyVerified
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsLocked
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailNeedsVerifying
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.EmailVerificationService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.email_locked
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.update_email
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpdateEmailAddressController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  draftDetailsService: DraftDetailsService,
  emailVerificationService: EmailVerificationService,
  update_email: update_email,
  email_locked: email_locked,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext,
  pcodRepository: PendingChangeRequestRepository
)
extends FrontendController(cc)
with DesiDetailsJourneySupport
with I18nSupport
with Logging {

  val showChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      ifChangeContactFeatureEnabledAndNoPendingChanges {
        isContactPageRequestValid("email").flatMap {
          case true => Future.successful(Ok(update_email(UpdateDetailsForms.emailAddressForm)))
          case _ => Future.successful(Redirect(routes.SelectDetailsController.showPage))
        }
      }
  }

  val submitChangeEmailAddress: Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      ifChangeContactFeatureEnabledAndNoPendingChanges {
        UpdateDetailsForms.emailAddressForm
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(update_email(formWithErrors))),
            newEmail => {
              val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
              emailVerificationService.getEmailVerificationStatus(newEmail, credId).flatMap {
                case EmailIsAlreadyVerified =>
                  for {
                    _ <- draftDetailsService.updateDraftDetails(desiDetails =>
                      desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail)))
                    )
                    _ <- sessionCacheService.delete(emailPendingVerificationKey)
                    journey <- isJourneyComplete()
                  } yield getNextPage(journey, "email")
                case EmailIsLocked => Future.successful(Redirect(desiDetails.routes.UpdateEmailAddressController.showEmailLocked))
                case EmailHasNotChanged =>
                  draftDetailsService.updateDraftDetails(desiDetails =>
                    desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail)))
                  ).flatMap {
                    _ =>
                      isJourneyComplete().flatMap(journeyComplete => Future.successful(getNextPage(journeyComplete, "email")))
                  }
                case EmailNeedsVerifying =>
                  for {
                    _ <- sessionCacheService.put(emailPendingVerificationKey, newEmail)
                    redirectUri <- emailVerificationService.initialiseEmailVerificationJourney(
                      credId,
                      newEmail,
                      messagesApi.preferred(request).lang
                    )
                  } yield Redirect(redirectUri)
              }
            }
          )
      }
  }

  val showEmailLocked: Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      ifChangeContactFeatureEnabledAndNoPendingChanges {
        Future.successful(Ok(email_locked()))
      }
  }

}
