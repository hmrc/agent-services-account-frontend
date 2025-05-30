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
import play.api.http.Status.{ACCEPTED, OK}
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.UserDetails
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.utils.HttpAPIMonitor
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentUserClientDetailsConnector @Inject()(http: HttpClientV2)(
    implicit val metrics: Metrics,
    appConfig: AppConfig,
    val ec: ExecutionContext)
    extends HttpAPIMonitor
    with Logging {

  private lazy val baseUrl = appConfig.agentUserClientDetailsBaseUrl

  def getTeamMembers(arn: Arn)(
      implicit rh: RequestHeader): Future[Option[Seq[UserDetails]]] = {
    monitor("ConsumedAPI-team-members-GET") {
      http.get(url"$baseUrl/agent-user-client-details/arn/${arn.value}/team-members").execute[HttpResponse]
        .map { response =>
        response.status match {
          case ACCEPTED => None
          case OK       => response.json.asOpt[Seq[UserDetails]]
          case other =>
            logger.warn(
              s"error getting TeamMemberList for ${arn.value}. Backend response status: $other")
            None
        }
      }
    }
  }
}
