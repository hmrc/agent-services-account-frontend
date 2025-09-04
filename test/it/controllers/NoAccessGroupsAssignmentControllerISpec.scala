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

package it.controllers

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.http.Status.FORBIDDEN
import play.api.http.Status.OK
import play.api.http.Status.SEE_OTHER
import play.api.i18n.Messages
import play.api.mvc._
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers.stubMessagesControllerComponents
import play.twirl.api.Html
import support.TestConstants
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.actions.AuthActions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.controllers.NoAccessGroupsAssignmentController
import uk.gov.hmrc.agentservicesaccount.views.html.pages.admin_access_for_access_groups
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{Nino => _, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class NoAccessGroupsAssignmentControllerISpec
extends PlaySpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  private val fakeRequest = FakeRequest()

  // TODO move auth/suspend actions to common file for all unit tests
  val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]

  private def authResponseAgent(credentialRole: CredentialRole): Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[Name] ~ Some[CredentialRole]] =
    Future.successful(
      new ~(
        new ~(
          new ~(
            new ~(
              Enrolments(agentEnrolment),
              Some(ggCredentials)
            ),
            Some(Email("test@email.com"))
          ),
          Some(Name(Some("Troy"), Some("Barnes")))
        ),
        Some(credentialRole)
      )
    )

  def givenAgentRecord = mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]) returns Future.successful(agentRecord)

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  def givenAuthorisedAgent(credentialRole: CredentialRole): ScalaOngoingStubbing[Future[Any]] = {
    mockAuthConnector.authorise(*[Predicate], *[Retrieval[Any]])(
      *[HeaderCarrier],
      *[ExecutionContext]
    ) returns authResponseAgent(credentialRole)
  }
  trait Setup {

    protected val mockAppConfig: AppConfig = mock[AppConfig]
    protected val mockEnvironment: Environment = mock[Environment]
    protected val authActions =
      new AuthActions(
        mockAppConfig,
        mockAuthConnector,
        mockEnvironment
      )
    protected val actionBuilder = new DefaultActionBuilderImpl(Helpers.stubBodyParser())

    protected val mockActions =
      new Actions(
        mockAgentAssuranceConnector,
        authActions,
        actionBuilder
      )

    protected val mockView: admin_access_for_access_groups = mock[admin_access_for_access_groups]
    protected val cc: MessagesControllerComponents = stubMessagesControllerComponents()

    object TestController
    extends NoAccessGroupsAssignmentController(mockActions, mockView)(mockAppConfig, cc)

  }

  "redirect for no assignment" should {
    "redirect admin user to show admin access information" in new Setup {
      givenAuthorisedAgent(User)
      givenAgentRecord

      val result: Future[Result] = TestController.redirectForNoAssignment(fakeRequest)

      Helpers.status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result) mustBe Some("/agent-services-account/manage-account/administrative-access")
    }

    "redirect standard user to administrators list" in new Setup {
      givenAuthorisedAgent(Assistant)

      val result: Future[Result] = TestController.redirectForNoAssignment(fakeRequest)

      Helpers.status(result) mustBe SEE_OTHER
      Helpers.redirectLocation(result) mustBe Some("/agent-services-account/administrators")
    }
  }

  "show admin access information" should {
    "display the page if user is an admin" in new Setup {
      givenAuthorisedAgent(User)
      givenAgentRecord

      mockView.apply()(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.showAdminAccessInformation()(fakeRequest)

      Helpers.status(result) mustBe OK
    }
    "return forbidden if user is an Assistant" in new Setup {
      givenAuthorisedAgent(Assistant)
      givenAgentRecord

      mockView.apply()(
        *[Request[Any]],
        *[Messages],
        *[AppConfig]
      ) returns Html("")

      val result: Future[Result] = TestController.showAdminAccessInformation()(fakeRequest)

      Helpers.status(result) mustBe FORBIDDEN
    }
  }

}
