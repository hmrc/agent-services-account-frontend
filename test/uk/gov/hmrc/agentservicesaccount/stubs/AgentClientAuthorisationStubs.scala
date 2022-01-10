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
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, SuspensionDetails}

object AgentClientAuthorisationStubs {

  def givenSuspensionStatus(suspensionDetails: SuspensionDetails): StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/suspension-details"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(suspensionDetails).toString())
    ))

  def givenSuspensionStatusNotFound: StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/suspension-details"))
      .willReturn(
        aResponse()
          .withStatus(204)
      ))

  def givenAgentRecordNotFound: StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/suspension-details"))
      .willReturn(
        aResponse()
          .withStatus(404)
      ))

  def givenAgentDetailsFound(agencyDetails: AgencyDetails): StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/agency-details"))
    .willReturn(
      aResponse()
        .withStatus(200)
        .withBody(Json.toJson(agencyDetails).toString)
    ))

  def givenAgentDetailsNoContent(): StubMapping =
    stubFor(get(urlEqualTo("/agent-client-authorisation/agent/agency-details"))
      .willReturn(
        aResponse()
          .withStatus(204)
      ))
}
