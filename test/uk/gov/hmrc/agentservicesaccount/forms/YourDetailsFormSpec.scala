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
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.yourDetailsForm
import uk.gov.hmrc.agentservicesaccount.models.YourDetails

class YourDetailsFormSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  val nameField = "fullName"
  val phoneField = "telephone"
  val validName = "John Tester"
  val validPhone = "01903 209919"
  val validInternationalPhone = "+33 (0) 2345 6789"

  "YourDetails form binding" should {
    "be successful when not empty" in {
      val params = Map(
        nameField -> validName,
        phoneField -> validPhone
      )

      yourDetailsForm.bind(params).value shouldBe
        Some(YourDetails(validName, validPhone))

    }

    "be successful when telephone number is international" in {
      val params = Map(
        nameField -> validName,
        phoneField -> validInternationalPhone
      )

      yourDetailsForm.bind(params).value shouldBe
        Some(YourDetails(validName, validInternationalPhone))

    }

    s"error when $nameField and $phoneField are empty" in {
      val params = Map(
        nameField -> "",
        phoneField -> ""
      )
      val validatedForm = yourDetailsForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "update-contact-details.your-details.name.error.empty"
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.your-details.telephone.error.empty"
      validatedForm.errors.length shouldBe 2
    }

    s"error when $nameField is too long" in {
      val params = Map(
        nameField -> RandomStringUtils.randomAlphanumeric(41),
        phoneField -> validPhone
      )
      val validatedForm = yourDetailsForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(nameField).get.message shouldBe "update-contact-details.your-details.name.error.tooLong"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField is too long" in {
      val params = Map(
        nameField -> validName,
        phoneField -> "+44 32456 78989 634567899887777"
      )
      val validatedForm = yourDetailsForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.your-details.telephone.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneField has non numeric characters" in {
      val params = Map(
        nameField -> validName,
        phoneField -> RandomStringUtils.randomAlphanumeric(11)
      )
      val validatedForm = yourDetailsForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneField).get.message shouldBe "update-contact-details.your-details.telephone.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind" in {
      val unboundForm = yourDetailsForm.mapping.unbind(YourDetails(
        validName,
        validPhone
      ))

      unboundForm shouldBe Map(
        nameField -> validName,
        phoneField -> validPhone
      )
    }
  }

}
