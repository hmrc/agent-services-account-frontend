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

//TODO: 11803 Will want to return removedCharacters for logging purposes aswell
case class SanitisedLegacySubscriptionName(sanitisedName: String, removedCharacters: String)

object SanitiseLegacySubscriptionName {

  def sanitise(name: String, legacyRegime: LegacyRegime): String = legacyRegime match {
    //  ASA agencyName can be any string up to 40 chars
    case CT | SA => sanitiseForCtSa(name)
    case PAYE => sanitiseForPaye(name)
  }

  //      TODO: 11803 Implement
  private def sanitiseForCtSa(name: String): String = {
//    val ctSaNameRegex = """^[A-Za-z0-9 .,()/&\-'‘’]{1,54}$""".r
    name
  }

  //      TODO: 11803 Implement
  private def sanitiseForPaye(name: String): String = {
//    val payeAgentNameRegex = """^[A-Za-z0-9 .,()@!-]{1,56}$""".r
    name
  }

}
