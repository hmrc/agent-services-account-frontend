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

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.models.{AuthProviderId, BusinessDetails, SubscriptionJourneyRecord}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration._

class AgentSubscriptionConnectorSpec extends BaseISpec {

  private lazy val connector = app.injector.instanceOf[AgentSubscriptionConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()


  "AgentSubscriptionConnector" should {
    "return a SubscriptionJourneyRecord when response is OK" in {
      val internalId = AuthProviderId("12345")
      val responseBody = Json.toJson(SubscriptionJourneyRecord(internalId,BusinessDetails(Utr("1234567890")))).toString

      stubFor(
        get(urlEqualTo(s"/agent-subscription/subscription/journey/id/${internalId.id}"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseBody)
          )
      )


      val result = connector.getJourneyById(internalId)(HeaderCarrier())

      val subscriptionJourneyOpt = Await.result(result, 5.seconds)
      subscriptionJourneyOpt shouldBe Some(SubscriptionJourneyRecord(internalId,BusinessDetails(Utr("1234567890"))))
    }
  }
}
