/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.test.Helpers._
import play.api.test.Injecting
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetailsResponse
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses
import stubs.AgentAssuranceStubs._
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class AgentAssuranceConnectorISpec
extends BaseISpec
with Injecting {

  private lazy val connector = inject[AgentAssuranceConnector]

  private val ukAMLSDetails = AmlsDetails(
    "HMRC",
    Some("123456789"),
    Some("safeId"),
    Some("bprSafeId"),
    Some(LocalDate.of(2022, 12, 25)),
    Some(LocalDate.of(2023, 12, 25))
  )

  private val ukAMLSDetailsResponse = AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, Some(ukAMLSDetails))

  private val overseasAMLSDetails = AmlsDetails("notHMRC")
  private val overseasAMLSDetailsResponse = AmlsDetailsResponse(AmlsStatuses.ValidAmlsNonUK, Some(overseasAMLSDetails))

  "getAMLSDetails" should {
    "return UK AMLS details" in {
      givenAMLSDetailsForArn(ukAMLSDetailsResponse, arn.value)

      val result = connector.getAMLSDetails(arn.value)

      await(result) shouldBe ukAMLSDetails
    }
    "return Overseas AMLS details" in {
      givenAMLSDetailsForArn(overseasAMLSDetailsResponse, arn.value)

      val result = connector.getAMLSDetails(arn.value)

      await(result) shouldBe overseasAMLSDetails
    }
    "handle 400 Bad Request" in {
      givenAMLSDetailsBadRequestForArn(arn.value)

      intercept[UpstreamErrorResponse] {
        await(connector.getAMLSDetails(arn.value))
      }.getMessage shouldBe "Error 400 invalid ARN when trying to get amls details"
    }
    "handle 500 Internal Server Error" in {
      givenAMLSDetailsServerErrorForArn(arn.value)

      intercept[UpstreamErrorResponse] {
        await(connector.getAMLSDetails(arn.value))
      }.getMessage shouldBe "Error 500 unable to get amls details"
    }
  }

  "getAmlsStatus" should {
    "return UK AMLS Status" in {
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsDetailsUK, None), arn.value)

      val result = connector.getAmlsStatus(arn)

      await(result) shouldBe AmlsStatuses.ValidAmlsDetailsUK
    }
    "return Overseas AMLS details" in {
      givenAMLSDetailsForArn(AmlsDetailsResponse(AmlsStatuses.ValidAmlsNonUK, None), arn.value)

      val result = connector.getAmlsStatus(arn)

      await(result) shouldBe AmlsStatuses.ValidAmlsNonUK
    }
    "handle 400 Bad Request" in {
      givenAMLSDetailsBadRequestForArn(arn.value)

      intercept[UpstreamErrorResponse] {
        await(connector.getAmlsStatus(arn))
      }.getMessage shouldBe "Error 400 invalid ARN when trying to get amls details"
    }
    "handle 500 Internal Server Error" in {
      givenAMLSDetailsServerErrorForArn(arn.value)

      intercept[UpstreamErrorResponse] {
        await(connector.getAmlsStatus(arn))
      }.getMessage shouldBe "Error 500 unable to get amls details"
    }
  }

  "getAgentRecord" should {
    "return the agent record for a given agent" in {

      givenAgentRecordFound(agentRecord)

      await(connector.getAgentRecord) shouldBe agentRecord
    }

    "throw exception when 204 response" in {
      givenAgentDetailsErrorResponse(204)
      intercept[UpstreamErrorResponse] {
        await(connector.getAgentRecord)
      }
    }
  }

}
