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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanInitiateRequest
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanInitiateResponse
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpscanInitiateConnector @Inject() (
  appConfig: AppConfig,
  httpClientV2: HttpClientV2
)(implicit ec: ExecutionContext) {

  private val baseUrl: String = appConfig.upscanInitiateBaseUrl

  /** https://github.com/hmrc/upscan-initiate?tab=readme-ov-file#post-upscanv2initiate
    */
  def initiate(
    upscanInitiateRequest: UpscanInitiateRequest
  )(implicit rh: RequestHeader): Future[UpscanInitiateResponse] = {
    val url: URL = url"$baseUrl/upscan/v2/initiate"
    httpClientV2
      .post(url)
      .withBody(Json.toJson(upscanInitiateRequest))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case status if is2xx(status) => response.json.as[UpscanInitiateResponse]
          case status =>
            throw UpstreamErrorResponse(
              s"[UpscanInitiateConnector] unexpected response from upscan-initiate - status: $status, body: ${response.body}",
              status
            )
        }
      }
  }

}
