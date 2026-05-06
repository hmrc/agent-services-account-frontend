/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.DoYouAlreadyManageFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

class DoYouAlreadyManageFormSpec
extends AnyWordSpec
with Matchers {

  private val regimes = Seq(
    LegacyRegime.SA,
    LegacyRegime.CT,
    LegacyRegime.PAYE
  )

  regimes.foreach { regime =>
    val prefix = regime.msgPrefix
    val initForm = form(regime)

    s"DoYouAlreadyManageForm for regime $regime" should {

      "bind successfully when true" in {
        val params = Map(
          doYouAlreadyManageKey -> "true"
        )

        initForm.bind(params).value shouldBe Some(
          DoYouAlreadyManageFormValues(true)
        )
      }

      "bind successfully when false" in {
        val params = Map(
          doYouAlreadyManageKey -> "false"
        )

        initForm.bind(params).value shouldBe Some(
          DoYouAlreadyManageFormValues(false)
        )
      }

      "return error when value is empty" in {
        val params = Map(
          doYouAlreadyManageKey -> ""
        )

        val validatedForm = initForm.bind(params)

        validatedForm.hasErrors shouldBe true
        validatedForm.error(doYouAlreadyManageKey).get.message shouldBe
          s"$prefix.do-you-already-manage.error.required"
        validatedForm.errors.length shouldBe 1
      }

      "return error when value is not provided at all" in {
        val params = Map.empty[String, String]

        val validatedForm = initForm.bind(params)

        validatedForm.hasErrors shouldBe true
        validatedForm.error(doYouAlreadyManageKey).get.message shouldBe
          s"$prefix.do-you-already-manage.error.required"
        validatedForm.errors.length shouldBe 1
      }

      "unbind correctly" in {
        val unboundForm = initForm.mapping.unbind(
          DoYouAlreadyManageFormValues(true)
        )

        unboundForm shouldBe Map(
          doYouAlreadyManageKey -> "true"
        )
      }
    }
  }

}
