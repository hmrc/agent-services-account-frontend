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

import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, UpdateAmlsJourney}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentAssuranceStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class AgentAssuranceConnectorSpec extends BaseISpec {

  private lazy val connector = app.injector.instanceOf[AgentAssuranceConnector]

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val arn: String = "TARN0000001"

  private val ukAMLSDetails = AmlsDetails(
    "HMRC",
    Some("123456789"),
    Some("safeId"),
    Some("bprSafeId"),
    Some(LocalDate.of(2022, 12, 25)),
    Some(LocalDate.of(2023, 12, 25))
  )

  private val amlsJourney = UpdateAmlsJourney(
    status = "UKAmls",
    newAmlsBody = Some("UK AMLS"),
    newMembershipNumber = Some("AMLS123"),
    newExpirationDate = Some(LocalDate.parse("2024-10-10"))
  )


  private val overseasAMLSDetails = AmlsDetails("notHMRC")

  "getAMLSDetails" should {
    "return UK AMLS details" in {
      givenAMLSDetailsForArn(ukAMLSDetails, arn)

      val result = connector.getAMLSDetails(arn)

      await(result) shouldBe ukAMLSDetails
    }
    "return Overseas AMLS details" in {
      givenAMLSDetailsForArn(overseasAMLSDetails, arn)

      val result = connector.getAMLSDetails(arn)

      await(result) shouldBe overseasAMLSDetails
    }
    "handle 204 No Content" in {
      givenAMLSDetailsNotFoundForArn(arn)

      intercept[Exception] {
        await(connector.getAMLSDetails(arn))
      }.getMessage shouldBe s"Error $NO_CONTENT no amls details found"
    }
    "handle 400 Bad Request" in {
      givenAMLSDetailsBadRequestForArn(arn)

      intercept[UpstreamErrorResponse] {
        await(connector.getAMLSDetails(arn))
      }.getMessage shouldBe "Error 400 invalid ARN when trying to get amls details"
    }
    "handle 500 Internal Server Error" in {
      givenAMLSDetailsServerErrorForArn(arn)

      intercept[UpstreamErrorResponse] {
        await(connector.getAMLSDetails(arn))
      }.getMessage shouldBe "Error 500 unable to get amls details"
    }
  }
}
