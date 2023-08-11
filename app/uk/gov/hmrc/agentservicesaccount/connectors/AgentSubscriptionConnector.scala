/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.{NO_CONTENT, OK}
import play.api.libs.json.Json
import play.utils.UriEncoding
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{ AuthProviderId, SubscriptionJourneyRecord}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.nio.charset.StandardCharsets
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import java.net.URL

@Singleton
class AgentSubscriptionConnector @Inject()(
                                            http: HttpClient,
                                            metrics: Metrics,
                                            appConfig: AppConfig
                                          )(implicit ec: ExecutionContext)
  extends HttpAPIMonitor with Logging {
  def encodePathSegment(pathSegment: String): String =
    UriEncoding.encodePathSegment(pathSegment, StandardCharsets.UTF_8.name)
  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getJourneyById(internalId: AuthProviderId)(implicit hc: HeaderCarrier): Future[Option[SubscriptionJourneyRecord]] =
    monitor(s"ConsumedAPI-Agent-Subscription-getJourneyByPrimaryId-GET") {
      val url =
        new  URL(s"${appConfig.agentSubscriptionBaseUrl}/agent-subscription/subscription/journey/id/${internalId.id}")
      println("getJourneyById + " + url)
      http.GET[HttpResponse](url)
        .map(response =>
          response.status match {
            case OK =>
              Json.parse(response.body).asOpt[SubscriptionJourneyRecord]
            case NO_CONTENT =>
              println("response.body " + response.body)
              None
            case s => logger.error(s"unexpected response $s when getting SubscriptionJourneyRecord"); None
          })
    }
}