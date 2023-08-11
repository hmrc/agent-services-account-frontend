/*
 * Copyright 2023 HM Revenue & Customs
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


import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.{AuthProviderId, BusinessDetails, SubscriptionJourneyRecord}

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class AgentSubscriptionConnectorSpec  extends BaseISpec {

  // Mocked dependencies

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private lazy val connector = app.injector.instanceOf[AgentSubscriptionConnector]
  // Test data
  val authProviderId = AuthProviderId("someId")
  val journeyRecord = SubscriptionJourneyRecord(authProviderId, BusinessDetails(Utr("1234567890")))

  // Example HeaderCarrier
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "AgentSubscriptionConnector" should {
    "retrieve a journey record by ID" in {
//      val encodedId: String = connector.encodePathSegment(authProviderId.id)
      val url = "/agent-subscription/subscription/journey/id/someId"
      val responseJson = Json.toJson(journeyRecord).toString()
      println(s"Response JSON: $responseJson")
      println("test + " + url)
      stubFor(get(urlEqualTo("/agent-subscription/subscription/journey/id/someId"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(responseJson)
        ))


      val result = connector.getJourneyById(authProviderId)

      println(journeyRecord)
      result.futureValue shouldEqual Some(journeyRecord)
    }
  }
}
