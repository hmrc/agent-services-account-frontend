/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import play.api.Logger
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.SuspensionResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class AgentSuspensionConnector @Inject()(@Named("agent-suspension-baseUrl") baseUrl: URL, http: HttpGet) {

  def getSuspensionStatus(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionResponse] = {
    http.GET[SuspensionResponse](new URL(baseUrl, s"/agent-suspension/status/arn/${arn.value}").toString)
  } recoverWith {
    case ex: NotFoundException =>
      Logger.warn("unable to retrieve suspension status")
      Future successful SuspensionResponse(Set.empty)
  }
}