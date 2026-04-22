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
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.BusinessNameFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

import scala.util.Random

//TODO: 11186 Need to correct this code
class PayeSubscriptionContactNameFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewBusinessName = "ABC-No.1 Accountants"
  val invalidNewBusinessName = "{][.',"

  private val legacyRegime = LegacyRegime.PAYE

  private val legacyRegimePrefix = legacyRegime.msgPrefix

  private val initForm = form(legacyRegime)

  "form binding" should {
    s"be successful when $businessNameUseAsaDataKey true" in {
      val businessNameValues = List(
        emptyValue,
        validNewBusinessName,
        invalidNewBusinessName
      )
      val params = Map(
        businessNameUseAsaDataKey -> true.toString,
        businessNameNewKey -> businessNameValues(Random.nextInt(businessNameValues.length))
      )

      initForm.bind(params).value shouldBe Some(BusinessNameFormValues(useAsaData = true, None))
    }

    s"be successful when $businessNameUseAsaDataKey false and $businessNameNewKey valid" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> validNewBusinessName
      )

      initForm.bind(params).value shouldBe Some(BusinessNameFormValues(useAsaData = false, Some(validNewBusinessName)))
    }

    s"error when $businessNameUseAsaDataKey empty" in {
      val businessNameValues = List(
        emptyValue,
        validNewBusinessName,
        invalidNewBusinessName
      )
      val params = Map(
        businessNameUseAsaDataKey -> emptyValue,
        businessNameNewKey -> businessNameValues(Random.nextInt(businessNameValues.length))
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameUseAsaDataKey).get.message shouldBe s"$legacyRegimePrefix.business-name.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $businessNameUseAsaDataKey false and $businessNameNewKey empty" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> emptyValue
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameNewKey).get.message shouldBe s"$legacyRegimePrefix.business-name.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $businessNameUseAsaDataKey false and $businessNameNewKey invalid" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> invalidNewBusinessName
      )

      val validatedForm = initForm.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameNewKey).get.message shouldBe s"$legacyRegimePrefix.business-name.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtBusinessNameFormValues" in {
      val unboundForm = initForm.mapping.unbind(BusinessNameFormValues(useAsaData = true, Some(validNewBusinessName)))

      unboundForm shouldBe Map(
        businessNameUseAsaDataKey -> true.toString,
        businessNameNewKey -> validNewBusinessName
      )
    }

  }

}
