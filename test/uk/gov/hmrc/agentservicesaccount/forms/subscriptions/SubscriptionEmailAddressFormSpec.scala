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
import play.api.i18n.Messages
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.EmailAddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import play.api.test.Helpers
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.CT_SA_EMAIL_MAX_LENGTH

import scala.util.Random

class SubscriptionEmailAddressFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewEmailAddress = "joe@bloggs.com"
  val invalidNewEmailAddress = "{][.',"

  private val legacyRegime = LegacyRegime.CT

  private val legacyRegimePrefix = legacyRegime.msgPrefix

  implicit val messages: Messages = Helpers.stubMessages()
  private val initForm = form(legacyRegime, "Agency Name")

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

      initForm.bind(params).value shouldBe Some(EmailAddressFormValues(useAsaData = true, None))
    }

    s"be successful when $emailAddressUseAsaDataKey false and $emailAddressNewKey valid" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> validNewEmailAddress
      )

      initForm.bind(params).value shouldBe Some(EmailAddressFormValues(useAsaData = false, Some(validNewEmailAddress)))
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

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressUseAsaDataKey).get.message shouldBe s"$legacyRegimePrefix.email-address.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailAddressUseAsaDataKey false and $emailAddressNewKey empty" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> emptyValue
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressNewKey).get.message shouldBe s"$legacyRegimePrefix.email-address.input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailAddressUseAsaDataKey false and $emailAddressNewKey too long" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> s"${(1 to CT_SA_EMAIL_MAX_LENGTH).map("a")}@email.com"
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressNewKey).get.message shouldBe s"$legacyRegimePrefix.email-address.input.error.length"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $emailAddressUseAsaDataKey false and $emailAddressNewKey invalid" in {
      val params = Map(
        emailAddressUseAsaDataKey -> false.toString,
        emailAddressNewKey -> invalidNewEmailAddress
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(emailAddressNewKey).get.message shouldBe s"$legacyRegimePrefix.email-address.input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind EmailAddressFormValues" in {
      val unboundForm = initForm.mapping.unbind(EmailAddressFormValues(useAsaData = true, Some(validNewEmailAddress)))

      unboundForm shouldBe Map(
        emailAddressUseAsaDataKey -> true.toString,
        emailAddressNewKey -> validNewEmailAddress
      )
    }

  }

}
