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

package uk.gov.hmrc.agentservicesaccount.controllers.desiDetails.util

import uk.gov.hmrc.agentservicesaccount.actions.AuthRequestWithAgentInfo
import uk.gov.hmrc.agentservicesaccount.connectors.AgentClientAuthorisationConnector
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

object CurrentAgencyDetails {

  def get(acaConnector: AgentClientAuthorisationConnector)
                                     (implicit hc: HeaderCarrier,
                                      request: AuthRequestWithAgentInfo[_],
                                      ec: ExecutionContext): Future[AgencyDetails] = {
    acaConnector.getAgentRecord().map(_.agencyDetails.getOrElse {
      val arn = request.agentInfo.arn
      throw new RuntimeException(s"Could not retrieve current agency details for $arn from the backend")
    })
  }
}
