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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionAddressForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtAddressFormValues

import scala.util.Random

class CtSubscriptionAddressFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewAddress = "joe@bloggs.com"
  val invalidNewAddress = "{][.',"

  "form binding" should {
    s"be successful when $addressUseAsaDataKey true" in {
      val addressValues = List(
        emptyValue,
        validNewAddress,
        invalidNewAddress
      )
      val params = Map(
        addressUseAsaDataKey -> true.toString,
        addressNewKey -> addressValues(Random.nextInt(addressValues.length))
      )

      form.bind(params).value shouldBe Some(CtAddressFormValues(useAsaData = true, None))
    }

    s"be successful when $addressUseAsaDataKey false and $addressNewKey valid" in {
      val params = Map(
        addressUseAsaDataKey -> false.toString,
        addressNewKey -> validNewAddress
      )

      form.bind(params).value shouldBe Some(CtAddressFormValues(useAsaData = false, Some(validNewAddress)))
    }

    s"error when $addressUseAsaDataKey empty" in {
      val addressValues = List(
        emptyValue,
        validNewAddress,
        invalidNewAddress
      )
      val params = Map(
        addressUseAsaDataKey -> emptyValue,
        addressNewKey -> addressValues(Random.nextInt(addressValues.length))
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(addressUseAsaDataKey).get.message shouldBe "asa.legacy.ct.email-address.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $addressUseAsaDataKey false and $addressNewKey empty" in {
      val params = Map(
        addressUseAsaDataKey -> false.toString,
        addressNewKey -> emptyValue
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(addressNewKey).get.message shouldBe "asa.legacy.ct.email-address.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $addressUseAsaDataKey false and $addressNewKey invalid" in {
      val params = Map(
        addressUseAsaDataKey -> false.toString,
        addressNewKey -> invalidNewAddress
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(addressNewKey).get.message shouldBe "asa.legacy.ct.email-address.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtAddressFormValues" in {
      val unboundForm = form.mapping.unbind(CtAddressFormValues(useAsaData = true, Some(validNewAddress)))

      unboundForm shouldBe Map(
        addressUseAsaDataKey -> true.toString,
        addressNewKey -> validNewAddress
      )
    }

  }

}
