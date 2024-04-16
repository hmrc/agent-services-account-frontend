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

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsDetailsResponse, AmlsStatuses}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentAssuranceStubs.{givenAMLSDetailsForArn, givenAMLSDetailsServerErrorForArn}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs.givenAgentRecordFound
import uk.gov.hmrc.agentservicesaccount.stubs.CookieHelper
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec

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


  private val ukAMLSDetails = AmlsDetails(
    "HMRC",
    Some("123456789"),
    Some("safeId"),
    Some("bprSafeId"),
    Some(LocalDate.of(2022, 1, 25)),
    Some(LocalDate.of(2023, 12, 7))
  )
  private val ukAMLSDetailsResponse = AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK,Some(ukAMLSDetails))

  "View AMLS Supervision Details endpoint" should {
    "return successfully when everything works" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(ukAMLSDetailsResponse, arn.value)

      val result = await(makeRequest("/manage-account/money-laundering-supervision").get())

      result.status shouldBe 200
      result.body should include("Money laundering supervision details")
    }
    "return an error if the call to get AMLS details fails" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsServerErrorForArn(arn.value)

      val result = await(makeRequest("/manage-account/money-laundering-supervision").get())

      result.status shouldBe 500
      result.body should include("Sorry, we’re experiencing technical difficulties")
    }
  }

}
