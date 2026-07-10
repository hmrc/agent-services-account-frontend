/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.http.Status.*
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.Es5GroupAllocatedEnrolment
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.UpstreamErrorResponse

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EnrolmentStoreProxyConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
extends Logging {

  private val baseUrl = appConfig.enrolmentStoreProxyBaseUrl

  def getGroupAllocatedEnrolment(
    groupId: String,
    regime: LegacyRegime,
    agentReference: String
  )(using hc: HeaderCarrier): Future[Option[Es5GroupAllocatedEnrolment]] = {

    val enrolmentKey = s"${regime.enrolmentKey}~${regime.agentReferenceKey}~$agentReference"

    http
      .get(url"$baseUrl/enrolment-store-proxy/enrolment-store/groups/$groupId/enrolments/$enrolmentKey")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => Some(response.json.as[Es5GroupAllocatedEnrolment])
          case NOT_FOUND | NO_CONTENT => None
          case other =>
            logger.warn(
              s"[ES5] Unexpected response groupId=$groupId enrolmentKey=$enrolmentKey status=$other body=${response.body}"
            )
            throw UpstreamErrorResponse(response.body, other)
        }
      }
  }

}
