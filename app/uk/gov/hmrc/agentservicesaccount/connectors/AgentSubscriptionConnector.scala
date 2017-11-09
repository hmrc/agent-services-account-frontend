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
import javax.inject.{Inject, Named, Singleton}

import play.api.libs.json.JsValue
import play.utils.UriEncoding
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails

import scala.concurrent.Future

@Singleton
class AgentSubscriptionConnector @Inject()(@Named("agent-subscription-baseUrl") baseUrl: URL, http: HttpGet with HttpPost) {

  def getAgencyName(arn: Arn)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val encodedArn = UriEncoding.encodePathSegment(arn.value, "UTF-8")
    val url = new URL(baseUrl, s"/agent-subscription/test-only/agency-name/$encodedArn").toString

    http.GET[JsValue](url).map { json =>
      (json \ "name").asOpt[String]
    }
  }
}
