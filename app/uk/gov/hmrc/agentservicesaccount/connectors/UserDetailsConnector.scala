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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.UserDetails
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, StringContextOps}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserDetailsConnector @Inject()(appConfig: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) extends Logging {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  private lazy val baseUrl = new URL(appConfig.ugsBaseUrl)

  def getUserDetails(userId: String)(implicit hc: HeaderCarrier): Future[Option[UserDetails]] = {
    http.GET[HttpResponse](url"$baseUrl/users-groups-search/users/$userId")
      .map(response =>
        response.status match {
          case OK => Json.parse(response.body).asOpt[UserDetails]
          case other =>
            logger.warn(s"Could not get user details for '$userId'. Status: $other, message: ${response.body}")
            None
        })
  }

}
