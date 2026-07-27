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

package uk.gov.hmrc.agentservicesaccount.models

import org.scalatest.EitherValues
import org.scalatest.Inspectors
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

class PostCodeSpec
extends UnitSpec,
  EitherValues,
  Inspectors {

  private val LegacyRegimes = Seq(
    LegacyRegime.PAYE,
    LegacyRegime.SA,
    LegacyRegime.CT
  )

  "Parsing a post code" can {
    forAll(LegacyRegimes) { legacyRegime =>
      s"mapping a $legacyRegime post code" should {
        val mapping = PostCode.mapping(legacyRegime)

        "handle valid postcodes" in {
          mapping.bind(Map("" -> "G1 1XQ")).value shouldBe "G1 1XQ"
          mapping.bind(Map("" -> "W12 7FW")).value shouldBe "W12 7FW"
          mapping.bind(Map("" -> "SW1A 2AA")).value shouldBe "SW1A 2AA"
          mapping.bind(Map("" -> "TF4 3TR")).value shouldBe "TF4 3TR"
          mapping.bind(Map("" -> "EC1A 1BB")).value shouldBe "EC1A 1BB"
          mapping.bind(Map("" -> "W1A 0AX")).value shouldBe "W1A 0AX"
          mapping.bind(Map("" -> "BFPO 1234")).value shouldBe "BFPO 1234"
        }

        "reject invalid postcodes" in {
          mapping.bind(Map("" -> "INVALID")).left.value should not be empty
          mapping.bind(Map("" -> "123456")).left.value should not be empty
          mapping.bind(Map("" -> "SW1A")).left.value should not be empty
          mapping.bind(Map("" -> "SW1A 2A")).left.value should not be empty
          mapping.bind(Map("" -> "")).left.value should not be empty
        }

      }
    }

    "normalise valid but non-standard postcodes" in {
      // missing space
      PostCode.normalise("G11XQ") shouldBe "G1 1XQ"
      PostCode.normalise("W127FW") shouldBe "W12 7FW"
      PostCode.normalise("SW1A2AA") shouldBe "SW1A 2AA"
      PostCode.normalise("TF43TR") shouldBe "TF4 3TR"
      PostCode.normalise("W1A0AX") shouldBe "W1A 0AX"
      // extra spaces
      PostCode.normalise("G1  1XQ") shouldBe "G1 1XQ"
      PostCode.normalise("W12  7FW") shouldBe "W12 7FW"
      PostCode.normalise("SW1A  2AA") shouldBe "SW1A 2AA"
    }

    "uppercase valid but non-standard postcodes" in {
      PostCode.normalise("g1 1xq") shouldBe "G1 1XQ"
      PostCode.normalise("w12 7fw") shouldBe "W12 7FW"
      PostCode.normalise("sw1a 2aa") shouldBe "SW1A 2AA"
      PostCode.normalise("tf4 3tr") shouldBe "TF4 3TR"
      PostCode.normalise("Tf43tR") shouldBe "TF4 3TR"
      PostCode.normalise("w1a 0ax") shouldBe "W1A 0AX"
    }
  }

}
