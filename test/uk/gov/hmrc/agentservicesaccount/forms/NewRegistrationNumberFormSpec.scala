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

package uk.gov.hmrc.agentservicesaccount.forms

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class NewRegistrationNumberFormSpec
extends AnyWordSpec
with Matchers {

  "NewRegistrationNumberForm binding" should {
    "for HMRC AMLS" when {

      "succeed when valid data provided" in {
        val data = Map(
          "number" -> "XAML00000123456"
        )

        val result = NewRegistrationNumberForm.form(isHmrc = true).bind(data)

        result.value shouldBe Some("XAML00000123456")
      }

      "fail when no data provided" in {
        val data = Map(
          "number" -> ""
        )

        val result = NewRegistrationNumberForm.form(isHmrc = true).bind(data)

        result.value shouldBe None
        result.errors.size shouldBe 1
        result.errors.head.message shouldBe "amls.enter-registration-number.error.empty"
      }

      "fail when invalid data provided" in {
        val data = Map(
          "number" -> "GGG12345X"
        )

        val result = NewRegistrationNumberForm.form(isHmrc = true).bind(data)

        result.value shouldBe None
        result.errors.size shouldBe 1
        result.errors.head.message shouldBe "amls.enter-registration-number.error.hmrc.invalid"
      }
    }

    "for not HMRC AMLS" when {
      "succeed when valid data provided" in {
        val data = Map(
          "number" -> "GGG12345X"
        )

        val result = NewRegistrationNumberForm.form(isHmrc = false).bind(data)

        result.value shouldBe Some("GGG12345X")
      }

      "fail when no data provided" in {
        val data = Map(
          "number" -> ""
        )

        val result = NewRegistrationNumberForm.form(isHmrc = false).bind(data)

        result.value shouldBe None
        result.errors.size shouldBe 1
        result.errors.head.message shouldBe "amls.enter-registration-number.error.empty"
      }

      "fail when invalid data provided" in {
        val data = Map(
          "number" -> "%$%1234FF"
        )

        val result = NewRegistrationNumberForm.form(isHmrc = false).bind(data)

        result.value shouldBe None
        result.errors.size shouldBe 1
        result.errors.head.message shouldBe "amls.enter-registration-number.error.not-hmrc.invalid"
      }
    }
  }
}
