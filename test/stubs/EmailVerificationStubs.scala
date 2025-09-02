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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerifyEmailResponse

object EmailVerificationStubs {

  def givenCheckEmailSuccess(
    credId: String,
    verificationStatusResponse: VerificationStatusResponse
  ): StubMapping = stubFor(get(urlEqualTo(s"/email-verification/verification-status/$credId"))
    .willReturn(aResponse().withBody(Json.toJson(verificationStatusResponse).toString()).withStatus(200)))

  def givenCheckEmailNotOK(
    credId: String,
    status: Int
  ): StubMapping = stubFor(get(urlEqualTo(s"/email-verification/verification-status/$credId"))
    .willReturn(aResponse().withStatus(status)))

  def givenVerifyEmailSuccess(redirectUri: String): StubMapping = stubFor(post(urlEqualTo("/email-verification/verify-email"))
    .willReturn(aResponse().withBody(Json.toJson(VerifyEmailResponse(redirectUri)).toString()).withStatus(201)))

  def givenVerifyEmailError(status: Int): StubMapping = stubFor(post(urlEqualTo("/email-verification/verify-email"))
    .willReturn(aResponse().withStatus(status)))

}
