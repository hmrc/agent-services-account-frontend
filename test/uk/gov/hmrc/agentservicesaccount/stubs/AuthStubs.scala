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

trait AuthStubs {

  def givenAuthorisedAsAgentWith(arn: String, isAdmin: Boolean = true) = {
    val credRole = if(isAdmin) "Admin" else "Assistant"
    stubFor(post(urlEqualTo("/auth/authorise"))
      .willReturn(
        aResponse()
          .withStatus(200).withBody(
          s"""{
            |  "internalId": "some-id",
            |  "affinityGroup": "Agent",
            |  "credentialRole": "$credRole",
            |  "allEnrolments": [{
            |    "key": "HMRC-AS-AGENT",
            |    "identifiers": [{ "key": "AgentReferenceNumber", "value": "$arn" }]
            |  }]
            |
            |}""".stripMargin
        )))
  }

  def GivenIsNotLoggedIn() = {
    stubFor(post(urlPathEqualTo(s"/auth/authorise"))
      .willReturn(aResponse()
        .withHeader("WWW-Authenticate", """MDTP detail="BearerTokenExpired"""")
        .withStatus(401)))
    this
  }
}
