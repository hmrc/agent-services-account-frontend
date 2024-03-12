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

package uk.gov.hmrc.agentservicesaccount.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsJourney}

object AgentAssuranceStubs {

  def givenAMLSDetailsForArn(amlsDetails: AmlsDetails, arn: String): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(Json.toJson(amlsDetails).toString())
      ))

  def givenAMLSDetailsNotFoundForArn(arn: String): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(204)
      ))

  def givenAMLSDetailsBadRequestForArn(arn: String): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))

  def givenAMLSDetailsServerErrorForArn(arn: String): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(500)
      ))

  def givenPutAmlsJourneySuccess(amlsJourney: AmlsJourney): StubMapping =
    stubFor(put(urlEqualTo("/agent-assurance/amls-journey"))
      .withRequestBody(equalToJson(Json.toJson(amlsJourney).toString()))
      .willReturn(
        aResponse().withStatus(202)
      ))

  def givenPutAmlsJourneyError(amlsJourney: AmlsJourney): StubMapping =
    stubFor(put(urlEqualTo("/agent-assurance/amls-journey"))
      .withRequestBody(equalToJson(Json.toJson(amlsJourney).toString()))
      .willReturn(
        aResponse().withStatus(500)
      ))

  def givenGetAmlsJourneySuccess(amlsJourney: Option[AmlsJourney], status: Int = 200): StubMapping =
    stubFor(get(urlEqualTo("/agent-assurance/amls-journey"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(amlsJourney.fold("")(journey =>   Json.toJson(journey).toString()))))

  def givenGetAmlsJourneyError: StubMapping =
    stubFor(get(urlEqualTo("/agent-assurance/amls-journey"))
      .willReturn(
        aResponse()
          .withStatus(500)))

  def givenDeleteAmlsJourneySuccess: StubMapping =
    stubFor(delete(urlEqualTo("/agent-assurance/amls-journey"))
      .willReturn(
        aResponse()
          .withStatus(204)))

  def givenDeleteAmlsJourneyError: StubMapping =
    stubFor(delete(urlEqualTo("/agent-assurance/amls-journey"))
      .willReturn(
        aResponse()
          .withStatus(500)))
}
