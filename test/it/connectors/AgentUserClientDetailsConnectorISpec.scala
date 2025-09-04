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

package it.connectors

import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Injecting
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.connectors.AgentUserClientDetailsConnector
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.UserDetails
import stubs.AgentUserClientDetailsStubs.stubGetTeamMembersForArn

class AgentUserClientDetailsConnectorISpec
extends BaseISpec
with Injecting {

  private lazy val connector = inject[AgentUserClientDetailsConnector]

  "getTeamMembers" should {
    "return the team members" in {
      val teamMembers = List(
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Robert Builder"),
          email = Some("bob@builder.com")
        ),
        UserDetails(
          credentialRole = Some("User"),
          name = Some("Steve Smith"),
          email = Some("steve@builder.com")
        ),
        // Assistant will be filtered out from the results we get back
        UserDetails(credentialRole = Some("Assistant"), name = Some("irrelevant"))
      )
      stubGetTeamMembersForArn(arn, teamMembers)
      await(connector.getTeamMembers(arn)) shouldBe Some(teamMembers)
    }
  }

}
