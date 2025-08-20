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
import uk.gov.hmrc.agentservicesaccount.models.SendEmailData
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class EmailConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2
)(implicit val ec: ExecutionContext)
extends Logging {

  private val baseUrl: String = appConfig.emailBaseUrl

  def sendEmail(
    emailData: SendEmailData
  )(implicit rh: RequestHeader): Future[Unit] = http.post(url"$baseUrl/hmrc/email").withBody(Json.toJson(emailData)).execute[Unit]

}
