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
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetailsResponse

object AgentAssuranceStubs {

  def givenAMLSDetailsForArn(amlsDetailsResponse: AmlsDetailsResponse, arn: String): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(Json.toJson(amlsDetailsResponse).toString())
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

  def givenAmlsStatusForArn(amlsDetailsResponse: AmlsDetailsResponse, arn: Arn): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(Json.toJson(amlsDetailsResponse).toString())
      ))

  def givenAmlsStatusBadRequestForArn(arn: Arn): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(400)
      ))

  def givenAmlsStatusServerErrorForArn(arn: Arn): StubMapping =
    stubFor(get(urlEqualTo(s"/agent-assurance/amls/arn/${arn.value}"))
      .willReturn(
        aResponse()
          .withStatus(500)
      ))
}
