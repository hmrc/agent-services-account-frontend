/*
 * Copyright 2018 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq => eqs}
import org.mockito.Mockito.{verify, when}
import org.slf4j.Logger
import play.api.LoggerLike
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.support.{AkkaMaterializerSpec, ResettingMockitoSugar}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionsSpec extends UnitSpec with ResettingMockitoSugar with AkkaMaterializerSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockAuthConnector = resettingMock[PlayAuthConnector]

  val slf4jLogger = resettingMock[Logger]
  val logger = new LoggerLike {
    override val logger: Logger = slf4jLogger
  }

  def mockAuth(affinityGroup: AffinityGroup = AffinityGroup.Agent, enrolment: Set[Enrolment], credentialRole: CredentialRole = User) =
    when(mockAuthConnector.authorise(any(), any[Retrieval[~[~[Enrolments, Option[AffinityGroup]], Option[CredentialRole]]]]())(any(), any()))
      .thenReturn(Future successful new ~[~[Enrolments, Option[AffinityGroup]], Option[CredentialRole]](new ~(Enrolments(enrolment), Some(affinityGroup)), Some(credentialRole)))

  def mockAuthNotLoggedIn(): Unit =
    when(mockAuthConnector.authorise(any(), any[Retrieval[~[Enrolments, Option[AffinityGroup]]]]())(any(), any()))
      .thenReturn(Future.failed(new MissingBearerToken))

  val arn = "TARN0000001"
  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  val otherEnrolment: Enrolment = agentEnrolment.copy(key = "IR-PAYE")

  val externalUrls: ExternalUrls = mock[ExternalUrls]
  val completeGgSignInUrl = "http://example.com/agent-subscription"
  when(externalUrls.agentSubscriptionUrl).thenReturn(completeGgSignInUrl)

  val authActions = new AuthActions(logger, externalUrls, mockAuthConnector)

  class TestAuth() {
    def testAuthActions(): Action[AnyContent] = Action.async {
      implicit request =>
        authActions.authorisedWithAgent { agent =>
            Future.successful(Ok(Json.toJson(agent.arn)))
        } map { maybeResult =>
          maybeResult.getOrElse(authActions.redirectToAgentSubscriptionGgSignIn)
        } recover {
          case _: NoActiveSession =>
            authActions.redirectToAgentSubscriptionGgSignIn
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
      mockAuthNotLoggedIn()

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(completeGgSignInUrl)

    }

    "redirect to GG sign in if logged in user is not an HMRC-AS-AGENT agent" in {
      mockAuth(enrolment = Set(otherEnrolment))

      val result: Future[Result] = testAuthImpl.testAuthActions().apply(FakeRequest())
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(completeGgSignInUrl)
    }
  }

  "authorisedWithAgent" should {
    "call body if the user has an Activated HMRC-AS-AGENT enrolment" in {
      mockAuth(AffinityGroup.Agent, Set(agentEnrolment))

      await(authActions.authorisedWithAgent(_ => Future successful "OK")) shouldBe Some("OK")
    }

    "supply the ARN to the body in an AgentInfo if the user has an Activated HMRC-AS-AGENT enrolment" in {
      mockAuth(AffinityGroup.Agent, Set(agentEnrolment))
      await(authActions.authorisedWithAgent(agentInfo => Future successful agentInfo.arn)) shouldBe Some(Arn(arn))
    }

    "return None if the user has a Pending HMRC-AS-AGENT enrolment" in {
      mockAuth(AffinityGroup.Agent, Set(agentEnrolment.copy(state = "Pending")))

      await(authActions.authorisedWithAgent(_ => Future successful "OK")) shouldBe None
    }

    "return None if the user has no HMRC-AS-AGENT enrolment" in {
      mockAuth(AffinityGroup.Agent, Set(otherEnrolment))

      await(authActions.authorisedWithAgent(_ => Future successful "OK")) shouldBe None
    }

    "return None and log a warning if the user has an Activated HMRC-AS-AGENT enrolment with no AgentReferenceNumber identifier" in {
      mockAuth(AffinityGroup.Agent, Set(agentEnrolment.copy(identifiers = Seq())))
      when(slf4jLogger.isWarnEnabled).thenReturn(true)

      await(authActions.authorisedWithAgent(agentInfo => Future successful agentInfo.arn)) shouldBe None

      verify(slf4jLogger).warn("No AgentReferenceNumber found in HMRC-AS-AGENT enrolment - this should not happen. Denying access.")
    }

    "return None if the user has affinity group != Agent" in {
      mockAuth(AffinityGroup.Individual, Set(agentEnrolment))

      await(authActions.authorisedWithAgent(_ => Future successful "OK")) shouldBe None
    }

  }
}
