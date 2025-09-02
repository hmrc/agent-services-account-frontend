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
import play.api.data.Form
import uk.gov.hmrc.agentservicesaccount.forms.SuspendDescriptionForm
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.recovery_description

class RecoveryDescriptionViewSpec
extends ViewBaseSpec {

  val view: recovery_description = inject[recovery_description]

  val form = SuspendDescriptionForm.form

  def renderPage(form: Form[String]): Document = Jsoup.parse(view.apply(form)(
    messages,
    fakeRequest,
    appConfig
  ).body)

  "recovery_description" should {
    val doc: Document = renderPage(form)

    "have the correct service name link" in {
      doc.select(".govuk-header__service-name").first.text() shouldBe "Agent services account"
      doc.select(".govuk-header__service-name").first.attr("href") shouldBe "/agent-services-account"
    }

    "have the correct sign out link" in {
      doc.select(".hmrc-sign-out-nav__link").first.text() shouldBe "Sign out"
      doc.select(".hmrc-sign-out-nav__link").first.attr("href") shouldBe "/agent-services-account/sign-out"
    }

    "display the correct page title" in {
      doc.title() shouldBe "Details of issue - Agent services account - GOV.UK"
    }

    "display a back link" in {
      doc.select(".govuk-back-link").text() shouldBe "Back"
    }

    "display correct h1" in {
      doc.select("h1").text() shouldBe "Details of issue"
    }

    "display correct h2" in {
      doc.select(".govuk-caption-xl").text() shouldBe "Account recovery"
    }

    "display correct label" in {
      doc.select(".govuk-label").first().text() shouldBe "Tell us what happened"
      doc.select(
        ".govuk-hint"
      ).first().text() shouldBe "Do not include personal or financial information like your National Insurance number or credit card details."
    }

    "display character count message" in {
      doc.select(".govuk-hint").get(1).text() shouldBe "You can enter up to 250 characters"
    }

    "display correct button" in {
      doc.select(".govuk-button").first().text() shouldBe "Save and continue"
    }
  }

  "recovery_description with empty submit errors" should {

    val formWithErrors = form.bind(Map("description" -> ""))

    val doc: Document = renderPage(formWithErrors)

    "display the error summary" in {
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      val errors = doc.select(".govuk-error-summary__list li")
      errors.get(0).text() shouldBe """Enter a description of the problem. For example, "I did not receive a letter"."""
    }
  }

  "recovery_description with invalid data submit errors" should {

    val formWithErrors = form.bind(Map("description" -> s"${List.fill(251)("a").mkString}"))

    val doc: Document = renderPage(formWithErrors)

    "display the error summary" in {
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      val errors = doc.select(".govuk-error-summary__list li")
      errors.get(0).text() shouldBe "Description must be 250 characters or fewer."
    }
  }

}
