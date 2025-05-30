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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.http.Status.FORBIDDEN
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.stubs.AgentServicesAccountStubs.stubASADeleteResponse
import uk.gov.hmrc.agentservicesaccount.stubs.AgentServicesAccountStubs.stubASAGetResponse
import uk.gov.hmrc.agentservicesaccount.stubs.AgentServicesAccountStubs.stubASAGetResponseError
import uk.gov.hmrc.agentservicesaccount.stubs.AgentServicesAccountStubs.stubASAPostResponse
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.Instant
import java.time.temporal.ChronoUnit

class AgentServicesAccountConnectorSpec
extends BaseISpec {

  val exampleArn: Arn = Arn("XARN1234567")
  val exampleModel: PendingChangeRequest = PendingChangeRequest(exampleArn, Instant.now().truncatedTo(ChronoUnit.SECONDS))
  val connector: AgentServicesAccountConnector = app.injector.instanceOf[AgentServicesAccountConnector]

  ".find" should {

    "return a PendingChangeRequest model when one is returned by agent-services-account" in {
      stubASAGetResponse(exampleModel)

      val result = connector.find(exampleArn)
      await(result) shouldBe Some(exampleModel)
    }

    "return None when no details could be found" in {
      stubASAGetResponseError(exampleArn, NOT_FOUND)

      val result = connector.find(exampleArn)
      await(result) shouldBe None
    }

    "return None when an unexpected status is returned" in {
      stubASAGetResponseError(exampleArn, FORBIDDEN)

      val result = connector.find(exampleArn)
      await(result) shouldBe None
    }
  }

  ".insert" should {

    "return nothing when a NO_CONTENT (204) response is returned by agent-services-account" in {
      stubASAPostResponse(NO_CONTENT)

      val result = connector.insert(exampleModel)
      await(result) shouldBe ()
    }

    "throw an UpstreamErrorResponse exception when an unexpected status is returned by agent-services-account" in {
      stubASAPostResponse(FORBIDDEN)

      intercept[UpstreamErrorResponse](await(connector.insert(exampleModel)))
    }
  }

  ".delete" should {

    "return true when a NO_CONTENT (204) response is returned by agent-services-account" in {
      stubASADeleteResponse(exampleArn, NO_CONTENT)

      val result = connector.delete(exampleArn)
      await(result) shouldBe true
    }

    "return false when a NOT_FOUND (404) response is returned by agent-services-account" in {
      stubASADeleteResponse(exampleArn, NOT_FOUND)

      val result = connector.delete(exampleArn)
      await(result) shouldBe false
    }

    "return false when an unexpected status is returned by agent-services-account" in {
      stubASADeleteResponse(exampleArn, FORBIDDEN)

      val result = connector.delete(exampleArn)
      await(result) shouldBe false
    }
  }

}
