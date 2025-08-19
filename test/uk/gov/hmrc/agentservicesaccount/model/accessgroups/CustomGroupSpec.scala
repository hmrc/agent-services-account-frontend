/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.model.accessgroups

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.AccessGroup
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.AgentUser
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.Client
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.CustomGroup
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.GroupSummary

import java.time.LocalDateTime
import java.util.UUID

class CustomGroupSpec
extends AnyFlatSpec
with Matchers {

  val arn: Arn = Arn("KARN1234567")
  val groupName = "some group"
  val agent: AgentUser = AgentUser("userId", "userName")
  val user1: AgentUser = AgentUser("user1", "User 1")
  val user2: AgentUser = AgentUser("user2", "User 2")

  val client1: Client = Client("HMRC-MTD-VAT~VRN~101747641", "John Innes")
  val client2: Client = Client("HMRC-PPT-ORG~EtmpRegistrationNumber~XAPPT0000012345", "Frank Wright")
  val client3: Client = Client("HMRC-CGT-PD~CgtRef~XMCGTP123456789", "George Candy")

  val now: LocalDateTime = LocalDateTime.now()
  val id = UUID.randomUUID()

  "AccessGroup" should "serialise to JSON and deserialize from string" in {

    val customGroup: CustomGroup = CustomGroup(
      id,
      arn,
      groupName,
      now,
      now,
      agent,
      agent,
      Set(
        agent,
        user1,
        user2
      ),
      Set(
        client1,
        client2,
        client3
      )
    )

    customGroup.isInstanceOf[AccessGroup] shouldBe true

    val serialised = Json.toJson(customGroup).toString
    Json.fromJson[CustomGroup](Json.parse(serialised)) shouldBe JsSuccess(customGroup)
  }

  "Creating a group summary from a custom group" should "work properly" in {

    val customGroup: CustomGroup = CustomGroup(
      id,
      arn,
      groupName,
      now,
      now,
      agent,
      agent,
      Set(agent, user1),
      Set(
        client1,
        client2,
        client3
      )
    )

    val groupSummary = GroupSummary.of(customGroup)
    groupSummary.taxService shouldBe None
    groupSummary.groupId shouldBe id
    groupSummary.isTaxGroup shouldBe false
    groupSummary.clientCount.get shouldBe 3
    groupSummary.groupName shouldBe groupName
    groupSummary.teamMemberCount shouldBe 2
    groupSummary.groupType shouldBe "custom"

  }

}
