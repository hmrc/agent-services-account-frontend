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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.http.MimeTypes.HTML
import play.api.http.Status.{FORBIDDEN, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, stubMessagesControllerComponents}
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentAssuranceConnector, AgentClientAuthorisationConnector}
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.is_amls_hmrc
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}


class AmlsIsHmrcControllerSpec extends PlaySpec with IdiomaticMockito with ArgumentMatchersSugar {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  //TODO move auth/suspend actions to common file for all unit tests
  val mockAcaConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
  val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]
  val notSuspendedDetails: Future[SuspensionDetails] = Future successful SuspensionDetails(suspensionStatus = false, None)
  def givenNotSuspended(): ScalaOngoingStubbing[Future[SuspensionDetails]] = {
    mockAcaConnector.getSuspensionDetails()(
      *[HeaderCarrier],
      *[ExecutionContext]) returns notSuspendedDetails
  }

  private val arn = Arn("BARN1234567")

  private val agentEnrolment: Set[Enrolment] = Set(
    Enrolment("HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None))

  private val ggCredentials: Credentials =
    Credentials("ggId", "GovernmentGateway")

  private def authResponseAgent(credentialRole: CredentialRole): Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[CredentialRole]] =
    Future.successful(new~(new~(new~(new~(
      Enrolments(agentEnrolment), Some(ggCredentials)),
      Some(Email("test@email.com"))),
      Some(Name(Some("Troy"), Some("Barnes")))),
      Some(credentialRole)))

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  def givenAuthorisedAgent(credentialRole: CredentialRole): ScalaOngoingStubbing[Future[Any]] = {
    mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
      *[HeaderCarrier],
      *[ExecutionContext]) returns authResponseAgent(credentialRole)
  }


  trait Setup {
    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions = new AuthActions(mockAppConfig, mockAuthConnector, mockEnvironment)
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())
    protected val mockActions =
      new Actions(mockAcaConnector, mockAgentAssuranceConnector, authActions, actionBuilder)

    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val view: is_amls_hmrc = mock[is_amls_hmrc]
    object TestController extends AmlsIsHmrcController(mockActions, view, cc)(mockAppConfig)
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri)
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withSession(SessionKeys.sessionId -> "session-x")


  "showAmlsIsHMRC" should {
    "return Ok and show the 'is AMLS body HMRC?' page" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      view.apply(*[Form[Boolean]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.showAmlsIsHmrc(FakeRequest())

      Helpers.status(response) mustBe OK
    }

    "return Forbidden when feature flag is off" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns false

      view.apply(*[Form[Boolean]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.showAmlsIsHmrc(FakeRequest())

      Helpers.status(response) mustBe FORBIDDEN
    }

  }

  "submitAmlsIsHmrc" should {
    "redirect to [not-implemented-hmrc-page]" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      val response: Future[Result] = TestController.submitAmlsIsHmrc(
        fakeRequest("POST")
          .withFormUrlEncodedBody("accept" -> "true")
      )

      Helpers.status(response) mustBe SEE_OTHER
      Helpers.redirectLocation(response).get mustBe "not-implemented-hmrc-page"
    }

    "redirect to manage-account/update-money-laundering-supervision" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      val response: Future[Result] = TestController.submitAmlsIsHmrc(
        fakeRequest("POST")
          .withFormUrlEncodedBody("accept" -> "false")
      )
      Helpers.status(response) mustBe SEE_OTHER
      Helpers.redirectLocation(response).get mustBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"
    }

    "return form with errors" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns true
      view.apply(*[Form[Boolean]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.submitAmlsIsHmrc(fakeRequest("POST").withFormUrlEncodedBody("accept" -> "") /* with empty form body */)

      Helpers.status(response) mustBe OK
      Helpers.contentType(response).get mustBe HTML
    }

    "return Forbidden when feature flag is off" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns false
      view.apply(*[Form[Boolean]])(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.submitAmlsIsHmrc(fakeRequest("POST").withFormUrlEncodedBody("accept" -> "true") /* doesn't matter */)

      Helpers.status(response) mustBe FORBIDDEN
    }
  }

}
