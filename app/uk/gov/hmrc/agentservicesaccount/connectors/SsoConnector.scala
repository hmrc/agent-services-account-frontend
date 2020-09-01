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

import java.net.URL

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoConnector @Inject()(httpClient: HttpClient, metrics: Metrics)(implicit val appConfig: AppConfig) extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def validateExternalDomain(domain: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    val url = new URL(s"${appConfig.ssoBaseUrl}/sso/validate/domain/$domain")
    httpClient.GET(url.toString)
      .map(_ => true)
      .recover {
        case e: BadRequestException => false
        case e: Exception =>
          Logger.error(s"Unable to validate domain $domain", e)
          false
      }
  }
}
