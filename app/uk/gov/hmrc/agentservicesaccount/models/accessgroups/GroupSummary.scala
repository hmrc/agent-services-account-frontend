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

  def groupType: String =
    if (taxService.isDefined)
      "tax"
    else
      "custom" // used for url context paths

}

object GroupSummary {
  implicit val format: OFormat[GroupSummary] = Json.format[GroupSummary]
}
