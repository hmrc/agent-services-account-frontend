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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetailsResponse
import uk.gov.hmrc.agentservicesaccount.models.AmlsRequest

object AgentAssuranceStubs {

  def givenAMLSDetailsForArn(
    amlsDetailsResponse: AmlsDetailsResponse,
    arn: String
  ): StubMapping = stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(amlsDetailsResponse).toString())
    ))

  def givenPostAMLSDetails(
    arn: String,
    amlsRequest: AmlsRequest
  ): StubMapping = stubFor(post(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
    .withRequestBody(equalToJson(Json.toJson(amlsRequest).toString())).willReturn(aResponse().withStatus(201)))

  def givenAMLSDetailsBadRequestForArn(arn: String): StubMapping = stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
    .willReturn(
      aResponse()
        .withStatus(400)
    ))

  def givenAMLSDetailsServerErrorForArn(arn: String): StubMapping = stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
    .willReturn(
      aResponse()
        .withStatus(500)
    ))

  def givenAgentRecordFound(agentRecord: AgentDetailsDesResponse): StubMapping = stubFor(get(urlEqualTo("/agent-assurance/agent-record-with-checks"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(agentRecord).toString)
    ))

  def givenAgentDetailsErrorResponse(status: Int): StubMapping = stubFor(get(urlEqualTo("/agent-assurance/agent-record-with-checks"))
    .willReturn(
      aResponse()
        .withStatus(status)
    ))

  def givenPostDesignatoryDetails(arn: String): StubMapping = stubFor(
    post(urlEqualTo(s"/agent-assurance/agent/agency-details/arn/$arn"))
      .willReturn(aResponse().withStatus(201))
  )

}
