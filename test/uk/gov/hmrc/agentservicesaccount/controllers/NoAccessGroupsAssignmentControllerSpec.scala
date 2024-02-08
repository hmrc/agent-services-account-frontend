/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.i18n.Messages
import play.api.mvc.{DefaultActionBuilderImpl, MessagesControllerComponents, Request, Result}
import play.api.test.Helpers.stubMessagesControllerComponents
import play.api.test.{DefaultAwaitTimeout, FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.{Actions, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.views.html.pages.admin_access_for_access_groups
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AuthConnector, Nino => _, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class NoAccessGroupsAssignmentControllerSpec extends PlaySpec
  with DefaultAwaitTimeout
  with IdiomaticMockito
  with ArgumentMatchersSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

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
      new Actions(mockAcaConnector, authActions, actionBuilder)

    protected val mockView: admin_access_for_access_groups = mock[admin_access_for_access_groups]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController extends NoAccessGroupsAssignmentController(mockActions, mockView)(mockAppConfig, cc)
  }

  "show admin access information" should {
    "display the page if user is an admin" in new Setup {
      givenAuthorisedAgent(User)
      givenNotSuspended()

      mockView.apply()(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showAdminAccessInformation()(fakeRequest)

      Helpers.status(result) mustBe OK
    }
    "return forbidden if user is an Assistant" in new Setup {
      givenAuthorisedAgent(Assistant)
      givenNotSuspended()

      mockView.apply()(*[Request[Any]], *[Messages], *[AppConfig]) returns Html("")

      val result: Future[Result] = TestController.showAdminAccessInformation()(fakeRequest)

      Helpers.status(result) mustBe FORBIDDEN
    }
  }

}
