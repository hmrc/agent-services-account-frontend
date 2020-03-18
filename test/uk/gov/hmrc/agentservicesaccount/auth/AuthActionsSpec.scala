/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.auth

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.slf4j.Logger
import play.api.LoggerLike
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.support.{AkkaMaterializerSpec, BaseUnitSpec}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionsSpec extends BaseUnitSpec with AkkaMaterializerSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector: PlayAuthConnector = resettingMock[PlayAuthConnector]

  val slf4jLogger: Logger = resettingMock[Logger]

  val logger: LoggerLike = new LoggerLike {
    override val logger: Logger = slf4jLogger
  }

  def mockAuth(enrolment: Set[Enrolment], credentialRole: CredentialRole = User): OngoingStubbing[Future[Enrolments ~ Option[CredentialRole]]] =
    when(mockAuthConnector.authorise(
      any[Predicate](), any[Retrieval[~[Enrolments, Option[CredentialRole]]]]())(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future successful new ~(Enrolments(enrolment), Some(credentialRole)))

  def mockAuthFailWith(reason: String): OngoingStubbing[Future[Enrolments ~ Option[AffinityGroup]]] =
    when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any[HeaderCarrier](), any[ExecutionContext]()))
      .thenReturn(Future.failed(AuthorisationException.fromString(reason)))

  val arn = "TARN0000001"
  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  val otherEnrolment: Enrolment = agentEnrolment.copy(key = "IR-PAYE")

  val externalUrls: ExternalUrls = mock[ExternalUrls]
  val completeGgSignInUrl = "/gg/sign-in?continue=%2F&origin=agent-services-account-frontend"
  val agentSubscriptionUrl = "/agent-subscription/start"
  def completeGgSignInUrlWithOtac(otacToken: String) =
    s"/gg/sign-in?continue=%2F%3Fp%3D$otacToken&origin=agent-services-account-frontend"
  when(externalUrls.agentSubscriptionUrl).thenReturn(agentSubscriptionUrl)
  when(externalUrls.continueFromGGSignIn).thenReturn(completeGgSignInUrl)

  val authActions = new AuthActions(logger, externalUrls, mockAuthConnector, env, configuration)

  class TestAuth() {
    def testAuthActions(): Action[AnyContent] = Action.async {
      implicit request =>
      authActions.withAuthorisedAsAgent { agent =>
        Future.successful(Ok(Json.toJson(agent.arn)))
      }
    }
  }

  val testAuthImpl = new TestAuth()

  "AuthorisedWithAgentAsync" should {
    "return an agent request" in {
      mockAuth(enrolment = Set(agentEnrolment))
      val result: Result = await(testAuthImpl.testAuthActions().apply(FakeRequest()))
      result.header.status shouldBe OK
      jsonBodyOf(result).as[Arn] shouldBe Arn(arn)
    }

    "redirect to GG sign in if agent is not logged in" in {
      mockAuthFailWith("MissingBearerToken")

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(completeGgSignInUrl)
    }

    "redirect to GG sign in if agent is not logged in with otac parameter" in {
      mockAuthFailWith("MissingBearerToken")

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest().withSession(
        "otacTokenParam" -> "foo"))
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(completeGgSignInUrlWithOtac("foo"))
    }

    "redirect to agent subscription start if logged in user is not an HMRC-AS-AGENT agent" in {
      mockAuth(enrolment = Set(otherEnrolment))

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/agent-subscription/start")
    }

    "return forbidden if the auth provider is not supported" in {
      mockAuthFailWith("UnsupportedAuthProvider")

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 403
    }

    "return forbidden if the affinity group is not supported" in {
      mockAuthFailWith("UnsupportedAffinityGroup")

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 403
    }
  }
}
