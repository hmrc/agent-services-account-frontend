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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.CREATED
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.paye._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class PayeSubscriptionConnector @Inject() (httpV2: HttpClientV2)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
) {

  private lazy val url = s"${appConfig.agentServicesAccountBaseUrl}/agent-services-account"

  def getCyaData: Future[PayeCyaData] = Future.successful(
    PayeCyaData(
      agentName = "Example Agent Ltd",
      contactName = "Jane Agent",
      telephoneNumber = Some("01632 960 001"),
      emailAddress = Some("jane.agent@example.com"),
      address = PayeAddress(
        line1 = "1 High Street",
        line2 = "Village",
        line3 = Some("County"),
        line4 = None,
        postCode = "AA1 1AA"
      )
    )
  )

  def submitRequest(cyaData: PayeCyaData)(implicit hc: HeaderCarrier): Future[Unit] = {
    httpV2
      .post(url"$url/legacy-subscription-request/paye").withBody(Json.toJson(cyaData)).execute[HttpResponse]
      .map {
        response =>
          response.status match {
            case OK => ()
            case e => throw UpstreamErrorResponse(s"[PayeSubscriptionConnector][submitRequest] Error $e unable to post paye legacy subscription request", e)
          }
      }
  }

}
