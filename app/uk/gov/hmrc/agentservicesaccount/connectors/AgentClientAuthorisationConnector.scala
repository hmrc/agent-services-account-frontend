/*
 * Copyright 2020 HM Revenue & Customs
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
import com.kenshoo.play.metrics.Metrics
import javax.inject.Inject
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{SuspensionDetails, SuspensionDetailsNotFound}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AgentClientAuthorisationConnector @Inject()(http: HttpClient)(implicit val metrics: Metrics, appConfig: AppConfig)
  extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getSuspensionDetails()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionDetails] =
    monitor("ConsumerAPI-Get-AgencySuspensionDetails-GET") {
      http
        .GET[HttpResponse](s"${appConfig.acaBaseUrl}/agent-client-authorisation/agent/suspension-details")
        .map(response =>
          response.status match {
            case OK => Json.parse(response.body).as[SuspensionDetails]
            case NO_CONTENT => SuspensionDetails(suspensionStatus = false, None)
            case NOT_FOUND => throw SuspensionDetailsNotFound("No record found for this agent")
          })
    }
}
