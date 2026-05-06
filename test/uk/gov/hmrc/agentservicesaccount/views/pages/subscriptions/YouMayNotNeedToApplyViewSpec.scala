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

package uk.gov.hmrc.agentservicesaccount.views.pages.subscriptions

import org.jsoup.Jsoup
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.YouMayNotNeedToApplyForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.YouMayNotNeedToApplyFormValues
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.you_may_not_need_to_apply

class YouMayNotNeedToApplyViewSpec
extends ViewBaseSpec
with Matchers {

  val view: you_may_not_need_to_apply = app.injector.instanceOf[you_may_not_need_to_apply]

  val regimes = Seq(
    LegacyRegime.SA,
    LegacyRegime.CT,
    LegacyRegime.PAYE
  )

  regimes.foreach { regime =>
    s"YouMayNotNeedToApply view for regime $regime" should {

      val title = messages(s"${regime.msgPrefix}.you-may-not-need-to-apply.title")

      "render page correctly without errors" in {
        val form = YouMayNotNeedToApplyForm.form(regime)

        val html =
          view(form, regime)(
            messages,
            fakeRequest,
            appConfig
          )
        val doc = Jsoup.parse(html.toString())

        val h1s = doc.select("h1")

        h1s.get(0).text() shouldBe title
        h1s.get(1).text() shouldBe messages("asa.legacy.you-may-not-need-to-apply.h1")

        doc.select("input[type=radio]").size() shouldBe 2

        doc.select("button").text() shouldBe messages("common.continue")
      }

      "render error summary when form has errors" in {
        val form = YouMayNotNeedToApplyForm.form(regime).bind(Map.empty[String, String])

        val html =
          view(form, regime)(
            messages,
            fakeRequest,
            appConfig
          )
        val doc = Jsoup.parse(html.toString())

        doc.select(".govuk-error-summary").size() shouldBe 1

        doc.select(".govuk-error-summary__list a").text() shouldBe
          messages(s"${regime.msgPrefix}.you-may-not-need-to-apply.error.required")
      }

      "pre-select radio when value exists (true)" in {
        val form = YouMayNotNeedToApplyForm.form(regime).fill(YouMayNotNeedToApplyFormValues(true))

        val doc = Jsoup.parse(
          view(form, regime)(
            messages,
            fakeRequest,
            appConfig
          ).toString()
        )

        doc.select("input[value=true]").hasAttr("checked") shouldBe true
      }

      "pre-select radio when value exists (false)" in {
        val form = YouMayNotNeedToApplyForm.form(regime).fill(YouMayNotNeedToApplyFormValues(false))

        val doc = Jsoup.parse(
          view(form, regime)(
            messages,
            fakeRequest,
            appConfig
          ).toString()
        )

        doc.select("input[value=false]").hasAttr("checked") shouldBe true
      }
    }
  }

}
