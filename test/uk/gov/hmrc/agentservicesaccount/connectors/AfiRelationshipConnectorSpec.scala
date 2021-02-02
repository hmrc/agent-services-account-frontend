/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.test.Injecting
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.stubs.AgentFiRelationshipStubs.{givenArnIsAllowlistedForIrv, givenArnIsNotAllowlistedForIrv}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class AfiRelationshipConnectorSpec extends BaseISpec with Injecting {

  val connector = inject[AfiRelationshipConnector]

  "checkIrvAllowed" should {
    "return true when agent-fi-relationship returns 204 No Content" in {
      val arn = Arn("TARN0000001")
      givenArnIsAllowlistedForIrv(arn)
      await(connector.checkIrvAllowed(arn)(HeaderCarrier())) shouldBe true
    }

    "return false when agent-fi-relationship returns 404 Not Found" in {
      val arn = Arn("TARN0000001")
      givenArnIsNotAllowlistedForIrv(arn)
      await(connector.checkIrvAllowed(arn)(HeaderCarrier())) shouldBe false
    }
  }
}
