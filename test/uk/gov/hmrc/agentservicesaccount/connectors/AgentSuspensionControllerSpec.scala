/*
 * Copyright 2019 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.SuspensionResponse
import uk.gov.hmrc.agentservicesaccount.stubs.AgentSuspensionStubs._
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class AgentSuspensionControllerSpec extends UnitSpec with GuiceOneAppPerSuite with WireMockSupport {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-suspension.port" -> wireMockPort,
        "auditing.enabled" -> false
      )

  private lazy val connector = new AgentSuspensionConnector(new URL(s"http://localhost:$wireMockPort"), app.injector.instanceOf[HttpGet])
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val arn = Arn("TARN0000001")

  "getSuspensionStatus" should {
    "return the suspension status for a given agent" in {
      val suspendedServices = SuspensionResponse(Set("HMRC-MTD-IT"))
      givenSuspensionStatus(arn, suspendedServices)
      await(connector.getSuspensionStatus(arn)) shouldBe suspendedServices
    }

    "return empty Set when no status is found" in {
      givenSuspensionStatusNotFound(arn)
      await(connector.getSuspensionStatus(arn)) shouldBe SuspensionResponse(Set.empty)
    }
  }

}
