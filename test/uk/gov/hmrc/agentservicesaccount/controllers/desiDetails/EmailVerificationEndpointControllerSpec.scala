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
import play.api.libs.json.Reads
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, RequestHeader}
import play.api.test.Helpers._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY, EMAIL_PENDING_VERIFICATION}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.emailverification.EmailIsAlreadyVerified
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepository
import uk.gov.hmrc.agentservicesaccount.services.{DraftDetailsService, EmailVerificationService, SessionCacheService}
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationEndpointControllerSpec extends PlaySpec
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
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockDraftDetailsService: DraftDetailsService = mock[DraftDetailsService]
    protected val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockPendingChangeRequestRepository = mock[PendingChangeRequestRepository]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController extends EmailVerificationEndpointController(
      mockActions,
      mockSessionCache,
      mockDraftDetailsService,
      cc
    )(mockAppConfig, ec, mockPendingChangeRequestRepository, mockEmailVerificationService)
  }


  "POST manage-account/contact-details/email-verification-finish (successful verification journey has finished)" should {
    "store the new email address in session and redirect to review new details page (also clear email verification cache value)" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockSessionCache.get(CURRENT_SELECTED_CHANGES)(*[Reads[Set[String]]], *[RequestHeader]) returns Future.successful(Some(Set("email")))
      mockSessionCache.get(DRAFT_NEW_CONTACT_DETAILS)(*[Reads[DesignatoryDetails]], *[RequestHeader]) returns Future.successful(Some(DesignatoryDetails(
        agencyDetails = agentRecord.agencyDetails.get.copy(
          agencyEmail = Some("new@email.com")
        ),
        otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
      )))

      mockSessionCache.get(DRAFT_SUBMITTED_BY)(*[Reads[YourDetails]], *[RequestHeader]) returns Future.successful(Some(
        YourDetails(fullName = "John Tester", telephone = "078187777831")
      ))
      // TODO check this returns the correct data

      mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

      mockPendingChangeRequestRepository.find(arn)(*[RequestHeader]) returns Future.successful(None)

      mockSessionCache.get[String](EMAIL_PENDING_VERIFICATION)(*[Reads[String]], *[Request[Any]]) returns Future.successful(Some("new@email.com"))

      mockEmailVerificationService.getEmailVerificationStatus("new@email.com", ggCredentials.providerId)(*[RequestHeader]) returns Future.successful(EmailIsAlreadyVerified)

      mockDraftDetailsService.updateDraftDetails(*[DesignatoryDetails => DesignatoryDetails])(*[RequestHeader]) returns Future.successful(())

      mockSessionCache.delete[String](EMAIL_PENDING_VERIFICATION)(*[RequestHeader]) returns Future.successful(())

      val result = TestController.finishEmailVerification()(fakeRequest)

      status(result) mustBe SEE_OTHER
      header("Location", result) mustBe Some(controllers.desiDetails.routes.CheckYourAnswersController.showPage.url)
    }
  }

}
