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

import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status.{ACCEPTED, OK}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, UserDetails}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AgentUserClientDetailsConnectorImpl])
trait AgentUserClientDetailsConnector extends HttpAPIMonitor with Logging {
  val http: HttpClient

  def getTeamMembers(arn: Arn)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Option[Seq[UserDetails]]]

}

@Singleton
class AgentUserClientDetailsConnectorImpl @Inject()(val http: HttpClient)(
    implicit metrics: Metrics,
    appConfig: AppConfig)
    extends AgentUserClientDetailsConnector
    with HttpAPIMonitor
    with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private lazy val baseUrl = appConfig.agentUserClientDetailsBaseUrl

  override def getTeamMembers(arn: Arn)(
      implicit hc: HeaderCarrier,
      ec: ExecutionContext): Future[Option[Seq[UserDetails]]] = {
    val url =
      s"$baseUrl/agent-user-client-details/arn/${arn.value}/team-members"
    monitor("ConsumedAPI-team-members-GET") {
      http.GET[HttpResponse](url).map { response =>
        response.status match {
          case ACCEPTED => None
          case OK       => response.json.asOpt[Seq[UserDetails]]
          case e =>
            throw UpstreamErrorResponse(s"error getTeamMemberList for ${arn.value}",
                                        e)
        }
      }
    }

  }

}
