/*
 * Copyright 2026 HM Revenue & Customs
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

object ObjectStoreStubs {

  def givenObjectStoreUploadFromUrlSucceeds(): StubMapping = {
    stubFor(
      post(urlEqualTo("/object-store/ops/upload-from-url"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                |{
                |  "location": "agent-services-account-frontend/test-ref",
                |  "contentLength": 12345,
                |  "contentMD5": "a3c2f1e38701bd2c7b54ebd7b1cd0dbc",
                |  "lastModified": "2024-01-01T12:00:00Z"
                |}
              """.stripMargin
            )
        )
    )
  }
}
