/*
 * Copyright 2022 HM Revenue & Customs
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

package it.connectors

import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.OK
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Injecting
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.connectors.PayeSubscriptionConnector
import stubs.AgentServicesAccountStubs._
import uk.gov.hmrc.agentservicesaccount.models.paye.PayeAddress
import uk.gov.hmrc.agentservicesaccount.models.paye.PayeCyaData
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport.hc
import uk.gov.hmrc.http.UpstreamErrorResponse

class PayeSubscriptionConnectorISpec
extends BaseISpec
with Injecting {

  val exampleCyaData = PayeCyaData(
    agentName = "Example Agent Ltd",
    contactName = "Jane Agent",
    telephoneNumber = Some("01632 960 001"),
    emailAddress = Some("jane.agent@example.com"),
    address = PayeAddress(
      line1 = "1 High Street",
      line2 = "Village",
      line3 = Some("County"),
      line4 = None,
      postCode = "AA1 1AA"
    )
  )
  val connector: PayeSubscriptionConnector = inject[PayeSubscriptionConnector]

  ".getCyaData" should {

    "return dummy CYA data (for now)" in {
      val result = connector.getCyaData
      await(result) shouldBe exampleCyaData
    }
  }

  ".submitRequest" should {

    "return nothing when a OK (200) response is returned by agent-services-account" in {
      givenPayeStartSubscriptionResponse(OK)

      val result = connector.submitRequest(exampleCyaData)
      await(result) shouldBe ()
    }

    "throw an UpstreamErrorResponse exception when an unexpected status is returned by agent-services-account" in {
      givenPayeStartSubscriptionResponse(INTERNAL_SERVER_ERROR)

      intercept[UpstreamErrorResponse](await(connector.submitRequest(exampleCyaData)))
    }
  }

}
