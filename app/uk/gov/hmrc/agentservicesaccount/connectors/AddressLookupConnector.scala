/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AddressLookupConnector @Inject() (http: HttpClientV2)(
  appConfig: AppConfig,
  implicit val ec: ExecutionContext
) {

  lazy val base: String = appConfig.addressLookupBaseUrl

  // For the details of how address lookup works, read the documentation for the address-lookup-frontend microservice.

  // After the journey is complete, the final address can be retrieved from this endpoint
  def getAddress(
    id: String
  )(implicit rh: RequestHeader): Future[ConfirmedResponseAddress] = http.get(url"$base/api/confirmed?id=$id").execute[ConfirmedResponseAddress]

  // Initialise the address lookup journey and returns a Url to redirect the user to  to start the journey.
  def init(
    alfJourneyConfig: JourneyConfigV2
  )(implicit rh: RequestHeader): Future[String] = http.post(url"$base/api/init").withBody(Json.toJson(alfJourneyConfig)).execute[HttpResponse].map { response =>
    response.status match {
      case ACCEPTED if response.headers.contains("Location") => response.headers("Location").head
      case x => throw new RuntimeException(s"Address lookup journey initialisation returned status $x")
    }
  }

}
