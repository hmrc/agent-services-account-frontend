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
import play.api.http.Status.ACCEPTED
import play.api.http.Status.OK
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.UserDetails
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
class AgentUserClientDetailsConnector @Inject() (http: HttpClientV2)(
  implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends Logging {

  private lazy val baseUrl = appConfig.agentUserClientDetailsBaseUrl

  def getTeamMembers(arn: Arn)(
    implicit rh: RequestHeader
  ): Future[Option[Seq[UserDetails]]] = {
    http.get(url"$baseUrl/agent-user-client-details/arn/${arn.value}/team-members").execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED => None
          case OK => response.json.asOpt[Seq[UserDetails]]
          case other =>
            logger.warn(
              s"error getting TeamMemberList for ${arn.value}. Backend response status: $other"
            )
            None
        }
      }
  }

}
