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

package support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.IntegrationPatience

object WiremockHelper
extends Eventually
with IntegrationPatience {

  val wiremockPort: Int = 11111
  val wiremockHost: String = "localhost"

  def verifyPost(
    uri: String,
    optBody: Option[String] = None
  ): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest =
      optBody match {
        case Some(body) => uriMapping.withRequestBody(equalTo(body))
        case None => uriMapping
      }
    verify(postRequest)
  }

  def verifyGet(uri: String): Unit = verify(getRequestedFor(urlEqualTo(uri)))
  def verifyGet(
    times: Int,
    uri: String
  ): Unit = verify(times, getRequestedFor(urlEqualTo(uri)))

  def stubGet(
    url: String,
    status: Integer,
    body: String
  ): StubMapping = stubFor(
    get(urlMatching(url))
      .willReturn(
        aResponse().withStatus(status).withBody(body)
      )
  )

  def stubPost(
    url: String,
    status: Integer,
    responseBody: String
  ): StubMapping = stubFor(
    post(urlMatching(url))
      .willReturn(
        aResponse().withStatus(status).withBody(responseBody)
      )
  )

  def stubPut(
    url: String,
    status: Integer,
    responseBody: String
  ): StubMapping = stubFor(
    put(urlMatching(url))
      .willReturn(
        aResponse().withStatus(status).withBody(responseBody)
      )
  )

  def stubPatch(
    url: String,
    status: Integer,
    responseBody: String
  ): StubMapping = stubFor(
    patch(urlMatching(url))
      .willReturn(
        aResponse().withStatus(status).withBody(responseBody)
      )
  )

  def stubDelete(
    url: String,
    status: Integer,
    responseBody: String
  ): StubMapping = stubFor(
    delete(urlMatching(url))
      .willReturn(
        aResponse().withStatus(status).withBody(responseBody)
      )
  )

}

trait WiremockHelper {

  import WiremockHelper._

  lazy val wmConfig: WireMockConfiguration = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer: WireMockServer = new WireMockServer(wmConfig)

  def startWiremock(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock(): Unit = wireMockServer.stop()

  def resetWiremock(): Unit = WireMock.reset()

}
