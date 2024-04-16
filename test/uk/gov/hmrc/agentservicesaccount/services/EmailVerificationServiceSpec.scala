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

import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatestplus.play.PlaySpec
import play.api.i18n.Lang
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers.await
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentClientAuthorisationConnector, EmailVerificationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers
import uk.gov.hmrc.agentservicesaccount.models.emailverification._
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationServiceSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext =  ExecutionContext.Implicits.global
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait Setup {
    protected val mockAgentClientAuthorisationConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
    protected val mockAppConfig: AppConfig = mock[AppConfig]

    object TestService extends EmailVerificationService(
      mockAgentClientAuthorisationConnector,
      mockEmailVerificationConnector
    )(ec, mockAppConfig)

  }


  "initialiseEmailVerificationJourney" should {
    "return an absolute url for use locally" when {
      "successfully called out to Email Verification" in new Setup {
        mockAppConfig.emailVerificationFrontendBaseUrl returns "localhost"

        mockEmailVerificationConnector.verifyEmail(
          VerifyEmailRequest(
            credId = ggCredentials.providerId,
            continueUrl = controllers.desiDetails.routes.EmailVerificationEndpointController.finishEmailVerification.absoluteURL(),
            origin = "HMRC Agent Services",
            deskproServiceName = None,
            accessibilityStatementUrl = "",
            email = Some(Email("new@email.com", controllers.desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress.absoluteURL())),
            lang = Some(Lang("en").code),
            backUrl = Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.absoluteURL()),
            pageTitle = None
          )
        )(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(Some(VerifyEmailResponse("/emailVerificationUrl")))

        val result = TestService.initialiseEmailVerificationJourney(ggCredentials.providerId, "new@email.com", Lang("en"))

        await(result) mustBe "localhost/emailVerificationUrl"
      }
    }

    "return a relative redirectUri" when {
      "successfully called out to Email Verification" in new Setup {
        mockAppConfig.emailVerificationFrontendBaseUrl returns ""

        mockEmailVerificationConnector.verifyEmail(
          VerifyEmailRequest(
            credId = ggCredentials.providerId,
            continueUrl = controllers.desiDetails.routes.EmailVerificationEndpointController.finishEmailVerification.url,
            origin = "HMRC Agent Services",
            deskproServiceName = None,
            accessibilityStatementUrl = "",
            email = Some(Email("new@email.com", controllers.desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress.url)),
            lang = Some(Lang("en").code),
            backUrl = Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.url),
            pageTitle = None
          )
        )(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(Some(VerifyEmailResponse("/emailVerificationUrl")))

        val result = TestService.initialiseEmailVerificationJourney(ggCredentials.providerId, "new@email.com", Lang("en"))

        await(result) mustBe "/emailVerificationUrl"
      }
    }

    "throw an exception" when {
      "failed calling out to Email Verification" in new Setup {
        mockAppConfig.emailVerificationFrontendBaseUrl returns ""

        mockEmailVerificationConnector.verifyEmail(
          VerifyEmailRequest(
            credId = ggCredentials.providerId,
            continueUrl = controllers.desiDetails.routes.EmailVerificationEndpointController.finishEmailVerification.url,
            origin = "HMRC Agent Services",
            deskproServiceName = None,
            accessibilityStatementUrl = "",
            email = Some(Email("new@email.com", controllers.desiDetails.routes.UpdateEmailAddressController.showChangeEmailAddress.url)),
            lang = Some(Lang("en").code),
            backUrl = Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.url),
            pageTitle = None
          )
        )(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(None)

        intercept[InternalServerException]{
          await(TestService.initialiseEmailVerificationJourney(ggCredentials.providerId, "new@email.com", Lang("en")))
        }
      }
    }
  }

  "getEmailVerificationStatus" should {
    "return EmailIsAlreadyVerified" when {
      "the user has already verified their email" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockEmailVerificationConnector.checkEmail(ggCredentials.providerId)(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(
          Some(VerificationStatusResponse(List(CompletedEmail("new@email.com", verified = true, locked = false))))
        )

        val result = TestService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)

        await(result) mustBe EmailIsAlreadyVerified
      }
    }

    "return EmailIsLocked" when {
      "the endpoint returns the email is locked" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockEmailVerificationConnector.checkEmail(ggCredentials.providerId)(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(
          Some(VerificationStatusResponse(List(CompletedEmail("new@email.com", verified = false, locked = true))))
        )

        val result = TestService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)

        await(result) mustBe EmailIsLocked
      }
    }

    "return EmailNeedsVerifying" when {
      "there is no list of emails returned from email verification" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)
        mockEmailVerificationConnector.checkEmail(ggCredentials.providerId)(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(
          Some(VerificationStatusResponse(List.empty))
        )

        val result = TestService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)

        await(result) mustBe EmailNeedsVerifying
      }
    }

    "return EmailHasNotChanged" when {
      "the user entered email is the same as the stored one" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockEmailVerificationConnector.checkEmail(ggCredentials.providerId)(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(
          Some(VerificationStatusResponse(List(CompletedEmail("abc@abc.com", verified = false, locked = false))))
        )

        val result = TestService.getEmailVerificationStatus("abc@abc.com", ggCredentials.providerId)

        await(result) mustBe EmailHasNotChanged
      }
    }


    "throw an exception" when {
      "the call to get agent record fails" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.failed(UpstreamErrorResponse("no agent record found", 500))

        intercept[UpstreamErrorResponse]{
          await(TestService.getEmailVerificationStatus("abc@abc.com", ggCredentials.providerId))
        }

      }
      "the call to check email verification status fails" in new Setup {
        mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

        mockEmailVerificationConnector.checkEmail(ggCredentials.providerId)(*[HeaderCarrier], *[ExecutionContext]) returns Future.failed(new Exception("Something went wrong"))

        intercept[Exception]{
          await(TestService.getEmailVerificationStatus("abc@abc.com", ggCredentials.providerId))
        }
      }
    }
  }

}
