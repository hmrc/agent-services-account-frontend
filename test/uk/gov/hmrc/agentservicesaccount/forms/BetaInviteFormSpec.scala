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

class BetaInviteFormSpec
extends AnyWordSpec
with Matchers {

  val fieldName = "size"

  "BetaInviteForm bound value for 'size' field name" should {

    // update string to trait?
    "be small when field value is 'small' " in {
      val params = Map(fieldName -> "small")
      BetaInviteForm.form.bind(params).value shouldBe Some("small")

    }

    "be medium when field value is 'medium' " in {
      val params = Map(fieldName -> "medium")
      BetaInviteForm.form.bind(params).value shouldBe Some("medium")
    }

    "be large when field value is 'large' " in {
      val params = Map(fieldName -> "large")
      BetaInviteForm.form.bind(params).value shouldBe Some("large")
    }

    "Be invalid with provided error message key when 'size' field not present in params" in {
      val params: Map[String, String] = Map.empty
      val validatedForm = BetaInviteForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(fieldName).get.message shouldBe "beta.invite.size.required.error"
    }

    "unbind" in {
      BetaInviteForm.form.mapping.unbind("small") shouldBe Map(
        "size" -> "small"
      )
    }
  }

}
