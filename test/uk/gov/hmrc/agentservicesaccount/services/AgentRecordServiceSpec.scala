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

import com.google.inject.AbstractModule
import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.connectors.AgentAssuranceConnector
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.models._

import scala.concurrent.Future

class AgentRecordServiceSpec
extends PlaySpec
with IdiomaticMockito
with ArgumentMatchersSugar {

  implicit val rh: RequestHeader = FakeRequest()

  val agentDetails: AgencyDetails = AgencyDetails(
    Some("My Agency"),
    Some("abc@abc.com"),
    Some("07345678901"),
    Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"
    ))
  )

  val suspensionDetails: SuspensionDetails = SuspensionDetails(suspensionStatus = false, None)

  val agentRecord: AgentDetailsDesResponse = AgentDetailsDesResponse(
    uniqueTaxReference = Some(Utr("0123456789")),
    agencyDetails = Some(agentDetails),
    suspensionDetails = Some(suspensionDetails)
  )

  "getAgentRecord" should {
    "get the agent record from agent assurance when the feature switch is false" in {

      val mockAgentAssuranceConnector: AgentAssuranceConnector = mock[AgentAssuranceConnector]

      val overrides =
        new AbstractModule() {
          override def configure(): Unit = {
            bind(classOf[AgentAssuranceConnector]).toInstance(mockAgentAssuranceConnector)
          }
        }
      val app = new GuiceApplicationBuilder().configure("features.enable-agent-record-via-asa" -> false)
        .overrides(overrides).build()

      val service: AgentRecordService = app.injector.instanceOf[AgentRecordService]

      (mockAgentAssuranceConnector.getAgentRecord(*[RequestHeader]).returns(Future.successful(agentRecord)))

      val result = service.getAgentRecord.futureValue

      result mustBe agentRecord
    }

    "get the agent record from agent services account when the feature switch is true" in {

      val mockAgentServicesAccountConnector: AgentServicesAccountConnector = mock[AgentServicesAccountConnector]

      val overrides =
        new AbstractModule() {
          override def configure(): Unit = {
            bind(classOf[AgentServicesAccountConnector]).toInstance(mockAgentServicesAccountConnector)
          }
        }
      val app = new GuiceApplicationBuilder().configure("features.enable-agent-record-via-asa" -> true)
        .overrides(overrides).build()

      val service: AgentRecordService = app.injector.instanceOf[AgentRecordService]

      mockAgentServicesAccountConnector.getAgentRecord(*[RequestHeader]).returns(Future.successful(agentRecord))

      val result = service.getAgentRecord.futureValue

      result mustBe agentRecord
    }
  }

}
