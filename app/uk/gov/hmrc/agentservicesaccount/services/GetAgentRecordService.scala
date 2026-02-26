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

package uk.gov.hmrc.agentservicesaccount.services

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse

import scala.concurrent.Future

@Singleton
class GetAgentRecordService @Inject() (
  agentAssuranceConnector: AgentAssuranceConnector,
  agentServicesAccountConnector: AgentServicesAccountConnector
)(
  implicit appConfig: AppConfig
) {

  def getAgentRecord(implicit rh: RequestHeader): Future[AgentDetailsDesResponse] =
    if (appConfig.enableAgentRecordViaAsa)
      agentServicesAccountConnector.getAgentRecord
    else
      agentAssuranceConnector.getAgentRecord

}
