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

import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agents.accessgroups.GroupSummary
import uk.gov.hmrc.agents.accessgroups.optin.OptinStatus
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.AccessGroupSummaries
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

@Singleton
class AgentPermissionsConnector @Inject() (http: HttpClientV2)(implicit
  val metrics: Metrics,
  appConfig: AppConfig,
  val ec: ExecutionContext
)
extends Logging {

  private val baseUrl = appConfig.agentPermissionsBaseUrl

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def getOptinStatus(arn: Arn)(implicit rh: RequestHeader): Future[Option[OptinStatus]] = {
    http
      .get(new URL(s"$baseUrl/agent-permissions/arn/${arn.value}/optin-status"))
      .transform(ws => ws.withRequestTimeout(2.minutes))
      .execute[Option[OptinStatus]]
      .recover { case e =>
        logger.warn(s"getOptinStatus error: ${e.getMessage}")
        Option.empty[OptinStatus]
      }
  }

  def getGroupsSummaries(arn: Arn)(implicit rh: RequestHeader): Future[Option[AccessGroupSummaries]] = {

    def buildHeaders: Seq[(String, String)] = {
      rh.headers.get("Cookie").map(_.split(";")).map { cookieParts =>
        cookieParts.foldLeft(Seq.empty[(String, String)]) {
          (
            acc,
            cookiePart
          ) =>
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
            }
            else
              acc
        }
      }.toSeq.flatten
    }

    http.get(url"$baseUrl/agent-permissions/arn/${arn.value}/groups").setHeader(buildHeaders: _*)
      .execute[HttpResponse].map { response =>
        response.status match {
          case OK => response.json.asOpt[AccessGroupSummaries]
          case e => logger.warn(s"GetGroupsSummaries returned status $e ${response.body}"); None
        }
      }
  }

  def getGroupsForTeamMember(
    arn: Arn,
    userId: String
  )(implicit rh: RequestHeader): Future[Option[Seq[GroupSummary]]] = {
    http.get(url"$baseUrl/agent-permissions/arn/${arn.value}/team-member/$userId/groups").execute[Option[Seq[GroupSummary]]]

  }

  def isOptedIn(
    arn: Arn
  )(implicit rh: RequestHeader): Future[Boolean] = http.get(url"$baseUrl/agent-permissions/arn/${arn.value}/optin-record-exists").execute[HttpResponse].map {
    response: HttpResponse =>
      response.status match {
        case NO_CONTENT => true
        case NOT_FOUND => false
        case other =>
          logger.warn(s"error getting opt in record for '$arn'. Backend response status: $other")
          false
      }
  }

  def syncEacd(
    arn: Arn,
    fullSync: Boolean
  )(implicit rh: RequestHeader): Future[Unit] = {
    http.post(url"$baseUrl/agent-permissions/arn/${arn.value}/sync?fullSync=$fullSync")
      .withBody(Json.toJson(SyncEacd("sync"))).execute[Unit]
  }

  def isArnAllowed(implicit rh: RequestHeader): Future[Boolean] = {
    http.get(url"$baseUrl/agent-permissions/arn-allowed").execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => true
          case other =>
            logger.warn(s"ArnAllowed call returned status $other")
            false
        }
      }

  }

  def isShownPrivateBetaInvite(implicit rh: RequestHeader): Future[Boolean] = {
    http.get(url"$baseUrl/agent-permissions/private-beta-invite").execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.info(s"in private beta or has dismissed private beta invite")
            false
          case _ => true
        }
      }

  }

  def declinePrivateBetaInvite()(implicit rh: RequestHeader): Future[Done] = {
    http.post(url"$baseUrl/agent-permissions/private-beta-invite/decline").execute[HttpResponse]
      .map { response =>
        response.status match {
          case CREATED => Done
          case CONFLICT =>
            logger.info(s"Tried to decline when already dismissed")
            Done
          case e =>
            throw UpstreamErrorResponse(
              s"error sending dismiss request for private beta invite",
              e
            )
        }
      }

  }

  case class SyncEacd(msg: String)

  object SyncEacd {
    implicit val format: OFormat[SyncEacd] = Json.format[SyncEacd]
  }

}
