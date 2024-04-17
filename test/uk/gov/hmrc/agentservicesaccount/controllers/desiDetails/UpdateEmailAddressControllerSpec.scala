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

import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Writes
import play.api.mvc.{AnyContentAsFormUrlEncoded, DefaultActionBuilderImpl, MessagesControllerComponents, Request}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers
import uk.gov.hmrc.agentservicesaccount.controllers.EMAIL_PENDING_VERIFICATION
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.emailverification.{EmailIsAlreadyVerified, EmailIsLocked, EmailNeedsVerifying}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, EmailVerificationService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.{email_locked, update_email}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class UpdateEmailAddressControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAgentClientAuthorisationConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val mockDraftDetailsService: DraftDetailsService = mock[DraftDetailsService]
    protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(mockAgentClientAuthorisationConnector, mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockPendingChangeOfDetailsRepository = mock[PendingChangeOfDetailsRepository]
    protected val mockUpdateEmailView: update_email = mock[update_email]
    protected val mockEmailLockedView: email_locked = mock[email_locked]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController extends UpdateEmailAddressController(
      mockActions,
      mockSessionCache,
      mockDraftDetailsService,
      mockEmailVerificationService,
      mockUpdateEmailView,
      mockEmailLockedView,
      cc
    )(mockAppConfig, ec, mockPendingChangeOfDetailsRepository)
  }


  "GET /manage-account/contact-details/new-email" should {
    "display the enter email address page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockUpdateEmailView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

      val result = TestController.showChangeEmailAddress()(fakeRequest)

      status(result) mustBe OK
    }
  }

  "POST /manage-account/contact-details/new-email" should {
    implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/").withFormUrlEncodedBody("emailAddress" -> "new@email.com")

    "(if the email is already verified) store the new email address in session and redirect to review new details page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)(*[HeaderCarrier]) returns Future.successful(EmailIsAlreadyVerified)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[Request[_]], *[HeaderCarrier]) returns Future.successful(())

      mockSessionCache.delete[String](EMAIL_PENDING_VERIFICATION)(*[Request[_]]) returns Future.successful(())


      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.url)
    }


    "(if the email is locked) redirect to the email-locked page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)(*[HeaderCarrier]) returns Future.successful(EmailIsLocked)


      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(controllers.desiDetails.routes.UpdateEmailAddressController.showEmailLocked.url)
    }

    "(if the email is unverified) redirect to the verify-email external journey" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)(*[HeaderCarrier]) returns Future.successful(EmailNeedsVerifying)

      mockSessionCache.put[String](EMAIL_PENDING_VERIFICATION, "new@email.com")(*[Writes[String]], *[Request[_]]) returns Future.successful(SessionKeys.sessionId -> "session-123")

      mockEmailVerificationService.initialiseEmailVerificationJourney(ggCredentials.providerId, "new@email.com", cc.langs.availables.head)(*[HeaderCarrier], *[Request[_]]) returns Future.successful("/fake-verify-email-journey")


      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result).get must include("/fake-verify-email-journey")
    }

    "display an error if the data submitted is invalid" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockUpdateEmailView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")


      val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/").withFormUrlEncodedBody("emailAddress" -> "invalid at email dot com")

      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

}
