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

package uk.gov.hmrc.agentservicesaccount.controllers

import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.data.Form
import play.api.http.MimeTypes.HTML
import play.api.http.Status.{FORBIDDEN, OK, SEE_OTHER}
import play.api.i18n.Messages
import play.api.libs.json.Writes
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.stubMessagesControllerComponents
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.models.UpdateMoneyLaunderingSupervisionDetails
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, CredentialRole, Enrolment, EnrolmentIdentifier, Enrolments, User, Nino => _}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class UpdateMoneyLaunderingSupervisionControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val local_valid_date_stub: LocalDate = LocalDate.now.plusMonths(6)

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri)
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withSession(SessionKeys.sessionId -> "session-x")

  //TODO move auth/suspend actions to common file for all unit tests
  val mockAcaConnector: AgentClientAuthorisationConnector = mock[AgentClientAuthorisationConnector]
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
    Future.successful(new ~(new ~(new ~(new ~(
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
    protected val mockAmlsLoader: AMLSLoader = mock[AMLSLoader]
    protected val mockCacheService: SessionCacheService = mock[SessionCacheService]
    protected val mockActions =
      new Actions(mockAcaConnector, authActions, actionBuilder)

    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()
    protected val view: update_money_laundering_supervision_details = mock[update_money_laundering_supervision_details]

    object TestController extends UpdateMoneyLaunderingSupervisionController(mockAmlsLoader, mockActions, mockCacheService, view, cc)(mockAppConfig, ec)
  }

  "showUpdateMoneyLaunderingSupervision" should {
    "return Ok and show the 'What are your money laundering supervision registration details?' page" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns true

      view.apply(*[Form[UpdateMoneyLaunderingSupervisionDetails]], *[Map[String, String]])(*[Messages], *[Request[Any]], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.showUpdateMoneyLaunderingSupervision(FakeRequest())

      Helpers.status(response) mustBe OK
    }
    "return Forbidden when feature flag is off" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAppConfig.enableNonHmrcSupervisoryBody returns false

      view.apply(*[Form[UpdateMoneyLaunderingSupervisionDetails]], *[Map[String, String]])(*[Messages], *[Request[Any]], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.showUpdateMoneyLaunderingSupervision(FakeRequest())

      Helpers.status(response) mustBe FORBIDDEN
    }
  }

  "submitUpdateMoneyLaunderingSupervision" should {
    "redirect to manage-account/update-money-laundering-supervision" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAmlsLoader.load("/amls-no-hmrc.csv") returns Map("AAA" -> "test")

      mockAppConfig.enableNonHmrcSupervisoryBody returns true
      view.apply(*[Form[UpdateMoneyLaunderingSupervisionDetails]], *[Map[String, String]])(*[Messages], *[Request[Any]], *[AppConfig]) returns Html("")

      mockCacheService.put(BODY, *[String])(*[Writes[String]], *[Request[Any]]) returns Future.successful(("body", "test"))
      mockCacheService.put(REG_NUMBER, *[String])(*[Writes[String]], *[Request[Any]]) returns Future.successful(("number", "test"))
      mockCacheService.put(END_DATE, *[LocalDate])(*[Writes[LocalDate]], *[Request[Any]]) returns Future.successful(("endDate", ""))

      val response: Future[Result] = TestController.submitUpdateMoneyLaunderingSupervision(
        fakeRequest("POST")
          .withFormUrlEncodedBody(
            "body" -> "AAA",
            "number" -> "987987",
            "endDate.day" -> local_valid_date_stub.getDayOfMonth.toString,
            "endDate.month" -> local_valid_date_stub.getMonthValue.toString,
            "endDate.year" -> local_valid_date_stub.getYear.toString)
      )

      Helpers.status(response) mustBe SEE_OTHER
      Helpers.redirectLocation(response).get mustBe routes.AmlsConfirmationController.showUpdatedAmlsConfirmationPage.url // redirects to confirmation page
    }
    "return form with errors" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()
      mockAmlsLoader.load("/amls-no-hmrc.csv") returns Map("AAA" -> "test")

      mockAppConfig.enableNonHmrcSupervisoryBody returns true
      view.apply(*[Form[UpdateMoneyLaunderingSupervisionDetails]], *[Map[String, String]])(*[Messages], *[Request[Any]], *[AppConfig]) returns Html("")

      val response: Future[Result] = TestController.submitUpdateMoneyLaunderingSupervision(
        fakeRequest("POST")
          .withFormUrlEncodedBody("body" -> "") /* with empty form body */)

      Helpers.status(response) mustBe OK
      Helpers.contentType(response).get mustBe HTML
    }
  }
}