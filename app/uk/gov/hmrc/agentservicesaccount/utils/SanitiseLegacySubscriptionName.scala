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

package uk.gov.hmrc.agentservicesaccount.utils

import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.*

case class SanitisedLegacySubscriptionName(sanitisedName: String, removedCharacters: List[String])

object SanitiseLegacySubscriptionName {

  def sanitise(name: String, legacyRegime: LegacyRegime): SanitisedLegacySubscriptionName = legacyRegime match {
    case CT | SA => sanitiseForCtSa(name)
    case PAYE => sanitiseForPaye(name)
  }

  private def sanitiseForCtSa(name: String): SanitisedLegacySubscriptionName = {
    val allowedCharacterForCtSa = """^[A-Za-z0-9 .,()/&\-'‘’]$""".r
    val nameSplitByAllowedCharacters: Map[Boolean, List[String]] = name.split("").toList.groupBy(allowedCharacterForCtSa.matches)
    val sanitisedName = nameSplitByAllowedCharacters.getOrElse(true, List.empty).mkString
    val removedCharacters = nameSplitByAllowedCharacters.getOrElse(false, List.empty)
    SanitisedLegacySubscriptionName(sanitisedName, removedCharacters)
  }

  private def sanitiseForPaye(name: String): SanitisedLegacySubscriptionName = {
    val nameWithAmpersandReplaced = name.replaceAll(" & ", " and ").replaceAll("&", " and ")
    val allowedCharacterForPaye= """^[A-Za-z0-9 .,()@!-]$""".r
    val nameSplitByAllowedCharacters: Map[Boolean, List[String]] = nameWithAmpersandReplaced.split("").toList.groupBy(allowedCharacterForPaye.matches)
    val sanitisedName = nameSplitByAllowedCharacters.getOrElse(true, List.empty).mkString
    val removedCharacters = name.filter(_ == '&').map(_.toString).toList ++ nameSplitByAllowedCharacters.getOrElse(false, List.empty)
    SanitisedLegacySubscriptionName(sanitisedName, removedCharacters)
  }

}
