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

package uk.gov.hmrc.agentservicesaccount.forms

import org.apache.commons.lang3.RandomStringUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.telephoneNumberForm
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.businessNameForm
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.emailAddressForm

class UpdateDetailsFormsSpec
extends AnyWordSpec
with Matchers {

  "Business Name form" should {

    val businessField = "name"

    val valid = Table(
      ("Description", "Input"),
      (s"valid $businessField", "Test Business Ltd"),
      (s"valid $businessField with permitted special characters", "Test , . ' - / Business Ltd"),
      (s"valid shortest length possible $businessField", "A"),
      (s"valid longest length possible $businessField", "XXXXXX Business Has 40 Chars Ltd XXXXXXX")
    )

    forAll(valid) {
      (
        description,
        input
      ) =>
        s"be successful when $description" in {
          val params = Map(
            businessField -> input
          )
          businessNameForm.bind(params).value shouldBe Some(input)
          businessNameForm.errors.length shouldBe 0
        }
    }

    val invalid = Table(
      ("Description", "Input", "Error Message"),
      (s"empty $businessField", "", "update-contact-details.name.error.empty"),
      (s"$businessField too long", "XXXXXXX Business Has 41 Chars Ltd XXXXXXX", "update-contact-details.name.error.invalid"),
      (s"$businessField has non-permitted special characters", "Business $ % ^ Ltd", "update-contact-details.name.error.invalid")
    )

    forAll(invalid) {
      (
        description,
        input,
        errorMessage
      ) =>
        s"error when $description" in {
          val params = Map(
            businessField -> input
          )
          val validatedForm = businessNameForm.bind(params)
          validatedForm.hasErrors shouldBe true
          validatedForm.error(businessField).get.message shouldBe errorMessage
          validatedForm.errors.length shouldBe 1
        }
    }

    "unbind" in {
      val validBusiness = "Test Business Ltd"
      val unboundForm = businessNameForm.mapping.unbind(
        validBusiness
      )
      unboundForm shouldBe Map(
        businessField -> validBusiness
      )
    }
  }

  "Telephone Number form" should {

    val phoneField = "telephoneNumber"

    val valid = Table(
      ("Description", "Input"),
      (s"valid $phoneField", "01903 209919"),
      (s"valid international $phoneField", "+33 (0) 2345 6789")
    )

    forAll(valid) {
      (
        description,
        input
      ) =>
        s"be successful when $description" in {
          val params = Map(
            phoneField -> input
          )
          telephoneNumberForm.bind(params).value shouldBe Some(input)
          telephoneNumberForm.errors.length shouldBe 0
        }
    }

    val invalid = Table(
      ("Description", "Input", "Error Message"),
      (s"empty $phoneField", "", "update-contact-details.phone.error.empty"),
      (s"$phoneField too long", "+442502502502502502502500", "update-contact-details.phone.error.invalid"),
      (s"$phoneField has non-numeric characters", RandomStringUtils.insecure().nextAlphanumeric(11), "update-contact-details.phone.error.invalid")
    )

    forAll(invalid) {
      (
        description,
        input,
        errorMessage
      ) =>
        s"error when $description" in {
          val params = Map(
            phoneField -> input
          )
          val validatedForm = telephoneNumberForm.bind(params)
          validatedForm.hasErrors shouldBe true
          validatedForm.error(phoneField).get.message shouldBe errorMessage
          validatedForm.errors.length shouldBe 1
        }
    }

    "unbind" in {
      val validPhone = "01903 209919"
      val unboundForm = telephoneNumberForm.mapping.unbind(
        validPhone
      )
      unboundForm shouldBe Map(
        phoneField -> validPhone
      )
    }
  }

  "Email address form" should {

    val emailField = "emailAddress"

    val valid = Table(
      ("Description", "Input"),
      (s"valid $emailField", "test@test.com"),
      (s"valid shortest possible $emailField", "x@x.x"),
      (s"valid longest possible $emailField", s"${"x" * 100}@${"y" * 27}.com")
    )

    forAll(valid) {
      (
        description,
        input
      ) =>
        s"be successful when $description" in {
          val params = Map(
            emailField -> input
          )
          emailAddressForm.bind(params).value shouldBe Some(input)
          emailAddressForm.errors.length shouldBe 0
        }
    }

    val invalid = Table(
      ("Description", "Input", "Error Message"),
      (s"empty $emailField", "", "update-contact-details.email.error.empty"),
      (s"$emailField too long", s"${"x" * 100}@${"y" * 28}.com", "update-contact-details.email.error.invalid")
    )

    forAll(invalid) {
      (
        description,
        input,
        errorMessage
      ) =>
        s"error when $description" in {
          val params = Map(
            emailField -> input
          )
          val validatedForm = emailAddressForm.bind(params)
          validatedForm.hasErrors shouldBe true
          validatedForm.error(emailField).get.message shouldBe errorMessage
          validatedForm.errors.length shouldBe 1
        }
    }

    "unbind" in {
      val validEmail = "test@test.com"
      val unboundForm = emailAddressForm.mapping.unbind(
        validEmail
      )
      unboundForm shouldBe Map(
        emailField -> validEmail
      )
    }
  }

}
