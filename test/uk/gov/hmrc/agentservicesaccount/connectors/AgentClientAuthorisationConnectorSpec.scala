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

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.{SuspensionDetails, SuspensionDetailsNotFound}

import scala.concurrent.ExecutionContext.Implicits.global

class AgentClientAuthorisationConnectorSpec extends BaseISpec {

  private lazy val connector = app.injector.instanceOf[AgentClientAuthorisationConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val arn: Arn = Arn("TARN0000001")

  "getSuspensionDetails" should {
    "return the suspension details for a given agent" in {
      val suspensionDetails = SuspensionDetails(suspensionStatus = true, Some(Set("ITSA")))
      givenSuspensionStatus(suspensionDetails)
      await(connector.getSuspensionDetails()) shouldBe suspensionDetails
    }

    "return false suspension details when no status is found" in {
      givenSuspensionStatusNotFound
      await(connector.getSuspensionDetails()) shouldBe SuspensionDetails(suspensionStatus = false, None)
    }

    "return not found error response when no agent record is found" in {
      givenAgentRecordNotFound
      intercept[SuspensionDetailsNotFound] {
        await(connector.getSuspensionDetails())
      }.getMessage shouldBe "No record found for this agent"
    }
  }

  "getAgencyDetails" should {
    "return agency details for a given agent" in {

      val agentDetails = AgencyDetails(
          Some("My Agency"),
          Some("abc@abc.com"),
          Some("07345678901"),
          Some(BusinessAddress(
            "25 Any Street",
            Some("Central Grange"),
            Some("Telford"),
            None,
            Some("TF4 3TR"),
            "GB"))
        )

      givenAgentDetailsFound(agentDetails)

      await(connector.getAgencyDetails()) shouldBe Some(agentDetails)
    }

    "return None when response is 204" in {
      givenAgentDetailsNoContent()
      await(connector.getAgencyDetails()) shouldBe None
    }
  }

}
