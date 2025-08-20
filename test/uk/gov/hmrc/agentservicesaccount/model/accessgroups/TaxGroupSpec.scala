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
import play.api.libs.json.JsValue
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

  val id: UUID = UUID.randomUUID()
  val dateTimeString: String = "2025-08-20T10:35:43.32378"
  val dateTime: LocalDateTime = LocalDateTime.parse(dateTimeString)
  def makeTaxGroup(
    service: String,
    excludedClients: Set[Client] = Set.empty,
    automaticUpdates: Boolean = false
  ): TaxGroup = TaxGroup(
    id = id,
    arn = arn,
    groupName = groupName,
    created = dateTime,
    lastUpdated = dateTime,
    createdBy = agent,
    lastUpdatedBy = agent,
    teamMembers = Set(
      agent,
      user1,
      user2
    ),
    service = service,
    automaticUpdates = automaticUpdates,
    excludedClients = excludedClients
  )

  def makeTaxGroupJson(
    service: String,
    excludedClients: Set[Client] = Set.empty,
    automaticUpdates: Boolean = false
  ): JsValue = Json.parse(
    // language=JSON
    s"""
         {
           "id": "$id",
           "arn": "${arn.value}",
           "groupName": "$groupName",
           "created": "$dateTimeString",
           "lastUpdated": "$dateTimeString",
           "createdBy": {
             "id": "${agent.id}",
             "name": "${agent.name}"
           },
           "lastUpdatedBy": {
             "id": "${agent.id}",
             "name": "${agent.name}"
           },
           "teamMembers": [
             {
                "id": "${agent.id}",
                "name": "${agent.name}"},
             {
                "id": "${user1.id}",
                "name": "${user1.name}"
             },
             {
                "id": "${user2.id}",
                "name": "${user2.name}"
             }
           ],
           "service": "$service",
           "automaticUpdates": $automaticUpdates,
           "excludedClients": ${Json.toJson(excludedClients)}
         }
       """
  )

  "TaxServiceAccessGroup" should "serialise to JSON and deserialize from string" in {
    val testTaxGroup: TaxGroup = makeTaxGroup(
      service = "HMRC-MTD-VAT",
      excludedClients = Set(client1)
    )
    val testTaxGroupJson: JsValue = makeTaxGroupJson(
      service = "HMRC-MTD-VAT",
      excludedClients = Set(client1)
    )

    Json.toJson(testTaxGroup) shouldBe testTaxGroupJson
    testTaxGroupJson.as[TaxGroup] shouldBe testTaxGroup
  }

  "TaxServiceAccessGroup for trusts" should "serialise to JSON and deserialize from string" in {
    val testTaxGroup: TaxGroup = makeTaxGroup(
      service = "TRUST",
      automaticUpdates = true
    )
    val testTaxGroupJson: JsValue = makeTaxGroupJson(
      service = "TRUST",
      automaticUpdates = true
    )

    Json.toJson(testTaxGroup) shouldBe testTaxGroupJson
    testTaxGroupJson.as[TaxGroup] shouldBe testTaxGroup
    testTaxGroup.isInstanceOf[AccessGroup] shouldBe true
  }

  "Creating a group summary from a tax group" should "work properly" in {
    val service = "TRUST"
    val taxGroup: TaxGroup = makeTaxGroup(
      service = service
    )
    val expectedGroupSummary = GroupSummary(
      groupId = id,
      groupName = taxGroup.groupName,
      clientCount = None,
      teamMemberCount = taxGroup.teamMembers.size,
      taxService = Some(service)
    )
    val groupSummary = GroupSummary.of(taxGroup)

    groupSummary shouldBe expectedGroupSummary
    groupSummary.isTaxGroup shouldBe expectedGroupSummary.isTaxGroup
    groupSummary.groupType shouldBe "tax"

  }

}
