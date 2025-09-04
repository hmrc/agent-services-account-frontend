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
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.suspension_warning

class SuspensionWarningViewSpec
extends ViewBaseSpec {

  val view: suspension_warning = inject[suspension_warning]

  "suspension_warning" should {

    val doc: Document = Jsoup.parse(view.apply()(
      messages,
      fakeRequest,
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
      doc.title() shouldBe "We have suspended your access to your agent services account - Agent services account - GOV.UK"
    }

    "display correct h1" in {
      doc.select("h1").text() shouldBe "We have suspended your access to your agent services account"
    }

    "display the correct body content" in {
      doc.select("p").get(0).text() shouldBe "We have sent you a letter to confirm this."
      doc.select("p").get(1).text() shouldBe "If you did not receive our letter, or think we have made a mistake, we can discuss this."
      doc.select("p").get(2).text() shouldBe "We can only talk to people in the roles below:"
      doc.select(".govuk-list--item").get(0).text() shouldBe "director"
      doc.select(".govuk-list--item").get(1).text() shouldBe "partner"
      doc.select(".govuk-list--item").get(2).text() shouldBe "sole trader"
      doc.select("p").get(3).text() shouldBe "If you want us to get in touch, select ‘Continue’. You can then enter the relevant contact details."
    }

    "display the correct button link" in {
      val buttonLink = doc.select(".govuk-button")
      buttonLink.text() shouldBe "Continue"
      buttonLink.attr("href") shouldBe "/agent-services-account/recovery-contact-details"
    }

    "display the correct link" in {
      val buttonLink = doc.select(".govuk-link").get(3)
      buttonLink.text() shouldBe "Return to Government Gateway sign in"
      buttonLink.attr("href") shouldBe "/agent-services-account/signed-out"
    }
  }

}
