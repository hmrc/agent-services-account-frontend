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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.DesiDetailsJourneySupport
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailHasNotChanged
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsAlreadyVerified
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsLocked
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailNeedsVerifying
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.EmailVerificationService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailVerificationEndpointController @Inject() (
  actions: Actions,
  val sessionCacheService: SessionCacheService,
  draftDetailsService: DraftDetailsService,
  cc: MessagesControllerComponents
)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext,
  pcodRepository: PendingChangeRequestRepository,
  ev: EmailVerificationService
)
extends FrontendController(cc)
with DesiDetailsJourneySupport
with I18nSupport
with Logging {

  /* This is the callback endpoint (return url) from the email-verification service and not for use of our own frontend. */
  val finishEmailVerification: Action[AnyContent] = actions.authActionCheckSuspend.async {
    implicit request =>
      ifChangeContactFeatureEnabledAndNoPendingChanges {
        isContactPageRequestValid("email").flatMap {
          case true =>
            sessionCacheService.get(emailPendingVerificationKey).flatMap {
              case Some(email) =>
                val credId = request.agentInfo.credentials.map(_.providerId).getOrElse(throw new RuntimeException("no available cred id"))
                ev.getEmailVerificationStatus(email, credId).flatMap {
                  case EmailIsAlreadyVerified =>
                    for {
                      _ <- draftDetailsService.updateDraftDetails(desiDetails =>
                        desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(email)))
                      ).flatMap {
                        _ => sessionCacheService.delete(emailPendingVerificationKey)
                      }
                      journey <- isJourneyComplete()
                    } yield
                      if (journey.journeyComplete)
                        Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
                      else
                        getNextPage(journey, "email")
                  case EmailIsLocked => Future.successful(Redirect(desiDetails.routes.UpdateEmailAddressController.showEmailLocked))
                  case EmailHasNotChanged =>
                    draftDetailsService.updateDraftDetails(desiDetails =>
                      desiDetails.copy(agencyDetails = desiDetails.agencyDetails.copy(agencyEmail = Some(email)))
                    ).flatMap {
                      _ =>
                        isJourneyComplete().map { journey =>
                          {
                            if (journey.journeyComplete)
                              Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
                            else
                              getNextPage(journey, "email")
                          }
                        }
                    }
                  case EmailNeedsVerifying =>
                    for {
                      _ <- sessionCacheService.put(emailPendingVerificationKey, email)
                      redirectUri <- ev.initialiseEmailVerificationJourney(
                        credId,
                        email,
                        messagesApi.preferred(request).lang
                      )
                    } yield Redirect(redirectUri)
                }
              case None =>
                isJourneyComplete().map { journey =>
                  {
                    if (journey.journeyComplete)
                      Redirect(desiDetails.routes.CheckYourAnswersController.showPage)
                    else
                      getNextPage(journey, "email")
                  }
                }
            }
          case _ => Future.successful(Redirect(routes.ViewContactDetailsController.showPage))
        }
      }
  }

}
