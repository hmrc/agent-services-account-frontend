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

package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK

object UpscanStubs {
  def givenUpscanInitiateSucceeds(reference: String = "test-ref"): StubMapping = {
    stubFor(
      post(urlEqualTo("/upscan/v2/initiate"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(s"""
              {
                "reference": "$reference",
                "uploadRequest": {
                  "href": "https://bucket-url",
                  "fields": {
                    "key": "$reference",
                    "acl": "private",
                    "x-amz-algorithm": "AWS4-HMAC-SHA256",
                    "x-amz-credential": "CREDENTIAL",
                    "x-amz-date": "20220401T000000Z",
                    "x-amz-meta-callback-url": "https://callback.url",
                    "policy": "POLICY",
                    "x-amz-signature": "SIGNATURE"
                  }
                }
              }
            """.stripMargin)
        )
    )
  }
}
