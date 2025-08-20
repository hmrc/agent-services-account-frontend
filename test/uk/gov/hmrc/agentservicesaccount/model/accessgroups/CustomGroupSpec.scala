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
import play.api.libs.json.JsValue
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
  val agent: AgentUser = AgentUser(
    id = "userId",
    name = "userName"
  )
  val user1: AgentUser = AgentUser(
    id = "user1",
    name = "User 1"
  )
  val user2: AgentUser = AgentUser(
    id = "user2",
    name = "User 2"
  )
  val client1: Client = Client(
    enrolmentKey = "HMRC-MTD-VAT~VRN~101747641",
    friendlyName = "John Innes"
  )
  val client2: Client = Client(
    enrolmentKey = "HMRC-PPT-ORG~EtmpRegistrationNumber~XAPPT0000012345",
    friendlyName = "Frank Wright"
  )
  val client3: Client = Client(
    enrolmentKey = "HMRC-CGT-PD~CgtRef~XMCGTP123456789",
    friendlyName = "George Candy"
  )
  val now: LocalDateTime = LocalDateTime.now()
  val id: UUID = UUID.randomUUID()
  val testCustomGroup: CustomGroup = CustomGroup(
    id = id,
    arn = arn,
    groupName = groupName,
    created = now,
    lastUpdated = now,
    createdBy = agent,
    lastUpdatedBy = agent,
    teamMembers = Set(
      agent,
      user1,
      user2
    ),
    clients = Set(
      client1,
      client2,
      client3
    )
  )
  val testCustomGroupJson: JsValue = Json.parse(
    // language=JSON
    s"""
       {
         "id": "${testCustomGroup.id}",
         "arn": "${testCustomGroup.arn.value}",
         "groupName": "${testCustomGroup.groupName}",
         "created": "${testCustomGroup.created}",
         "lastUpdated": "${testCustomGroup.lastUpdated}",
         "createdBy": {
           "id": "${testCustomGroup.createdBy.id}",
           "name": "${testCustomGroup.createdBy.name}"
         },
         "lastUpdatedBy": {
           "id": "${testCustomGroup.lastUpdatedBy.id}",
           "name": "${testCustomGroup.lastUpdatedBy.name}"
         },
         "teamMembers": [
            {
              "id": "${agent.id}",
              "name": "${agent.name}"
            },
            {
              "id": "${user1.id}",
              "name": "${user1.name}"
            },
            {
              "id": "${user2.id}",
              "name": "${user2.name}"
            }
         ],
         "clients": [
           {
             "enrolmentKey": "${client1.enrolmentKey}",
             "friendlyName": "${client1.friendlyName}"
           },
           {
              "enrolmentKey": "${client2.enrolmentKey}",
              "friendlyName": "${client2.friendlyName}"
           },
           {
              "enrolmentKey": "${client3.enrolmentKey}",
              "friendlyName": "${client3.friendlyName}"
           }
         ]
       }
     """
  )

  "AccessGroup" should "serialise to JSON and deserialize from string" in {
    testCustomGroup.isInstanceOf[AccessGroup] shouldBe true
    Json.toJson(testCustomGroup) shouldBe testCustomGroupJson
    testCustomGroupJson.as[CustomGroup] shouldBe testCustomGroup
  }

  "Creating a group summary from a custom group" should "work properly" in {
    val expectedGroupSummary = GroupSummary(
      groupId = id,
      groupName = testCustomGroup.groupName,
      clientCount = Some(testCustomGroup.clients.size),
      teamMemberCount = testCustomGroup.teamMembers.size,
      taxService = None
    )
    val groupSummary = GroupSummary.of(testCustomGroup)
    groupSummary shouldBe expectedGroupSummary
    groupSummary.isTaxGroup shouldBe false
    groupSummary.groupType shouldBe "custom"

  }

}
