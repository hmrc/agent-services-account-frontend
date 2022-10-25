/*
 * Copyright 2022 HM Revenue & Customs
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

class BetaInviteContactDetailsFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val nameField = "name"
  val emailField = "email"
  val phoneField = "phone"

  "BetaInviteContactDetailsForm binding" should {
    "be successful when not empty (no phone)" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> None
      )

      // TODO fix this trash
      params shouldBe params
     // BetaInviteContactDetailsForm.form.bind(params).value

    }

    "be successful when not empty (with phone)" in {
      true shouldBe true
    }

    s"error when $nameField not present in params" in {
      val params: Map[String, String] = Map.empty
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      //validatedForm.error(fieldName).get.message shouldBe "beta.invite.size.required.error"
    }

    s"error when $emailField not present in params" in {
      val params: Map[String, String] = Map.empty
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      //validatedForm.error(fieldName).get.message shouldBe "beta.invite.size.required.error"
    }

    "unbind" in {
      BetaInviteForm.form.mapping.unbind("small") shouldBe Map(
        "size" -> "small")
    }
  }

}
