/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.paye

import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.connectors.PayeSubscriptionConnector
import uk.gov.hmrc.agentservicesaccount.models.paye._

import scala.concurrent.{ExecutionContext, Future}

class PayeSubscriptionRequestControllerISpec extends ComponentBaseISpec {

  private val confirmPath   = "/paye-subscription/request/confirm"
  private val submittedPath = "/paye-subscription/request/submitted"

  private object TestState {
    var status: PayeStatus =
      PayeStatus(hasSubscription = false, hasRequestInProgress = false)

    var cyaData: PayeCyaData =
      PayeCyaData(
        agentName = "Example Agent Ltd",
        contactName = "Jane Agent",
        telephoneNumber = Some("01632 960 001"),
        emailAddress = Some("jane.agent@example.com"),
        address = PayeAddress(
          line1 = "1 High Street",
          line2 = "Village",
          line3 = Some("County"),
          line4 = None,
          postCode = "AA1 1AA"
        )
      )

    var failStatus: Boolean = false
    var failCyaData: Boolean = false
    var failSubmit: Boolean = false
  }

  private final class TestPayeSubscriptionConnector extends PayeSubscriptionConnector {
    override def getStatus()(implicit ec: ExecutionContext): Future[PayeStatus] =
      if (TestState.failStatus) Future.failed(new RuntimeException("boom-status"))
      else Future.successful(TestState.status)

    override def getCyaData()(implicit ec: ExecutionContext): Future[PayeCyaData] =
      if (TestState.failCyaData) Future.failed(new RuntimeException("boom-cya"))
      else Future.successful(TestState.cyaData)

    override def submitRequest()(implicit ec: ExecutionContext): Future[Unit] =
      if (TestState.failSubmit) Future.failed(new RuntimeException("boom-submit"))
      else Future.successful(())
  }

  override def extraConfig(): Map[String, String] = super.extraConfig()

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(config ++ extraConfig() ++ downstreamServices)
      .overrides(
        bind[PayeSubscriptionConnector].toInstance(new TestPayeSubscriptionConnector)
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    TestState.status = PayeStatus(hasSubscription = false, hasRequestInProgress = false)
    TestState.failStatus = false
    TestState.failCyaData = false
    TestState.failSubmit = false
  }

  s"GET $confirmPath" should {

    "return OK and render the confirmation screen when eligible" in {
      TestState.status = PayeStatus(hasSubscription = false, hasRequestInProgress = false)

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(confirmPath)

      result.status shouldBe OK
      assertPageHasTitle("Check your details before requesting a PAYE subscription")(result)

      result.body should include("Example Agent Ltd")
      result.body should include("Jane Agent")
      result.body should include("Return to your agent services account")
    }

    "redirect to the ASA homepage when not eligible (has subscription)" in {
      TestState.status = PayeStatus(hasSubscription = true, hasRequestInProgress = false)

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(confirmPath)

      result.status shouldBe SEE_OTHER
      result.header("Location").value shouldBe "/agent-services-account/home"
    }

    "redirect to the ASA homepage when not eligible (request in progress)" in {
      TestState.status = PayeStatus(hasSubscription = false, hasRequestInProgress = true)

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(confirmPath)

      result.status shouldBe SEE_OTHER
      result.header("Location").value shouldBe "/agent-services-account/home"
    }

    "return 500 with a technical difficulties page when the connector errors" in {
      TestState.failStatus = true

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(confirmPath)

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.body should include("Sorry, we’re experiencing technical difficulties")
    }
  }

  s"POST $confirmPath" should {

    "redirect to submitted screen when eligible" in {
      TestState.status = PayeStatus(hasSubscription = false, hasRequestInProgress = false)

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = post(confirmPath)(Map.empty)

      result.status shouldBe SEE_OTHER
      result.header("Location").value shouldBe "/agent-services-account/paye-subscription/request/submitted"
    }

    "redirect to the ASA homepage when not eligible" in {
      TestState.status = PayeStatus(hasSubscription = true, hasRequestInProgress = false)

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = post(confirmPath)(Map.empty)

      result.status shouldBe SEE_OTHER
      result.header("Location").value shouldBe "/agent-services-account/home"
    }

    "return 500 with a technical difficulties page when submit fails" in {
      TestState.status = PayeStatus(hasSubscription = false, hasRequestInProgress = false)
      TestState.failSubmit = true

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = post(confirmPath)(Map.empty)

      result.status shouldBe INTERNAL_SERVER_ERROR
      result.body should include("Sorry, we’re experiencing technical difficulties")
    }
  }

  s"GET $submittedPath" should {

    "return OK and render submitted screen" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(submittedPath)

      result.status shouldBe OK
      assertPageHasTitle("PAYE subscription request submitted")(result)
      result.body should include("Return to your agent services account")
    }
  }
}
