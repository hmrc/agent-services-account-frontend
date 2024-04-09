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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class YesNoFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val fieldName = "accept"

  "YesNoForm bound value for 'accept' field name" should {
    "be true field value is 'true' " in {
      val params = Map(fieldName -> "true")
      YesNoForm.form().bind(params).value shouldBe Some(true)
      YesNoForm.form().bind(params).value.get shouldBe true

    }
    "be false when field value is 'false' " in {
      val params = Map(fieldName -> "false")
      YesNoForm.form().bind(params).value.get shouldBe false
    }
    "be None when field value is not boolean " in {
      val params = Map(fieldName -> "not convertible to boolean")
      YesNoForm.form().bind(params).value shouldBe None
    }

    "Be invalid with provided error message key when 'accept' field name not present in params" in {
      val params: Map[String, String] = Map.empty
      val validatedForm = YesNoForm.form("my.error.key").bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(fieldName).get.message shouldBe "my.error.key"
    }

    "unbind" in {
      YesNoForm.form("my.error.key").mapping.unbind(true) shouldBe Map(
        fieldName -> "true")
    }
  }
}
