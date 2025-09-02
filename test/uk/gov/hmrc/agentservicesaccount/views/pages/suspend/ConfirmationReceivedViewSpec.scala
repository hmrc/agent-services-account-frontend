/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.views.pages.suspend

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.confirmation_received

class ConfirmationReceivedViewSpec
extends ViewBaseSpec {

  val view: confirmation_received = inject[confirmation_received]

  "confirmation_received" should {

    val doc: Document = Jsoup.parse(view.apply()(
      fakeRequest,
      messages,
      appConfig
    ).body)

    "have the correct service name link" in {
      doc.select(".govuk-header__service-name").first.text() shouldBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") shouldBe "/agent-services-account"
    }

    "have the correct sign out link" in {
      doc.select(".hmrc-sign-out-nav__link").first.text() shouldBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") shouldBe "/agent-services-account/sign-out"
    }

    "display the correct page title" in {
      doc.title() shouldBe "We have received your details - Agent services account - GOV.UK"
    }

    "display correct h1" in {
      doc.select("h1").text() shouldBe "We have received your details"
    }

    "display correct h2" in {
      doc.select("h2.govuk-heading-m").text() shouldBe "What happens next"
    }

    "display the correct paragraphs" in {
      doc.select("p.govuk-body").text() shouldBe "We will get in touch using the contact details you provided. We aim to do this within 5 working days."
    }

    "display the correct link" in {
      doc.select("a.govuk-link").get(2).text() shouldBe "Return to Government Gateway sign in"
    }
  }

}
