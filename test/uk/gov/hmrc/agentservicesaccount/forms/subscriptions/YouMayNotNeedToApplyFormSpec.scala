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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.YouMayNotNeedToApplyForm._
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.YouMayNotNeedToApplyFormValues

class YouMayNotNeedToApplyFormSpec
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

    s"YouMayNotNeedToApplyForm for regime $regime" should {

      "bind successfully when true" in {
        val params = Map(doYouStillWantToApplyKey -> "true")

        initForm.bind(params).value shouldBe Some(
          YouMayNotNeedToApplyFormValues(true)
        )
      }

      "bind successfully when false" in {
        val params = Map(doYouStillWantToApplyKey -> "false")

        initForm.bind(params).value shouldBe Some(
          YouMayNotNeedToApplyFormValues(false)
        )
      }

      "return error when empty" in {
        val params = Map(doYouStillWantToApplyKey -> "")

        val result = initForm.bind(params)

        result.hasErrors shouldBe true
        result.error(doYouStillWantToApplyKey).get.message shouldBe
          s"$prefix.you-may-not-need-to-apply.error.required"
      }

      "return error when missing" in {
        val result = initForm.bind(Map.empty[String, String])

        result.hasErrors shouldBe true
        result.error(doYouStillWantToApplyKey).get.message shouldBe
          s"$prefix.you-may-not-need-to-apply.error.required"
      }

      "unbind correctly" in {
        val unbound = initForm.mapping.unbind(
          YouMayNotNeedToApplyFormValues(true)
        )

        unbound shouldBe Map(
          doYouStillWantToApplyKey -> "true"
        )
      }
    }
  }

}
