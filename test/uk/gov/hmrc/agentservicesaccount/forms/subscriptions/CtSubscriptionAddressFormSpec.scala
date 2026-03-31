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

class CtSubscriptionAddressFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""

  "form binding" should {
    s"be successful when $addressUseAsaDataKey true" in {
      val params = Map(
        addressUseAsaDataKey -> true.toString
      )

      form.bind(params).value shouldBe Some(CtAddressFormValues(useAsaData = true))
    }

    s"be successful when $addressUseAsaDataKey false" in {
      val params = Map(
        addressUseAsaDataKey -> false.toString
      )

      form.bind(params).value shouldBe Some(CtAddressFormValues(useAsaData = false))
    }

    s"error when $addressUseAsaDataKey empty" in {
      val params = Map(
        addressUseAsaDataKey -> emptyValue
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(addressUseAsaDataKey).get.message shouldBe "asa.legacy.ct.address.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtAddressFormValues" in {
      val unboundForm = form.mapping.unbind(CtAddressFormValues(useAsaData = true))

      unboundForm shouldBe Map(
        addressUseAsaDataKey -> true.toString
      )
    }

  }

}
