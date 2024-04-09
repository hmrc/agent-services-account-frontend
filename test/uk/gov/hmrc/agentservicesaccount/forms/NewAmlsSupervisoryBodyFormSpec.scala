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

class NewAmlsSupervisoryBodyFormSpec extends AnyWordSpec with Matchers {

  val amlsBodies = Map("ACCA" -> "Association of Certified Chartered Accountants")

  "NewAmlsSupervisoryBodyForm binding" should {
    "succeed when valid data provided" in {
      val data = Map(
        "body" -> "ACCA"
      )

      NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = true).bind(data).value shouldBe Some("ACCA")
    }

    "generate error when nothing entered for UK agent" in {
      val data = Map(
        "body" -> ""
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = true).bind(data)

      result.value shouldBe None
      result.errors.size shouldBe 1
      result.errors.head.message shouldBe "amls.new-supervisory-body.error"
    }

    "generate error when invalid entry for UK agent" in {
      val data = Map(
        "body" -> "%^$"
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = true).bind(data)

      result.value shouldBe None
      result.errors.size shouldBe 1
      result.errors.head.message shouldBe "amls.new-supervisory-body.error"
    }

    "accept for overseas agent" in {
      val data = Map(
        "body" -> "OS AMLS"
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = false).bind(data)

      result.value shouldBe Some("OS AMLS")
    }

    "generate error when nothing entered for overseas agent" in {
      val data = Map(
        "body" -> ""
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = false).bind(data)

      result.value shouldBe None
      result.errors.size shouldBe 1
      result.errors.head.message shouldBe "amls.new-supervisory-body.error"
    }

    "generate error when invalid characters are entered for overseas agent" in {
      val data = Map(
        "body" -> "&%$"
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = false).bind(data)

      result.value shouldBe None
      result.errors.size shouldBe 1
      result.errors.head.message shouldBe "amls.new-supervisory-body.error.os.regex"
    }

    "generate error when too many characters are entered for overseas agent" in {
      val data = Map(
        "body" -> new scala.util.Random().alphanumeric.take(101).mkString
      )

      val result = NewAmlsSupervisoryBodyForm.form(amlsBodies)(isUk = false).bind(data)

      result.value shouldBe None
      result.errors.size shouldBe 1
      result.errors.head.message shouldBe "amls.new-supervisory-body.error.os.max-length"
    }
  }
}
