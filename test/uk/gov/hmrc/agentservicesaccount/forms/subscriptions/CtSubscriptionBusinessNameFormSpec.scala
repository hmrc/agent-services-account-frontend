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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionBusinessNameForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtBusinessNameFormValues

import scala.util.Random

class CtSubscriptionBusinessNameFormSpec
extends AnyWordSpec
with Matchers {

  val emptyValue = ""
  val validNewBusinessName = "ABC-No.1 Accountants"
  val invalidNewBusinessName = "{][.',"

  "newBusinessNameForm form binding" should {
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

      form.bind(params).value shouldBe Some(CtBusinessNameFormValues(useAsaData = true, None))
    }

    s"be successful when $businessNameUseAsaDataKey false and $businessNameNewKey valid" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> validNewBusinessName
      )

      form.bind(params).value shouldBe Some(CtBusinessNameFormValues(useAsaData = false, Some(validNewBusinessName)))
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

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameUseAsaDataKey).get.message shouldBe "asa.legacy.ct.business-name.use-asa.error.required"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $businessNameUseAsaDataKey false and $businessNameNewKey empty" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> emptyValue
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameNewKey).get.message shouldBe "asa.legacy.ct.business-name.new-input.error.empty"
      validatedForm.errors.length shouldBe 1
    }

    s"error when $businessNameUseAsaDataKey false and $businessNameNewKey invalid" in {
      val params = Map(
        businessNameUseAsaDataKey -> false.toString,
        businessNameNewKey -> invalidNewBusinessName
      )

      val validatedForm = form.bind(params)
      validatedForm.hasErrors shouldBe true
      validatedForm.error(businessNameNewKey).get.message shouldBe "asa.legacy.ct.business-name.new-input.error.invalid"
      validatedForm.errors.length shouldBe 1
    }

    "unbind CtBusinessNameFormValues" in {
      val unboundForm = form.mapping.unbind(CtBusinessNameFormValues(useAsaData = true, Some(validNewBusinessName)))

      unboundForm shouldBe Map(
        businessNameUseAsaDataKey -> true.toString,
        businessNameNewKey -> validNewBusinessName
      )
    }

  }

}
