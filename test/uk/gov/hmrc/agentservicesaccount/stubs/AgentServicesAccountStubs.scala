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

package uk.gov.hmrc.agentservicesaccount.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest.connectorWrites

object AgentServicesAccountStubs {

  def stubASAGetResponse(model: PendingChangeRequest): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-services-account/change-of-details-request/${model.arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(Json.toJson(model)(connectorWrites).toString())
      ))

  def stubASAGetResponseError(arn: Arn, status: Int): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-services-account/change-of-details-request/${arn.value}"))
      .willReturn(
        aResponse().withStatus(status)
      ))

  def stubASAPostResponse(status: Int): StubMapping =
    stubFor(post(urlEqualTo(s"/agent-services-account/change-of-details-request"))
      .willReturn(
        aResponse().withStatus(status)
      ))

  def stubASADeleteResponse(arn: Arn, status: Int): StubMapping =
    stubFor(delete(urlEqualTo(s"/agent-services-account/change-of-details-request/${arn.value}"))
      .willReturn(
        aResponse().withStatus(status)
      ))
}
