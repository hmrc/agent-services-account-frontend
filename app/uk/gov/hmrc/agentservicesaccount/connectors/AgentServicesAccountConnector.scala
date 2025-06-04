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

import play.api.Logging
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest.connectorReads
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest.connectorWrites
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentServicesAccountConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
extends Logging {

  val url: String = s"${appConfig.agentServicesAccountBaseUrl}/agent-services-account/change-of-details-request"

  def find(arn: Arn)(implicit rh: RequestHeader): Future[Option[PendingChangeRequest]] = http.get(new URL(s"$url/${arn.value}")).execute[HttpResponse].map {
    response =>
      response.status match {
        case OK => Some(response.json.as[PendingChangeRequest](connectorReads))
        case NOT_FOUND => None
        case status =>
          logger.warn(s"[AgentServicesAccountConnector][find] - Unexpected response, received status: $status")
          None
      }
  }

  def insert(pendingChangeRequest: PendingChangeRequest)(implicit rh: RequestHeader): Future[Unit] = {
    http.post(new URL(url)).withBody(Json.toJson(pendingChangeRequest)(connectorWrites)).execute[HttpResponse].map { response =>
      response.status match {
        case NO_CONTENT => ()
        case status => throw UpstreamErrorResponse("[AgentServicesAccountConnector][insert] - Unable to insert record", status)
      }
    }
  }

  def delete(arn: Arn)(implicit rh: RequestHeader): Future[Boolean] = http.delete(new URL(s"$url/${arn.value}")).execute[HttpResponse].map { response =>
    response.status match {
      case NO_CONTENT => true
      case NOT_FOUND => false
      case status =>
        logger.warn(s"[AgentServicesAccountConnector][delete] - Unexpected response, received status: $status")
        false
    }
  }

}
