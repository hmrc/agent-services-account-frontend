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

package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.{SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientAuthorisationConnectorSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-suspension.port" -> wireMockPort,
        "auditing.enabled" -> false
      )

  private lazy val connector = new AgentClientAuthorisationConnector(new URL(s"http://localhost:$wireMockPort"), app.injector.instanceOf[HttpClient])
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val metrics: Metrics = app.injector.instanceOf[Metrics]

  val arn = Arn("TARN0000001")

  "getSuspensionDetails" should {
    "return the suspension details for a given agent" in {
      val suspensionDetails = SuspensionDetails(suspensionStatus = true, Some(Set("ITSA")))
      givenSuspensionStatus(suspensionDetails)
      await(connector.getSuspensionDetails()) shouldBe suspensionDetails
    }

    "return false suspension details when no status is found" in {
      givenSuspensionStatusNotFound
      await(connector.getSuspensionDetails()) shouldBe SuspensionDetails(suspensionStatus = false, None)
    }

    "return not found error response when no agent record is found" in {
      givenAgentRecordNotFound
      intercept[SuspensionDetailsNotFound] {
        await(connector.getSuspensionDetails())
      }.getMessage shouldBe "No record found for this agent"
    }
  }

}
