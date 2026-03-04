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

import com.google.inject.ImplementedBy
import play.api.http.Status.{BAD_REQUEST, CREATED}
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.paye._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
final class PayeSubscriptionConnector @Inject() (httpV2: HttpClientV2)(implicit
   appConfig: AppConfig,
   val ec: ExecutionContext
) {

  def getCyaData: Future[PayeCyaData] = Future.successful(
    PayeCyaData(
      agentName = "Example Agent Ltd",
      contactName = "Jane Agent",
      telephoneNumber = Some("01632 960 001"),
      emailAddress = Some("jane.agent@example.com"),
      address = PayeAddress(
        line1 = "1 High Street",
        line2 = "Village",
        line3 = Some("County"),
        line4 = None,
        postCode = "AA1 1AA"
      )
    )
  )

//  TODO: 10593 Implement correct call to this endpoint
  def submitRequest(): Future[Unit] = {
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
    Future.successful(())
  }

}
