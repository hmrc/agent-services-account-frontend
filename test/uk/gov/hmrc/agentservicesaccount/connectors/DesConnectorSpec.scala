/*
 * Copyright 2017 HM Revenue & Customs
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
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.stubs.DesStubs
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}

class DesConnectorSpec extends UnitSpec with GuiceOneAppPerTest with WireMockSupport {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.des.port" -> wireMockPort,
        "microservice.services.auth.port" -> wireMockPort
      )

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val connector = new DesConnector(new URL(s"http://localhost:$wireMockPort"), "authToken", "testEnv", app.injector.instanceOf[HttpGet], app.injector.instanceOf[Metrics])
  private implicit val hc = HeaderCarrier()

  "AgentRecordDetails" should {
    "parse empty json " in {
      AgentRecordDetails.agentRecordDetailsRead.reads(Json.parse("""{}""")) shouldBe JsSuccess(AgentRecordDetails(None))
    }

    "parse json with agencyDetails and agencyName" in {
      AgentRecordDetails.agentRecordDetailsRead.reads(Json.parse(
        """
          |{
          |   "agencyDetails" : {
          |      "agencyName" : "Agency Test"
          |   }
          |}
        """.stripMargin)).get shouldBe AgentRecordDetails(Some(AgencyDetails("Agency Test")))
    }
  }

  "DesConnector" should {
    "return agency name when DES returns 200 and agency name is present" in {
      DesStubs.givenDESRespondsWithAgencyName("TestARN", "ACME")
      await(connector.getAgencyName(Arn("TestARN"))) shouldBe Some("ACME")
    }

    "return None when DES returns 200 but agencyDetails is not present" in {
      DesStubs.givenDESRespondsWithoutAgencyDetails("TestARN")
      await(connector.getAgencyName(Arn("TestARN"))) shouldBe None
    }

    "return None when DES returns 404" in {
      DesStubs.givenDESReturnsError("TestARN", 404)
      await(connector.getAgencyName(Arn("TestARN"))) shouldBe None
    }

    "return None when DES returns 400" in {
      DesStubs.givenDESReturnsError("TestARN", 400)
      await(connector.getAgencyName(Arn("TestARN"))) shouldBe None
    }

    "return None when DES returns 500 response" in {
      DesStubs.givenDESReturnsError("TestARN", 500)
      await(connector.getAgencyName(Arn("TestARN"))) shouldBe None
    }
  }
}
