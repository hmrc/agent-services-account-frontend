/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.Reads._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

case class AgencyName(agencyName: String)

@Singleton
class AgentServicesAccountConnector @Inject()(@Named("agent-services-account-baseUrl") baseUrl: URL, httpGet: HttpGet) {
  def getAgencyName(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    val url = new URL(baseUrl, s"/agent-services-account/agent/agency-name")

    httpGet.GET[HttpResponse](url.toString).map { response =>
      response.status match {
        case 200 => (response.json \ "agencyName").toOption.map(_.as[String])
        case _ => None
      }
    }.recoverWith {
      case _: Upstream5xxResponse | _: HttpException => Future.successful(None)
    }
  }
}