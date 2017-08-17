/*
 * Copyright 2017 HM Revenue & Customs
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

import java.net.URL
import javax.inject.{Inject, Singleton}

import com.google.inject.name.Named
import play.api.Logger
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class SsoConnector @Inject()(http: HttpGet, @Named("sso-baseUrl") baseUrl: URL) extends AgentsHttpErrorMonitor {

  def validateExternalDomain(domain: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = new URL(baseUrl, s"/sso/validate/domain/$domain")
    http.GET(url.toString)
      .map(_ => true)
      .recover {
        case e: BadRequestException => false
        case e: Exception =>
          Logger.error(s"Unable to validate domain $domain", e)
          false
      }
  }
}
