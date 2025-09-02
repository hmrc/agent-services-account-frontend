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
import uk.gov.hmrc.agentservicesaccount.forms.ContactDetailsSuspendForm
import uk.gov.hmrc.agentservicesaccount.models.SuspendContactDetails
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.suspend.contact_details

class ContactDetailsViewSpec
extends ViewBaseSpec {

  val view: contact_details = inject[contact_details]

  val form = ContactDetailsSuspendForm.form

  def renderPage(form: Form[SuspendContactDetails]): Document = Jsoup.parse(view.apply(form)(
    messages,
    fakeRequest,
    appConfig
  ).body)

  "contact_details" should {

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
      doc.title() shouldBe "Contact Details - Agent services account - GOV.UK"
    }

    "display a back link" in {
      doc.select(".govuk-back-link").text() shouldBe "Back"
    }

    "display correct h1" in {
      doc.select("h1").text() shouldBe "Contact Details"
    }

    "display correct h2" in {
      doc.select(".govuk-caption-xl").text() shouldBe "Account recovery"
    }

    "display correct inset text" in {
      doc.select(".govuk-inset-text").text() shouldBe "We can only talk to a director, partner or sole trader."
    }

    "display correct labels" in {
      doc.select(".govuk-label").get(0).text() shouldBe "Contact name"
      doc.select(".govuk-label").get(1).text() shouldBe "Contact email address"
      doc.select(".govuk-label").get(2).text() shouldBe "Telephone number"
      doc.select(".govuk-hint").first().text() shouldBe "We need this to call you about your account"
    }

    "display correct button" in {
      doc.select(".govuk-button").first().text() shouldBe "Save and continue"
    }
  }

  "contact_details with empty submit errors" should {

    val formWithErrors = form.bind(Map(
      "name" -> "",
      "email" -> "",
      "phone" -> ""
    ))

    val doc: Document = renderPage(formWithErrors)

    "display the error summary" in {
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      val errors = doc.select(".govuk-error-summary__list li")
      errors.get(0).text() shouldBe "Enter the contact name"
      errors.get(1).text() shouldBe "Enter an email address with a name, @ symbol and a domain name, like name@example.com"
      errors.get(2).text() shouldBe "Enter your telephone number"
    }
  }

  "contact_details with invalid data errors" should {

    val formWithErrors = form.bind(Map(
      "name" -> "<>",
      "email" -> "<>",
      "phone" -> "<>"
    ))

    val doc: Document = renderPage(formWithErrors)

    "display the error summary" in {
      doc.select(".govuk-error-summary__title").text() shouldBe "There is a problem"
      val errors = doc.select(".govuk-error-summary__list li")
      errors.get(0).text() shouldBe "Name must not include the characters < and >"
      errors.get(1).text() shouldBe "Enter an email address with a name, @ symbol and a domain name, like name@example.com"
      errors.get(2).text() shouldBe "Enter a telephone number, like 01642 123 456, 07777 777 777 or +33 23 45 67 88"
    }
  }

}
