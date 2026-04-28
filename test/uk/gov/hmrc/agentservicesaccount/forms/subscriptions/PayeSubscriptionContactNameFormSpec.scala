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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.PayeSubscriptionContactNameForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.PayeContactNameFormValues

class PayeSubscriptionContactNameFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewContactName = "My Name"
  val invalidNewContactName = "{][.',"

  private val initForm = form

  "form binding" should {
    s"be successful when $contactNameKey valid" in {
      val params = Map(
        contactNameKey -> validNewContactName
      )

      initForm.bind(params).value shouldBe Some(PayeContactNameFormValues(validNewContactName))
    }

    s"error when $contactNameKey empty" in {
      val params = Map(
        contactNameKey -> emptyValue
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(contactNameKey).get.message shouldBe "asa.legacy.paye.contact-name.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $contactNameKey invalid" in {
      val params = Map(
        contactNameKey -> invalidNewContactName
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(contactNameKey).get.message shouldBe "asa.legacy.paye.contact-name.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind PayeContactNameFormValues" in {
      val unboundForm = initForm.mapping.unbind(PayeContactNameFormValues(validNewContactName))

      unboundForm shouldBe Map(
        contactNameKey -> validNewContactName
      )
    }

  }

}
