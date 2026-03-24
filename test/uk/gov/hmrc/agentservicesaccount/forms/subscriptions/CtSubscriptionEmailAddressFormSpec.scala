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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionEmailAddressForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtEmailAddressFormValues

import scala.util.Random

class CtSubscriptionEmailAddressFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewEmailAddress = "joe@bloggs.com"
  val invalidNewEmailAddress = "{][.',"

  "form binding" should {
    s"be successful when $emailAddressUseAsaDataKey true" in {
      val emailAddressValues = List(
        emptyValue,
        validNewEmailAddress,
        invalidNewEmailAddress
      )
      val params = Map(
        emailAddressUseAsaDataKey -> true.toString,
        emailAddressNewKey -> emailAddressValues(Random.nextInt(emailAddressValues.length))
      )

      form.bind(params).value shouldBe Some(CtEmailAddressFormValues(useAsaData = true, None))
    }

    s"be successful when $emailAddressUseAsaDataKey false and $emailAddressNewKey valid" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> validNewEmailAddress
      )

      form.bind(params).value shouldBe Some(CtEmailAddressFormValues(useAsaData = false, Some(validNewEmailAddress)))
    }

    s"error when $emailAddressUseAsaDataKey empty" in {
      val emailAddressValues = List(
        emptyValue,
        validNewEmailAddress,
        invalidNewEmailAddress
      )
      val params = Map(
        emailAddressUseAsaDataKey -> emptyValue,
        emailAddressNewKey -> emailAddressValues(Random.nextInt(emailAddressValues.length))
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressUseAsaDataKey).get.message shouldBe "asa.legacy.ct.email-address.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailAddressUseAsaDataKey false and $emailAddressNewKey empty" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> emptyValue
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressNewKey).get.message shouldBe "asa.legacy.ct.email-address.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailAddressUseAsaDataKey false and $emailAddressNewKey invalid" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> invalidNewEmailAddress
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressNewKey).get.message shouldBe "asa.legacy.ct.email-address.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtEmailAddressFormValues" in {
      val unboundForm = form.mapping.unbind(CtEmailAddressFormValues(useAsaData = true, Some(validNewEmailAddress)))

      unboundForm shouldBe Map(
        emailAddressUseAsaDataKey -> true.toString,
        emailAddressNewKey -> validNewEmailAddress
      )
    }

  }

}
