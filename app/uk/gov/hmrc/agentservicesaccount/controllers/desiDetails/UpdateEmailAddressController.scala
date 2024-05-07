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
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.{EMAIL_PENDING_VERIFICATION, desiDetails}
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms
import uk.gov.hmrc.agentservicesaccount.models.emailverification.{EmailHasNotChanged, EmailIsAlreadyVerified, EmailIsLocked, EmailNeedsVerifying}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, EmailVerificationService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.{email_locked, update_email}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateEmailAddressController @Inject()(actions: Actions,
                                             val sessionCache: SessionCacheService,
                                             draftDetailsService: DraftDetailsService,
                                             emailVerificationService: EmailVerificationService,
                                             update_email: update_email,
                                             email_locked: email_locked,
                                             cc: MessagesControllerComponents
                                            )(implicit appConfig: AppConfig,
                                              ec: ExecutionContext,
                                              pcodRepository: PendingChangeRequestRepository,
                                              agentAssuranceConnector: AgentAssuranceConnector
                                            ) extends FrontendController(cc) with DesiDetailsJourneySupport with I18nSupport with Logging {


  val showChangeEmailAddress: Action[AnyContent] =
    actions.authActionCheckSuspend.async {
      implicit request =>
        ifChangeContactFeatureEnabledAndNoPendingChanges {
          isContactPageRequestValid("email").flatMap {
            case true => Future.successful(Ok(update_email(UpdateDetailsForms.emailAddressForm)))
            case _ => Future.successful(Redirect(routes.SelectDetailsController.showPage))
          }
        }
    }

  val submitChangeEmailAddress: Action[AnyContent] =
    actions.authActionCheckSuspend.async {
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
                      _ <- draftDetailsService.updateDraftDetails(
                        desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail)))
                      )
                      _ <- sessionCache.delete(EMAIL_PENDING_VERIFICATION)
                      journey <- isJourneyComplete()
                    } yield getNextPage(journey, "email")
                  case EmailIsLocked =>
                    Future.successful(Redirect(desiDetails.routes.UpdateEmailAddressController.showEmailLocked))
                  case EmailHasNotChanged =>
                    draftDetailsService.updateDraftDetails(
                      desiDetails => desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(newEmail)))
                    ).flatMap {
                      _ =>
                        isJourneyComplete().flatMap(journeyComplete => Future.successful(getNextPage(journeyComplete, "email")))
                    }
                  case EmailNeedsVerifying =>
                    for {
                      _ <- sessionCache.put(EMAIL_PENDING_VERIFICATION, newEmail)
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

  val showEmailLocked: Action[AnyContent] =
    actions.authActionCheckSuspend.async {
      implicit request =>
        ifChangeContactFeatureEnabledAndNoPendingChanges {
          Future.successful(Ok(email_locked()))
        }
    }


}
