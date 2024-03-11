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
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.{AmlsDetails, AmlsJourney}
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentAssuranceConnector @Inject()(httpV2: HttpClientV2)(implicit val metrics: Metrics, appConfig: AppConfig)
  extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val baseUrl = appConfig.agentAssuranceBaseUrl

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def getAMLSDetails(arn: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[AmlsDetails] = {
    httpV2.get(new URL(s"$baseUrl/agent-assurance/amls/arn/$arn")).execute[HttpResponse].map { response =>
      response.status match {
        case OK => Json.parse(response.body).as[AmlsDetails]
        case NO_CONTENT => throw new Exception(s"Error $NO_CONTENT no amls details found") //TODO update when designs are done
        case BAD_REQUEST => throw UpstreamErrorResponse(s"Error $BAD_REQUEST invalid ARN when trying to get amls details", BAD_REQUEST)
        case e => throw UpstreamErrorResponse(s"Error $e unable to get amls details", e)
      }
    }
  }

  val amlsJourneyUrl = new URL(s"$baseUrl/agent-assurance/amls-journey")
  def putAmlsJourney(amlsJourney: AmlsJourney)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] = {
    httpV2
      .put(amlsJourneyUrl)
      .withBody(Json.toJson(amlsJourney))
      .execute[HttpResponse].map(response => response.status match {
        case ACCEPTED => ()
        case e    => throw UpstreamErrorResponse(s"Error $e unable to save amls journey", e)
      })
  }

  def getAmlsJourney(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[AmlsJourney]] =
    httpV2
      .get(amlsJourneyUrl)
      .execute[HttpResponse]
      .map( response => response.status match {
        case OK           => response.json.asOpt[AmlsJourney]
        case NO_CONTENT   => None
        case e            => throw UpstreamErrorResponse(s"Error $e unable to get amls journey", e)
      })

  def deleteAmlsJourney(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Unit] =
    httpV2
      .delete(amlsJourneyUrl)
      .execute[HttpResponse]
      .map(response => response.status match {
        case NO_CONTENT     => ()
        case e              => throw UpstreamErrorResponse(s"Error $e unable to delete amls journey", e)
    })
}
