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

package uk.gov.hmrc.agentservicesaccount.services

import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers
import uk.gov.hmrc.agentservicesaccount.models.emailverification._
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject()(agentAssuranceConnector: AgentAssuranceConnector,
                                         emailVerificationConnector: EmailVerificationConnector
                                        )(implicit ec: ExecutionContext, appConfig: AppConfig) {

  def getEmailVerificationStatus(newEmail: String, credId: String)
                                (implicit rh: RequestHeader): Future[EmailVerificationStatus] =
    for {
      optCurrentEmail <- agentAssuranceConnector.getAgentRecord.map(_.agencyDetails.flatMap(_.agencyEmail))
      isUnchanged = optCurrentEmail.exists(_.trim.equalsIgnoreCase(newEmail.trim))
      checkVerifications <- emailVerificationConnector.checkEmail(credId)
      previouslyCompletedEmailVerification = checkVerifications.flatMap(_.emails.find(completedEmail => completedEmail.equalsTrimmed(newEmail)))
    } yield {
      previouslyCompletedEmailVerification match {
        case _ if isUnchanged => EmailHasNotChanged
        case Some(completedEmailVerification) if completedEmailVerification.locked => EmailIsLocked
        case Some(completedEmailVerification) if completedEmailVerification.verified => EmailIsAlreadyVerified
        case None => EmailNeedsVerifying
      }
    }

  def initialiseEmailVerificationJourney(credId: String, newEmail: String, lang: Lang)
                                        (implicit rh: RequestHeader): Future[String] = {
    val useAbsoluteUrls = appConfig.emailVerificationFrontendBaseUrl.contains("localhost")

    def makeUrl(call: Call): String = if (useAbsoluteUrls) call.absoluteURL() else call.url

    val emailRequest = VerifyEmailRequest(
      credId = credId,
      continueUrl = makeUrl(controllers.desiDetails.routes.EmailVerificationEndpointController.finishEmailVerification),
      origin = if (lang.code == "cy") "Gwasanaethau Asiant CThEM" else "HMRC Agent Services",
      deskproServiceName = None, // TODO - this should probably have a name?
      accessibilityStatementUrl = "", // TODO (already here prior to restructuring on 16/04/24)
      email = Some(Email(newEmail, makeUrl(controllers.desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress))),
      lang = Some(lang.code),
      backUrl = Some(makeUrl(controllers.desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress)),
      pageTitle = None
    )

    emailVerificationConnector.verifyEmail(emailRequest).map {
      case Some(emailVerificationResponse) if useAbsoluteUrls =>
        appConfig.emailVerificationFrontendBaseUrl + emailVerificationResponse.redirectUri
      case Some(emailVerificationResponse) =>
        emailVerificationResponse.redirectUri
      case None =>
        throw new InternalServerException(
          "[EmailVerificationService][initialiseEmailVerificationJourney] " +
            "No response was returned from the email verification service"
        )
    }
  }

}

