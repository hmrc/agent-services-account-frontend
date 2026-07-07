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

package utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.*
import uk.gov.hmrc.agentservicesaccount.utils.SanitiseLegacySubscriptionName.sanitise

class SanitiseLegacySubscriptionNameSpec
extends PlaySpec {

  private val acceptableName = "NAME"
  //    val ctSaNameRegex = """^[A-Za-z0-9 .,()/&\-'‘’]{1,54}$""".r
  //    val payeAgentNameRegex = """^[A-Za-z0-9 .,()@!-]{1,56}$""".r
//  ASA agencyName can be any string up to 40 chars

  //      TODO: 11803 Implement
  "SanitiseLegacySubscriptionName.sanitise" should {
    List(CT, SA, PAYE).foreach(legacyRegime => {
      s"do some stuff for all $legacyRegime" in {
        sanitise(acceptableName, legacyRegime) mustBe acceptableName
      }
    })

    List(CT, SA).foreach(legacyRegime => {
      s"do some stuff for some $legacyRegime" in {
        true mustBe false
      }
    })

    List(PAYE).foreach(legacyRegime => {
      s"do some stuff for one $legacyRegime" in {
        true mustBe false
      }
    })
  }

}
