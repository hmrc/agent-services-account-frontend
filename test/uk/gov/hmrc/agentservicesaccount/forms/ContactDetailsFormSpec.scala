/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.commons.lang3.RandomStringUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.agentservicesaccount.models.{BetaInviteContactDetails, SuspendContactDetails}

class ContactDetailsFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val nameField = "name"
  val emailField = "email"
  val phoneField = "phone"
  val utrField = "utr"

  "BetaInviteContactDetailsForm binding" should {
    "be successful when not empty (no phone)" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> ""
      )

      BetaInviteContactDetailsForm.form.bind(params).value shouldBe
        Some(BetaInviteContactDetails("Blah alkfh","asdlkj@eqkf.do",None))

    }

    "be successful when not empty (with phone)" in {
        val params = Map(
          nameField -> "Blah alkfh",
          emailField -> "asdlkj@eqkf.do",
          phoneField -> "32456 789896"
        )

        BetaInviteContactDetailsForm.form.bind(params).value shouldBe
          Some(BetaInviteContactDetails("Blah alkfh","asdlkj@eqkf.do",Some("32456 789896")))
    }

    s"error when $nameField and $emailField are empty" in {
      val params = Map(
        nameField -> "",
        emailField -> "",
        phoneField -> ""
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "error.required.name"
      validatedForm.error(emailField).get.message shouldBe "error.required.email"
      validatedForm.errors.length shouldBe 2
    }

    s"error when $nameField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(81),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> ""
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "error.max-length.name"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(80),
        // total 255 characters
        emailField -> RandomStringUtils.randomAlphanumeric(251).concat("@a.a"),
        phoneField -> ""
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailField).get.message shouldBe "error.max-length.email"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailField does not have @ symbol" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "not an email",
        phoneField -> "1 2 3 4 5 6 7 8 9 0 "
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailField).get.message shouldBe "error.invalid.email"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(79),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> "+44 32456 78989 6345678"
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "error.max-length.phone"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField has non numeric characters" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(79),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> RandomStringUtils.randomAlphanumeric(11)
      )
      val validatedForm = BetaInviteContactDetailsForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "error.invalid.phone"
      validatedForm.errors.length shouldBe 1
    }

    "unbind" in {
      val unboundForm = BetaInviteContactDetailsForm.form.mapping.unbind(BetaInviteContactDetails(
        "Blah alkfh",
        "asdlkj@eqkf.do",
        None
      ))

      unboundForm shouldBe Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do"
      )
    }
  }

  "ContactDetailsSuspendedForm binding" should {
    "be successful when not empty (no utr)" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> "01273 000 000"
      )

      ContactDetailsSuspendForm.form.bind(params).value shouldBe
        Some(SuspendContactDetails("Blah alkfh", "asdlkj@eqkf.do","01273 000 000", None))

    }

    "be successful when not empty (with utr)" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> "32456 789896",
        utrField -> "1234567890"
      )

      ContactDetailsSuspendForm.form.bind(params).value shouldBe
        Some(SuspendContactDetails("Blah alkfh", "asdlkj@eqkf.do", "32456 789896", Some("1234567890")))
    }

    s"error when $nameField, $emailField and $phoneField are empty" in {
      val params = Map(
        nameField -> "",
        emailField -> "",
        phoneField -> ""
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "error.suspended-details.required.name"
      validatedForm.error(emailField).get.message shouldBe "error.suspended-details.required.email"
      validatedForm.error(phoneField).get.message shouldBe "error.suspended-details.required.telephone"
      validatedForm.errors.length shouldBe 3
    }

    s"error when $nameField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(81),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(20)
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "error.max-length.name"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $nameField contains '<' or '>' " in {
      val params = Map(
        nameField -> "<somebody>",
        emailField -> RandomStringUtils.randomAlphanumeric(25).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(20)
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "error.suspended-details.invalid-chars.name"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(80),
        // total 255 characters
        emailField -> RandomStringUtils.randomAlphanumeric(251).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(20)
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailField).get.message shouldBe "error.max-length.email"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailField does not have @ symbol" in {
      val params = Map(
        nameField -> "Blah alkfh",
        emailField -> "not an email",
        phoneField -> "1 2 3 4 5 6 7 8 9 0 "
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailField).get.message shouldBe "error.suspended-details.required.email"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(79),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> "+44 32456 78989 6345678"
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "error.max-length.telephone"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField has non numeric characters" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(79),
        emailField -> RandomStringUtils.randomAlphanumeric(250).concat("@a.a"),
        phoneField -> RandomStringUtils.randomAlphanumeric(11)
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "error.suspended-details.invalid.telephone"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $utrField is less than 10 characters" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(9),
        emailField -> RandomStringUtils.randomAlphanumeric(25).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(11),
        utrField -> "12312"
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(utrField).get.message shouldBe "error.suspended-details.max-length.utr"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $utrField is more than 14 characters" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(9),
        emailField -> RandomStringUtils.randomAlphanumeric(25).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(11),
        utrField -> "123126565656567"
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(utrField).get.message shouldBe "error.suspended-details.max-length.utr"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $utrField has invalid characters" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(9),
        emailField -> RandomStringUtils.randomAlphanumeric(25).concat("@a.a"),
        phoneField -> RandomStringUtils.randomNumeric(11),
        utrField -> "1234567890M"
      )
      val validatedForm = ContactDetailsSuspendForm.form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(utrField).get.message shouldBe "error.suspended-details.invalid.utr"
      validatedForm.errors.length shouldBe 1
    }

    "unbind without utr" in {
      val unboundForm = ContactDetailsSuspendForm.form.mapping.unbind(SuspendContactDetails(
        "Blah alkfh",
        "asdlkj@eqkf.do",
        "123456",
        None
      ))

      unboundForm shouldBe Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> "123456"
      )
    }

    "unbind with utr" in {
      val unboundForm = ContactDetailsSuspendForm.form.mapping.unbind(SuspendContactDetails(
        "Blah alkfh",
        "asdlkj@eqkf.do",
        "123456",
        Some("K1234567890")
      ))

      unboundForm shouldBe Map(
        nameField -> "Blah alkfh",
        emailField -> "asdlkj@eqkf.do",
        phoneField -> "123456",
        utrField -> "K1234567890"
      )
    }
  }

}
