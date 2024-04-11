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
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{status, stubMessagesControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.controllers.DRAFT_NEW_CONTACT_DETAILS
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeOfDetailsRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.enter_sa_code
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.domain.SaUtr
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

class EnterSACodeControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar
  with TestConstants {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()


  private val arn: Arn = Arn("arn")
  private val credentialRole: User.type = User
  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment("HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None))
  private val ggCredentials: Credentials =
    Credentials("ggId", "GovernmentGateway")

  private val authResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[User.type]] =
    Future.successful(new~(new~(new~(new~(
      Enrolments(agentEnrolment), Some(ggCredentials)),
      Some(Email("test@email.com"))),
      Some(Name(Some("Troy"), Some("Barnes")))),
      Some(credentialRole)))

  private val suspensionDetailsResponse: Future[SuspensionDetails] = Future.successful(SuspensionDetails(suspensionStatus = false, None))

  private val agencyDetails = AgencyDetails(
    agencyName = Some("My Agency"),
    agencyEmail = Some("abc@abc.com"),
    agencyTelephone = Some("07345678901"),
    agencyAddress = Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"))
  )

  private val emptyOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = false,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  private val saChangesOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = true,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  private val desiDetailsWithEmptyOtherServices = DesignatoryDetails(agencyDetails, emptyOtherServices)

  private val desiDetailsSaChangesOtherServices = DesignatoryDetails(agencyDetails, saChangesOtherServices)


  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockAuthConnector: AuthConnector = mock[AuthConnector]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)

    protected val mockAgentClientAuthorisationConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
    protected val mockActions =
      new Actions(mockAgentClientAuthorisationConnector, mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val mockPendingChangeOfDetailsRepository = mock[PendingChangeOfDetailsRepository]
    protected val mockView: enter_sa_code = mock[enter_sa_code]
    protected val mockSessionCache: SessionCacheService = mock[SessionCacheService]
    protected val mockAcaConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController extends EnterSACodeController(mockActions, mockSessionCache, mockAcaConnector, mockView, mockPendingChangeOfDetailsRepository)(mockAppConfig, cc, ec)
  }

  "showPage" should {
    "display the page" in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showPage (fakeRequest)

      status(result) mustBe OK

    }

    "return Forbidden when the feature flag is off" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns false

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      val result: Future[Result] = TestController.showPage(fakeRequest)

      status(result) mustBe NOT_FOUND

    }
  }

  "onSubmit" should {

    "return 303 SEE_OTHER and store data for UK agent " in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)(*[Reads[DesignatoryDetails]], *[Request[Any]]) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

      mockSessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(saChanges = SaChanges(true, Some(SaUtr("123456"))))))(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("saCode" -> "123456"))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/contact-details/apply-code-CT"
    }

    "return 303 SEE_OTHER and DO NOT store data for continueWithoutCode " in new Setup {
      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)(*[Reads[DesignatoryDetails]], *[Request[Any]]) returns Future.successful(Some(desiDetailsSaChangesOtherServices))

      mockSessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, desiDetailsWithEmptyOtherServices)(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.continueWithoutSaCode(
        FakeRequest("GET", "/").withFormUrlEncodedBody("body" -> ""))

      status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result).get mustBe "/agent-services-account/manage-account/contact-details/apply-code-CT"
    }


    "return 200 OK when invalid form submission" in new Setup {

      mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
        *[HeaderCarrier],
        *[ExecutionContext]) returns authResponse

      mockAppConfig.enableChangeContactDetails returns true

      mockAgentClientAuthorisationConnector.getAgentRecord()(*[HeaderCarrier], *[ExecutionContext]) returns Future.successful(agentRecord)

      mockPendingChangeOfDetailsRepository.find(arn) returns Future.successful(None)

      mockSessionCache.get[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS)(*[Reads[DesignatoryDetails]], *[Request[Any]]) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

      mockSessionCache.put[DesignatoryDetails](DRAFT_NEW_CONTACT_DETAILS, desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(saChanges = SaChanges(true, None))))(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

      mockView.apply(*[Form[String]])(*[Messages], *[Request[_]], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.onSubmit(
        FakeRequest("POST", "/").withFormUrlEncodedBody("body" -> ""))

      status(result) mustBe OK
    }
  }

}
