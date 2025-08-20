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

import play.api.http.Status.BAD_REQUEST
import play.api.http.Status.CREATED
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentAssuranceConnector @Inject() (httpV2: HttpClientV2)(implicit
  appConfig: AppConfig,
  val ec: ExecutionContext
) {

  private lazy val baseUrl = appConfig.agentAssuranceBaseUrl

  import uk.gov.hmrc.http.HttpReads.Implicits._

  def getAMLSDetailsResponse(
    arn: String
  )(implicit rh: RequestHeader): Future[AmlsDetailsResponse] = httpV2.get(new URL(s"$baseUrl/agent-assurance/amls/arn/$arn")).execute[HttpResponse].map {
    response =>
      response.status match {
        case OK => Json.parse(response.body).as[AmlsDetailsResponse]
        case BAD_REQUEST => throw UpstreamErrorResponse(s"Error $BAD_REQUEST invalid ARN when trying to get amls details", BAD_REQUEST)
        case e => throw UpstreamErrorResponse(s"Error $e unable to get amls details", e)
      }
  }

  def getAMLSDetails(arn: String)(implicit rh: RequestHeader): Future[AmlsDetails] = getAMLSDetailsResponse(arn).map(_.details
    .getOrElse(throw UpstreamErrorResponse(s"Error $BAD_REQUEST invalid ARN when trying to get amls details", BAD_REQUEST)))

  def getAmlsStatus(arn: Arn)(implicit
    ec: ExecutionContext,
    rh: RequestHeader
  ): Future[AmlsStatus] = getAMLSDetailsResponse(arn.value).map(_.status)

  def postAmlsDetails(
    arn: Arn,
    amlsRequest: AmlsRequest
  )(implicit rh: RequestHeader): Future[Unit] = {
    httpV2
      .post(new URL(s"$baseUrl/agent-assurance/amls/arn/${arn.value}")).withBody(Json.toJson(amlsRequest)).execute[HttpResponse]
      .map {
        response =>
          response.status match {
            case CREATED => Future.successful(())
            case BAD_REQUEST => throw UpstreamErrorResponse(s"Error $BAD_REQUEST invalid request", BAD_REQUEST)
            case e => throw UpstreamErrorResponse(s"Error $e unable to post amls details", e)
          }
      }
  }

  def postDesignatoryDetails(
    arn: Arn,
    base64HtmlForPdf: String
  )(implicit rh: RequestHeader): Future[Unit] = {
    httpV2
      .post(new URL(s"$baseUrl/agent-assurance/agent/agency-details/arn/${arn.value}")).withBody(base64HtmlForPdf).execute[HttpResponse]
      .map {
        response =>
          response.status match {
            case CREATED => Future.successful(())
            case e => throw UpstreamErrorResponse(s"Error $e unable to post designatory details", e)
          }
      }
  }

  def getAgentRecord(implicit rh: RequestHeader): Future[AgentDetailsDesResponse] = httpV2
    .get(new URL(s"$baseUrl/agent-assurance/agent-record-with-checks"))
    .execute[HttpResponse].map(response =>
      response.status match {
        case OK => Json.parse(response.body).as[AgentDetailsDesResponse]
        case other => throw UpstreamErrorResponse(s"agent record unavailable: des response code: $other", 500)
      }
    )

}
