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

package uk.gov.hmrc.agentservicesaccount.endpoints

import play.api.test.Helpers._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import play.api.libs.ws.{WSClient, WSRequest}
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.stubs.AgentAssuranceStubs.{givenAMLSDetailsForArn, givenAMLSDetailsServerErrorForArn}
import uk.gov.hmrc.agentservicesaccount.stubs.CookieHelper

import java.time.LocalDate

class AMLSDetailsEndpoint extends BaseISpec with GuiceOneServerPerSuite with CookieHelper {

  /**
   * These could move to a component interface base spec, but we don't need the server
   * for other integration tests.
   * CookieHelper can move there as well
   */
  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  def makeRequest(path: String): WSRequest = {
    ws.url(s"http://localhost:$port/agent-services-account$path")
      .withFollowRedirects(false)
      .withCookies(mockSessionCookie)
  }

  override implicit lazy val app: Application = appBuilder().build()

  private val arn: String = "TARN0000001"
  private val ukAMLSDetails = AmlsDetails(
    "HMRC",
    Some("123456789"),
    Some("safeId"),
    Some("bprSafeId"),
    Some(LocalDate.of(2022, 1, 25)),
    Some(LocalDate.of(2023, 12, 7))
  )

  "View AMLS Supervision Details endpoint" should {
    "return successfully when everything works" in {
      givenAuthorisedAsAgentWith(arn)
      givenAMLSDetailsForArn(ukAMLSDetails, arn)

      val result = await(makeRequest("/manage-account/money-laundering-supervision").get())

      result.status shouldBe 200
      result.body should include("Money laundering supervision details")
    }
    "return an error if the call to get AMLS details fails" in {
      givenAuthorisedAsAgentWith(arn)
      givenAMLSDetailsServerErrorForArn(arn)

      val result = await(makeRequest("/manage-account/money-laundering-supervision").get())

      result.status shouldBe 500
      result.body should include("Sorry, we’re experiencing technical difficulties")
    }
  }

}
