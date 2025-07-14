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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.emailverification._
import uk.gov.hmrc.agentservicesaccount.utils.HttpAPIMonitor
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EmailVerificationConnector @Inject() (
  http: HttpClientV2,
  val metrics: Metrics
)(implicit
  val appConfig: AppConfig,
  val ec: ExecutionContext
)
extends HttpAPIMonitor
with Logging {

  def verifyEmail(request: VerifyEmailRequest)(implicit rh: RequestHeader): Future[Option[VerifyEmailResponse]] = {
    monitor(s"ConsumedAPI-email-verify-POST") {
      http.post(url"${appConfig.emailVerificationBaseUrl}/email-verification/verify-email").withBody(Json.toJson(request)).execute[HttpResponse]
        .map { response =>
          response.status match {
            case 201 => Some(response.json.as[VerifyEmailResponse])
            case status =>
              logger.error(s"verifyEmail error for $request; HTTP status: $status, message: $response")
              None
          }
        }
    }
  }

  def checkEmail(credId: String)(implicit rh: RequestHeader): Future[Option[VerificationStatusResponse]] = {
    monitor(s"ConsumedAPI-email-verification-status-GET") {
      http.get(url"${appConfig.emailVerificationBaseUrl}/email-verification/verification-status/$credId").execute[HttpResponse].map { response =>
        response.status match {
          case 200 => Some(response.json.as[VerificationStatusResponse])
          case 404 => Some(VerificationStatusResponse(List.empty))
          case status =>
            logger.error(s"email verification status error for $credId; HTTP status: $status, message: $response")
            None
        }
      }
    }
  }

}
