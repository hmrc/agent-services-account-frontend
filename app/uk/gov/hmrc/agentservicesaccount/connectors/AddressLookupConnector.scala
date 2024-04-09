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
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.addresslookup._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddressLookupConnector @Inject()(http: HttpClient)(appConfig: AppConfig, implicit val ec: ExecutionContext) {

  lazy val base: String = appConfig.addressLookupBaseUrl

  // For the details of how address lookup works, read the documentation for the address-lookup-frontend microservice.

  // After the journey is complete, the final address can be retrieved from this endpoint
  def getAddress(id: String)(implicit hc: HeaderCarrier): Future[ConfirmedResponseAddress] =
    http.GET[ConfirmedResponseAddress](s"$base/api/confirmed?id=$id")

  // Initialise the address lookup journey and returns a Url to redirect the user to  to start the journey.
  def init(alfJourneyConfig: JourneyConfigV2)(implicit hc: HeaderCarrier): Future[String] =
    http.POST[JourneyConfigV2, HttpResponse](s"$base/api/init", alfJourneyConfig).map { response: HttpResponse =>
      response.status match {
        case ACCEPTED if response.headers.contains("Location") => response.headers("Location").head
        case x => throw new RuntimeException(s"Address lookup journey initialisation returned status $x")
      }
    }
}
