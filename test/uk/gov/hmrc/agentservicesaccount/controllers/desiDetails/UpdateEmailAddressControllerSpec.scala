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

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.DefaultActionBuilderImpl
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.test.Helpers._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsAlreadyVerified
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsLocked
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailNeedsVerifying
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.DraftDetailsService
import uk.gov.hmrc.agentservicesaccount.services.EmailVerificationService
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.email_locked
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.update_email
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class UpdateEmailAddressControllerSpec
extends PlaySpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  trait Setup {

    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions =
      new AuthActions(
        mockAppConfig,
        mockAuthConnector,
        mockEnvironment
      )

    protected val mockDraftDetailsService: DraftDetailsService = mock[DraftDetailsService]
    protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(
        mockAgentAssuranceConnector,
        authActions,
        actionBuilder
      )

    protected val mockPendingChangeRequestRepository = mock[PendingChangeRequestRepository]
    protected val mockUpdateEmailView: update_email = mock[update_email]
    protected val mockEmailLockedView: email_locked = mock[email_locked]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    mockSessionCache.delete[String](emailPendingVerificationKey)(*[RequestHeader]) returns Future.successful(())
    mockSessionCache.delete[Set[String]](currentSelectedChangesKey)(*[RequestHeader]) returns Future.successful(())
    mockSessionCache.delete[YourDetails](draftSubmittedByKey)(*[RequestHeader]) returns Future.successful(())
    mockSessionCache.delete[DesignatoryDetails](draftNewContactDetailsKey)(*[RequestHeader]) returns Future.successful(())

    object TestController
    extends UpdateEmailAddressController(
      mockActions,
      mockSessionCache,
      mockDraftDetailsService,
      mockEmailVerificationService,
      mockUpdateEmailView,
      mockEmailLockedView,
      cc
    )(
      mockAppConfig,
      ec,
      mockPendingChangeRequestRepository
    )

  }

  "GET /manage-account/contact-details/new-email" should {
    "display the enter email address page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockUpdateEmailView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")

      val result = TestController.showChangeEmailAddress()(fakeRequest)

      status(result) mustBe OK
    }
  }

  "POST /manage-account/contact-details/new-email" should {
    implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/").withFormUrlEncodedBody("emailAddress" -> "new@email.com")

    "(if the email is already verified) store the new email address in session and redirect to review new details page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus(
        "new@email.com",
        ggCredentials.providerId
      )(*[RequestHeader]) returns Future.successful(EmailIsAlreadyVerified)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockSessionCache.delete[String](emailPendingVerificationKey)(*[RequestHeader]) returns Future.successful(())
      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))
      mockSessionCache.get(draftSubmittedByKey)(*[Reads[YourDetails]], *[RequestHeader]) returns
        Future.successful(Some(YourDetails(fullName = "John Tester", telephone = "08982383777")))
      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get.copy(agencyEmail = Some("new@email.com")),
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))

      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.url)
    }

    "(if the email is locked) redirect to the email-locked page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus(
        "new@email.com",
        ggCredentials.providerId
      )(*[RequestHeader]) returns Future.successful(EmailIsLocked)
      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get,
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))

      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(controllers.desiDetails.routes.UpdateEmailAddressController.showEmailLocked.url)
    }

    "(if the email is unverified) redirect to the verify-email external journey" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockEmailVerificationService.getEmailVerificationStatus(
        "new@email.com",
        ggCredentials.providerId
      )(*[RequestHeader]) returns Future.successful(EmailNeedsVerifying)

      mockSessionCache.put[String](emailPendingVerificationKey, "new@email.com")(
        *[Writes[String]],
        *[RequestHeader]
      ) returns Future.successful(SessionKeys.sessionId -> "session-123")

      mockEmailVerificationService.initialiseEmailVerificationJourney(
        ggCredentials.providerId,
        "new@email.com",
        cc.langs.availables.head
      )(*[RequestHeader]) returns Future.successful("/fake-verify-email-journey")
      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get,
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))

      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe SEE_OTHER
      header("Location", result).get must include("/fake-verify-email-journey")
    }

    "display an error if the data submitted is invalid" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]
      ) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockUpdateEmailView.apply(*[Form[String]])(
        *[Messages],
        *[RequestHeader],
        *[AppConfig]
      ) returns Html("")
      mockSessionCache.get(currentSelectedChangesKey)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))

      mockSessionCache.get(draftNewContactDetailsKey)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get,
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/").withFormUrlEncodedBody("emailAddress" -> "invalid at email dot com")

      val result = TestController.submitChangeEmailAddress()(request)

      status(result) mustBe BAD_REQUEST
    }
  }

}
