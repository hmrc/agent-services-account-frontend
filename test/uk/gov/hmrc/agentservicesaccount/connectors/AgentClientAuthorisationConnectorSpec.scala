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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientAuthorisationConnectorSpec extends BaseISpec {

  private lazy val connector = app.injector.instanceOf[AgentClientAuthorisationConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "getAgentRecord" should {
    "return the agent record for a given agent" in {

      givenAgentRecordFound(agentRecord)

      await(connector.getAgentRecord()) shouldBe agentRecord
    }

    "throw exception when 204 response" in {
      givenAgentDetailsErrorResponse(204)
      intercept[UpstreamErrorResponse]{
        await(connector.getAgentRecord())
      }
    }
  }

}
