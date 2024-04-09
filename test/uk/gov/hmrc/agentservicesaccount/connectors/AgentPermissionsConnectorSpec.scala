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

import akka.Done
import play.api.test.Helpers._
import play.api.test.Injecting
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class AgentPermissionsConnectorSpec extends BaseISpec with Injecting {

  val connector: AgentPermissionsConnector = inject[AgentPermissionsConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  "isShownPrivateBetaInvite check" should {
    "return false when agent-permissions returns 200 Ok" in {
      givenHidePrivateBetaInvite()
      await(connector.isShownPrivateBetaInvite) shouldBe false
    }

    "return true when agent-permissions returns 404 Not found" in {
      givenHidePrivateBetaInviteNotFound()
      await(connector.isShownPrivateBetaInvite) shouldBe true
    }
  }

  "declinePrivateBetaInvite" should {
    "return Done when agent-permissions returns 201 Created" in {
      givenHideBetaInviteResponse()
      await(connector.declinePrivateBetaInvite()) shouldBe Done
    }

    "return Done when agent-permissions returns 404 Conflict" in {
      givenHideBetaInviteResponse(conflict = true)
      await(connector.declinePrivateBetaInvite()) shouldBe Done
    }

    "throw error if unsupported status" in {
      givenHideBetaInviteResponse(conflict = true)
      await(connector.declinePrivateBetaInvite()) shouldBe Done
    }
  }
}
