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

import play.api.http.Status.{NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AfiRelationshipConnector @Inject()(http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {
  private val baseUrl = new URL(appConfig.afiBaseUrl)

  def checkIrvAllowed(arn: Arn)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = new URL(baseUrl, s"/agent-fi-relationship/${arn.value}/irv-allowed")
    http
      .GET[HttpResponse](url.toString)
      .map {
        case HttpResponse(NO_CONTENT, _, _) => true
        case HttpResponse(NOT_FOUND, _, _)  => false
      }
  }

}
