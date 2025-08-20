/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models.accessgroups

import play.api.libs.json._

import java.util.UUID

case class GroupSummary(
  groupId: UUID,
  groupName: String,
  clientCount: Option[Int], // Will not be populated for tax service groups
  teamMemberCount: Int,
  taxService: Option[String] = None // Will only be populated for tax service groups
) {

  def isTaxGroup: Boolean = taxService.isDefined
  def isCustomGroup: Boolean = taxService.isEmpty
  def groupType: String =
    if (isTaxGroup)
      "tax"
    else
      "custom" // used for url context paths

}

object GroupSummary {

  def of(accessGroup: AccessGroup): GroupSummary =
    accessGroup match {
      case taxGroup: TaxGroup =>
        GroupSummary(
          groupId = taxGroup.id,
          groupName = taxGroup.groupName,
          clientCount = None, // info not retained in group - group could be empty
          teamMemberCount = taxGroup.teamMembers.size,
          taxService = Some(taxGroup.service)
        )
      case customGroup: CustomGroup =>
        GroupSummary(
          groupId = customGroup.id,
          groupName = customGroup.groupName,
          clientCount = Some(customGroup.clients.size),
          teamMemberCount = customGroup.teamMembers.size,
          taxService = None
        )
    }

  implicit val format: OFormat[GroupSummary] = Json.format[GroupSummary]

}
