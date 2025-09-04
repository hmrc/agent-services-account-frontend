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
import uk.gov.hmrc.agentservicesaccount.models.AccountRecoverySummary
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.check_recovery_answers

class CheckRecoveryAnswersViewSpec
extends ViewBaseSpec {

  val view: check_recovery_answers = inject[check_recovery_answers]

  val accountRecoverySummary: AccountRecoverySummary = AccountRecoverySummary(
    name = "Bob",
    email = "abc@abc.com",
    phone = "0101",
    description = "help",
    arn = "123"
  )

  "check_recovery_answers" should {

    val doc: Document = Jsoup.parse(view.apply(accountRecoverySummary)(
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
      doc.title() shouldBe "Check your answers - Agent services account - GOV.UK"
    }

    "display a back link" in {
      doc.select(".govuk-back-link").text() shouldBe "Back"
    }

    "display correct h1" in {
      doc.select("h1").text() shouldBe "Check your answers"
    }

    "display correct h2" in {
      doc.select(".govuk-caption-xl").text() shouldBe "Account recovery"
    }

    "display correct definition list data" in {
      val list = doc.select(".govuk-summary-list__row")
      list.get(0).text() shouldBe "Contact name Bob Change"
      list.get(1).text() shouldBe "Contact email address abc@abc.com Change"
      list.get(2).text() shouldBe "Telephone number 0101 Change"
      list.get(3).text() shouldBe "Details of issue help Change"
    }

    "display correct button" in {
      doc.select(".govuk-button").first().text() shouldBe "Confirm and send"
    }
  }

}
