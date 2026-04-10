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
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.confirmation

class ConfirmationPageSpec
extends ViewBaseSpec {

  private val view: confirmation = inject[confirmation]

  private val legacyRegime = LegacyRegime.SA

  "confirmation view" should {

    val doc: Document = Jsoup.parse(
      view.apply(legacyRegime)(
        fakeRequest,
        messages,
        appConfig
      ).body
    )

    "have the correct service name link" in {
      doc.select(".govuk-header__service-name").first.text() shouldBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") shouldBe "/agent-services-account"
    }

    "have the correct sign out link" in {
      doc.select(".hmrc-sign-out-nav__link").first.text() shouldBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") shouldBe "/agent-services-account/sign-out"
    }

    "display the correct page title" in {
      doc.title() shouldBe "We are processing your enrolment - Agent services account - GOV.UK"
    }

    "display the confirmation panel with correct content" in {
      val panel = doc.select(".govuk-panel")

      panel.select(".govuk-panel__title").text() shouldBe "We are processing your enrolment"
      panel.select(".govuk-panel__body").text() should not be empty
    }

    "display the confirmation paragraphs" in {
      val paragraphs = doc.select("p.govuk-body")

      paragraphs.get(0).text() should not be empty
      paragraphs.get(1).text() should not be empty
      paragraphs.get(2).text() should not be empty
    }

    "display the return home link" in {
      val homeLink = doc.select(".govuk-link").get(2)

      homeLink.text() shouldBe "Return to your agent services account homepage"
      homeLink.attr("href") shouldBe "/agent-services-account"
    }
  }

}
