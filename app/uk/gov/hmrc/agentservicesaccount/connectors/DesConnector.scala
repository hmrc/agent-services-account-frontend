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

import com.kenshoo.play.metrics.Metrics
import play.api.libs.json.{JsPath, Json, Reads}
import play.utils.UriEncoding
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.play.http.logging.Authorization
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

case class AgentRecordDetails(agencyDetails: Option[AgencyDetails])
case class AgencyDetails(agencyName: String)

object AgentRecordDetails {
  implicit val agencyDetailsRead: Reads[AgencyDetails] = Json.reads[AgencyDetails]

  implicit val agentRecordDetailsRead: Reads[AgentRecordDetails] = Json.reads[AgentRecordDetails]
}

@Singleton
class DesConnector @Inject()(@Named("des-baseUrl") baseUrl: URL,
                             @Named("des.authorization-token") authorizationToken: String,
                             @Named("des.environment") environment: String,
                             httpGet: HttpGet)
  extends AgentsHttpErrorMonitor {

  def getAgencyName(arn: Arn)(implicit hc: HeaderCarrier): Future[Option[String]] = {

    val encodedArn = UriEncoding.encodePathSegment(arn.value, "UTF-8")
    val url = new URL(baseUrl, s"/registration/personal-details/arn/$encodedArn")

    (for {
      agencyRecordDetails <- getWithDesHeaders[AgentRecordDetails]("GetAgentRecord", url)
    } yield agencyRecordDetails.agencyDetails.map(_.agencyName)
    ).recover {
        case _ => None
      }
  }

  private def getWithDesHeaders[A: HttpReads](apiName: String, url: URL)(implicit hc: HeaderCarrier): Future[A] = {
    val desHeaderCarrier = hc.copy(
      authorization = Some(Authorization(s"Bearer $authorizationToken")),
      extraHeaders = hc.extraHeaders :+ "Environment" -> environment)

    // TODO: After the great library upgrade, consider using kenshoo monitoring to monitor all calls, not just errors
    monitor(s"ConsumedAPI-DES-$apiName-GET") {
      httpGet.GET[A](url.toString)(implicitly[HttpReads[A]], desHeaderCarrier)
    }
  }
}
