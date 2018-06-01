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

package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL

import com.kenshoo.play.metrics.Metrics
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentservicesaccount.stubs.SsoStubs
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

class SsoConnectorSpec extends UnitSpec with GuiceOneAppPerTest with WireMockSupport {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.agent-services-account.port" -> wireMockPort,
        "microservice.services.sso.port" -> wireMockPort,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false,
        "passcodeAuthentication.enabled" -> true
      )

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val connector = new SsoConnector(app.injector.instanceOf[HttpGet], new URL(s"http://localhost:$wireMockPort"), app.injector.instanceOf[Metrics])
  private implicit val hc = HeaderCarrier()

  "SsoConnector" should {
    "return 204" in {
      SsoStubs.givenDomainIsWhitelisted("foo.com")
      val result = await(connector.validateExternalDomain("foo.com"))
      result shouldBe true
    }

    "return 400" in {
      SsoStubs.givenDomainIsNotWhitelisted("Imnotvalid.com")
      val result = await(connector.validateExternalDomain("Imnotvalid.com"))
      result shouldBe false
    }

    "return false when request fails" in {
      SsoStubs.givenDomainCheckFails("foo.com")
      val result = await(connector.validateExternalDomain("foo.com"))
      result shouldBe false
    }
  }
}
