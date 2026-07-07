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

  private val acceptableNameForAll = "A. Person, Acceptable-Co. (BA)"
  private val invalidCharactersForAll = "£$%^*=+[]{};:<>?~"

  "SanitiseLegacySubscriptionName.sanitise" should {
    List(CT, SA, PAYE).foreach(legacyRegime => {
      s"return the original name with removed characters empty for an acceptable name for $legacyRegime" in {
        val sanitisedLegacySubscriptionName = sanitise(acceptableNameForAll, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe acceptableNameForAll
        sanitisedLegacySubscriptionName.removedCharacters mustBe List.empty
      }

      s"remove invalid characters for all from sanitisedName and add to removed characters for $legacyRegime" in {
        val nameToSanitise = acceptableNameForAll + invalidCharactersForAll
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe acceptableNameForAll
        sanitisedLegacySubscriptionName.removedCharacters mustBe invalidCharactersForAll.split("").toList
      }
    })

    List(CT, SA).foreach(legacyRegime => {
      "@!".split("").foreach(invalidCtSaCharacter => {
        s"remove $invalidCtSaCharacter from sanitisedName and add to removed characters for $legacyRegime" in {
          val randomSplit = Math.floor(Math.random() * acceptableNameForAll.length).toInt
          val splitString = acceptableNameForAll.splitAt(randomSplit)
          val nameToSanitise = s"${splitString._1}$invalidCtSaCharacter${splitString._2}"
          val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
          sanitisedLegacySubscriptionName.sanitisedName mustBe acceptableNameForAll
          sanitisedLegacySubscriptionName.removedCharacters mustBe List(invalidCtSaCharacter)
        }
      })
    })

    List(PAYE).foreach(legacyRegime => {
      "/'‘’".split("").foreach(invalidPayeCharacter => {
        s"remove $invalidPayeCharacter from sanitisedName and add to removed characters for $legacyRegime" in {
          val randomSplit = Math.floor(Math.random() * acceptableNameForAll.length).toInt
          val splitString = acceptableNameForAll.splitAt(randomSplit)
          val nameToSanitise = s"${splitString._1}$invalidPayeCharacter${splitString._2}"
          val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
          sanitisedLegacySubscriptionName.sanitisedName mustBe acceptableNameForAll
          sanitisedLegacySubscriptionName.removedCharacters mustBe List(invalidPayeCharacter)
        }
      })

      s"replace ' & ' with ' and ' for $legacyRegime - one &" in {
        val nameToSanitise = "Fish & Chips"
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe "Fish and Chips"
        sanitisedLegacySubscriptionName.removedCharacters mustBe List("&")
      }

      s"replace '&' with ' and ' for $legacyRegime - one &" in {
        val nameToSanitise = "Fish&Chips"
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe "Fish and Chips"
        sanitisedLegacySubscriptionName.removedCharacters mustBe List("&")
      }

      s"replace ' & ' with ' and ' for $legacyRegime - multiple &" in {
        val nameToSanitise = "Fish & Chips & Beans & Peas"
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas"
        sanitisedLegacySubscriptionName.removedCharacters mustBe List("&", "&", "&")
      }

      s"replace '&' with ' and ' for $legacyRegime - multiple &" in {
        val nameToSanitise = "Fish&Chips&Beans&Peas"
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas"
        sanitisedLegacySubscriptionName.removedCharacters mustBe List("&", "&", "&")
      }

      s"replace ' & ' with ' and ' and '&' with ' and ' for $legacyRegime" in {
        val nameToSanitise = "Fish&Chips&Beans & Peas & Gravy"
        val sanitisedLegacySubscriptionName = sanitise(nameToSanitise, legacyRegime)
        sanitisedLegacySubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas and Gravy"
        sanitisedLegacySubscriptionName.removedCharacters mustBe List(
          "&",
          "&",
          "&",
          "&"
        )
      }
    })
  }

}
