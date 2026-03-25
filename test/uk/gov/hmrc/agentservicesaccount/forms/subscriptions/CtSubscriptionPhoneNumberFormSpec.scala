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

package uk.gov.hmrc.agentservicesaccount.forms.subscriptions

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionPhoneNumberForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtPhoneNumberFormValues

import scala.util.Random

class CtSubscriptionPhoneNumberFormSpec
extends AnyWordSpec
with Matchers {

  private val emptyValue = ""
  private val validNewPhoneNumber = "1234567890"
  private val invalidNewPhoneNumber = "skdjfhjs"

  "form binding" should {
    s"be successful when $phoneNumberUseAsaDataKey true" in {
      val phoneNumberValues = List(
        emptyValue,
        validNewPhoneNumber,
        invalidNewPhoneNumber
      )
      val params = Map(
        phoneNumberUseAsaDataKey -> true.toString,
        phoneNumberNewKey -> phoneNumberValues(Random.nextInt(phoneNumberValues.length))
      )

      form.bind(params).value shouldBe Some(CtPhoneNumberFormValues(useAsaData = true, None))
    }

    s"be successful when $phoneNumberUseAsaDataKey false and $phoneNumberNewKey valid" in {
      val params = Map(
        phoneNumberUseAsaDataKey -> false.toString,
        phoneNumberNewKey -> validNewPhoneNumber
      )

      form.bind(params).value shouldBe Some(CtPhoneNumberFormValues(useAsaData = false, Some(validNewPhoneNumber)))
    }

    s"error when $phoneNumberUseAsaDataKey empty" in {
      val phoneNumberValues = List(
        emptyValue,
        validNewPhoneNumber,
        invalidNewPhoneNumber
      )
      val params = Map(
        phoneNumberUseAsaDataKey -> emptyValue,
        phoneNumberNewKey -> phoneNumberValues(Random.nextInt(phoneNumberValues.length))
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneNumberUseAsaDataKey).get.message shouldBe "asa.legacy.ct.phone-number.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneNumberUseAsaDataKey false and $phoneNumberNewKey empty" in {
      val params = Map(
        phoneNumberUseAsaDataKey -> false.toString,
        phoneNumberNewKey -> emptyValue
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneNumberNewKey).get.message shouldBe "asa.legacy.ct.phone-number.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $phoneNumberUseAsaDataKey false and $phoneNumberNewKey invalid" in {
      val params = Map(
        phoneNumberUseAsaDataKey -> false.toString,
        phoneNumberNewKey -> invalidNewPhoneNumber
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(phoneNumberNewKey).get.message shouldBe "asa.legacy.ct.phone-number.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtPhoneNumberFormValues" in {
      val unboundForm = form.mapping.unbind(CtPhoneNumberFormValues(useAsaData = true, Some(validNewPhoneNumber)))

      unboundForm shouldBe Map(
        phoneNumberUseAsaDataKey -> true.toString,
        phoneNumberNewKey -> validNewPhoneNumber
      )
    }

  }

}
