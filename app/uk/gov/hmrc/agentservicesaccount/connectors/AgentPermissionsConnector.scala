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
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, OptinStatus}
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.AccessGroupSummaries
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


@Singleton
class AgentPermissionsConnector @Inject()(http: HttpClient)(implicit val metrics: Metrics, appConfig: AppConfig) extends HttpAPIMonitor with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val baseUrl = appConfig.agentPermissionsBaseUrl

  def getOptinStatus(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptinStatus]] = {
    val url = s"$baseUrl/agent-permissions/arn/${arn.value}/optin-status"
    monitor("ConsumedAPI-GetOptinStatus-GET"){
      http.GET[HttpResponse](url).map{ response =>
          response.status match {
            case OK => response.json.asOpt[OptinStatus]
            case e => logger.warn(s"getOptinStatus returned status $e ${response.body}"); None
          }
      }
    }
  }

  def getGroupsSummaries(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AccessGroupSummaries]] = {

    def buildHeaders: Seq[(String, String)] = {
      hc.otherHeaders.toMap.get("Cookie").map(_.split(";")).map { cookieParts =>
        cookieParts.foldLeft(Seq.empty[(String, String)]) { (acc, cookiePart) =>
          if (cookiePart.trim.startsWith("PLAY_LANG")) {
            Try {
              val pairKeyValue = cookiePart.split("=")
              pairKeyValue(0).trim -> pairKeyValue(1).trim
            } match {
              case Success(pair) => acc :+ pair
              case Failure(ex) =>
                logger.error(s"Unable to obtain lang header: ${ex.getMessage} from $cookiePart")
                acc
            }
          } else acc
        }
      }.toSeq.flatten
    }

    val url = s"$baseUrl/agent-permissions/arn/${arn.value}/groups"
    monitor("ConsumedAPI-GetGroupsSummaries-GET"){
      http.GET[HttpResponse](url, headers = buildHeaders).map{ response =>
        response.status match {
          case OK => response.json.asOpt[AccessGroupSummaries]
          case e => logger.warn(s"GetGroupsSummaries returned status $e ${response.body}"); None
        }
      }
    }
  }

  def syncEacd(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val url = s"$baseUrl/agent-permissions/arn/${arn.value}/sync"

    http.PATCH[SyncEacd, HttpResponse](url, SyncEacd("sync")).map{ response =>
      response.status match {
        case OK =>
          logger.debug(s"EACD sync called for $arn")
        case other =>
          logger.warn(s"syncEacd returned status $other ${response.body}");
      }
    } transformWith {
      case Success(_) =>
        Future.successful(logger.debug("EACD sync called successfully"))
      case Failure(ex) =>
        Future.successful(logger.error(s"EACD sync call failed: ${ex.getMessage}"))
    }
  }

  case class SyncEacd(msg: String)
  object SyncEacd {
    implicit val format: OFormat[SyncEacd] = Json.format[SyncEacd]
  }
}
