/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models

import play.api.libs.json._

/*
  Stripped-down representation of AccessGroupSummary/AccessGroupSummaries for the sole purpose of determining
  if the client has any access groups at all.
 */

case class AccessGroupSummary(groupId: String)

object AccessGroupSummary {
  implicit val formatAccessGroupSummary: OFormat[AccessGroupSummary] = Json.format[AccessGroupSummary]
}

case class AccessGroupSummaries(groups: Seq[AccessGroupSummary])

object AccessGroupSummaries {
  implicit val format: OFormat[AccessGroupSummaries] = Json.format[AccessGroupSummaries]
}
