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
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.GroupSummary
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.TaxGroup

import java.time.LocalDateTime
import java.util.UUID

class TaxGroupSpec
extends AnyFlatSpec
with Matchers {

  val arn: Arn = Arn("KARN1234567")
  val groupName = "some group"
  val agent: AgentUser = AgentUser("userId", "userName")
  val user1: AgentUser = AgentUser("user1", "User 1")
  val user2: AgentUser = AgentUser("user2", "User 2")
  val client1: Client = Client("HMRC-MTD-VAT~VRN~101747641", "John Innes")

  val id = UUID.randomUUID()
  val now: LocalDateTime = LocalDateTime.now()

  "TaxServiceAccessGroup" should "serialise to JSON and deserialize from string" in {
    val service: String = "HMRC-MTD-VAT"
    val now = LocalDateTime.now()

    val accessGroup: TaxGroup = TaxGroup(
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
      service,
      automaticUpdates = false,
      Set(client1)
    )

    val serialised = Json.toJson(accessGroup).toString
    Json.fromJson[TaxGroup](Json.parse(serialised)) shouldBe JsSuccess(accessGroup)
  }

  "TaxServiceAccessGroup for trusts" should "serialise to JSON and deserialize from string" in {
    val service: String = "TRUST"

    val taxGroup: TaxGroup = TaxGroup(
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
      service,
      automaticUpdates = true,
      Set.empty
    )

    val jsonString = Json.toJson(taxGroup).toString
    Json.fromJson[TaxGroup](Json.parse(jsonString)) shouldBe JsSuccess(taxGroup)
    taxGroup.isInstanceOf[AccessGroup] shouldBe true
  }

  "Creating a group summary from a tax group" should "work properly" in {
    val service: String = "TRUST"

    val taxGroup: TaxGroup = TaxGroup(
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
      service = service,
      automaticUpdates = false,
      Set.empty
    )

    val groupSummary = GroupSummary.of(taxGroup)
    groupSummary.taxService shouldBe Some(service)
    groupSummary.groupId shouldBe id
    groupSummary.isTaxGroup shouldBe true
    groupSummary.clientCount shouldBe None
    groupSummary.groupName shouldBe groupName
    groupSummary.teamMemberCount shouldBe 3
    groupSummary.groupType shouldBe "tax"

  }

}
