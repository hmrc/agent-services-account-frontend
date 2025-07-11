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

import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.userDetails.UserDetails
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UserDetailsConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics
)(implicit
  val appConfig: AppConfig,
  val ec: ExecutionContext
)
extends Logging {

  def getUserDetails(credId: String)(implicit rh: RequestHeader): Future[Option[UserDetails]] = {
    http.get(url"${appConfig.userDetailsBaseUrl}/user-details/id/$credId").execute[Option[UserDetails]].recover {
      case e =>
        logger.warn(s"user details status error for $credId; message: ${e.getMessage}")
        None
    }
  }

}
