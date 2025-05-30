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
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.telephoneNumberForm

class UpdatePhoneFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val phoneField = "telephoneNumber"
  val validPhone = "01903 209919"
  val validInternationalPhone = "+33 (0) 2345 6789"

  "telephoneNumber form binding" should {
    "be successful when not empty" in {
      val params = Map(
        phoneField -> validPhone
      )

      telephoneNumberForm.bind(params).value shouldBe
        Some(validPhone)

    }

    "be successful when telephone number is international" in {
      val params = Map(
        phoneField -> validInternationalPhone
      )

      telephoneNumberForm.bind(params).value shouldBe
        Some(validInternationalPhone)

    }

    s"error when $phoneField is empty" in {
      val params = Map(
        phoneField -> ""
      )
      val validatedForm = telephoneNumberForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.phone.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField is too long" in {
      val params = Map(
        phoneField -> "+44 32456 78989 634567899887777"
      )
      val validatedForm = telephoneNumberForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.phone.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField has non numeric characters" in {
      val params = Map(
        phoneField -> RandomStringUtils.insecure().nextAlphanumeric(11)
      )
      val validatedForm = telephoneNumberForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.phone.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind" in {
      val unboundForm = telephoneNumberForm.mapping.unbind(
        validPhone
      )

      unboundForm shouldBe Map(
        phoneField -> validPhone
      )
    }
  }

}
