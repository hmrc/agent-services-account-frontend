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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.AgentRegime.*
import uk.gov.hmrc.agentservicesaccount.utils.SanitiseSubscriptionName.sanitise

class SanitiseSubscriptionNameSpec
extends PlaySpec {

  private val acceptableNameForAll = "A. Person, Acceptable-Co. (BA)"
  private val invalidCharactersForAll = "£$%^*=+[]{};:<>?~"

  "SanitiseSubscriptionName.sanitise" should {
    List(CT, SA, PAYE).foreach(agentRegime => {
      s"return the original name with removed characters empty for an acceptable name for $agentRegime" in {
        val sanitisedSubscriptionName = sanitise(acceptableNameForAll, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe acceptableNameForAll
        sanitisedSubscriptionName.removedCharacters mustBe List.empty
      }

      s"remove invalid characters for all from sanitisedName and add to removed characters for $agentRegime" in {
        val nameToSanitise = acceptableNameForAll + invalidCharactersForAll
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe acceptableNameForAll
        sanitisedSubscriptionName.removedCharacters mustBe invalidCharactersForAll.split("").toList
      }

      s"strip diacritics from sanitisedName and add to removed characters for $agentRegime" in {
        val resume = "résumé"
        val sanitisedResume = sanitise(resume, agentRegime)
        sanitisedResume.sanitisedName mustBe "resume"
        sanitisedResume.removedCharacters mustBe List("é", "é")

        val pinata = "piñata"
        val sanitisedPinata = sanitise(pinata, agentRegime)
        sanitisedPinata.sanitisedName mustBe "pinata"
        sanitisedPinata.removedCharacters mustBe List("ñ")

        val facade = "façade"
        val sanitisedFacade = sanitise(facade, agentRegime)
        sanitisedFacade.sanitisedName mustBe "facade"
        sanitisedFacade.removedCharacters mustBe List("ç")

        val naive = "naïve"
        val sanitisedNaive = sanitise(naive, agentRegime)
        sanitisedNaive.sanitisedName mustBe "naive"
        sanitisedNaive.removedCharacters mustBe List("ï")
      }
    })

    List(CT, SA).foreach(agentRegime => {
      "@!".split("").foreach(invalidCtSaCharacter => {
        s"remove $invalidCtSaCharacter from sanitisedName and add to removed characters for $agentRegime" in {
          val randomSplit = Math.floor(Math.random() * acceptableNameForAll.length).toInt
          val splitString = acceptableNameForAll.splitAt(randomSplit)
          val nameToSanitise = s"${splitString._1}$invalidCtSaCharacter${splitString._2}"
          val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
          sanitisedSubscriptionName.sanitisedName mustBe acceptableNameForAll
          sanitisedSubscriptionName.removedCharacters mustBe List(invalidCtSaCharacter)
        }
      })
    })

    List(PAYE).foreach(agentRegime => {
      "/'‘’".split("").foreach(invalidPayeCharacter => {
        s"remove $invalidPayeCharacter from sanitisedName and add to removed characters for $agentRegime" in {
          val randomSplit = Math.floor(Math.random() * acceptableNameForAll.length).toInt
          val splitString = acceptableNameForAll.splitAt(randomSplit)
          val nameToSanitise = s"${splitString._1}$invalidPayeCharacter${splitString._2}"
          val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
          sanitisedSubscriptionName.sanitisedName mustBe acceptableNameForAll
          sanitisedSubscriptionName.removedCharacters mustBe List(invalidPayeCharacter)
        }
      })

      s"replace ' & ' with ' and ' for $agentRegime - one &" in {
        val nameToSanitise = "Fish & Chips"
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe "Fish and Chips"
        sanitisedSubscriptionName.removedCharacters mustBe List("&")
      }

      s"replace '&' with ' and ' for $agentRegime - one &" in {
        val nameToSanitise = "Fish&Chips"
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe "Fish and Chips"
        sanitisedSubscriptionName.removedCharacters mustBe List("&")
      }

      s"replace ' & ' with ' and ' for $agentRegime - multiple &" in {
        val nameToSanitise = "Fish & Chips & Beans & Peas"
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas"
        sanitisedSubscriptionName.removedCharacters mustBe List("&", "&", "&")
      }

      s"replace '&' with ' and ' for $agentRegime - multiple &" in {
        val nameToSanitise = "Fish&Chips&Beans&Peas"
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas"
        sanitisedSubscriptionName.removedCharacters mustBe List("&", "&", "&")
      }

      s"replace ' & ' with ' and ' and '&' with ' and ' for $agentRegime" in {
        val nameToSanitise = "Fish&Chips&Beans & Peas & Gravy"
        val sanitisedSubscriptionName = sanitise(nameToSanitise, agentRegime)
        sanitisedSubscriptionName.sanitisedName mustBe "Fish and Chips and Beans and Peas and Gravy"
        sanitisedSubscriptionName.removedCharacters mustBe List(
          "&",
          "&",
          "&",
          "&"
        )
      }
    })
  }

}
