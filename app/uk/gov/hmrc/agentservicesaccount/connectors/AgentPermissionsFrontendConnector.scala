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
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status.OK
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentPermissionsFrontendConnector @Inject()(http: HttpClient)
                                                 (implicit val metrics: Metrics, appConfig: AppConfig)
  extends HttpAPIMonitor with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def isArnAllowed(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    monitor("ConsumedAPI-GranPermsArnAllowed-GET") {
      http.GET[HttpResponse](appConfig.agentPermissionsArnAllowedUrl).map{ response =>
        response.status match {
          case OK => true
          case other =>
            logger.warn(s"ArnAllowed call returned status $other")
            false
        }
      }
    }
  }

}
