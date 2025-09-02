/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.views.pages.contactDetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.saCodeForm
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details._

class EnterSaCodePageSpec
extends ViewBaseSpec {

  val view: enter_sa_code = inject[enter_sa_code]

  val form: Form[String] = saCodeForm
  val formWithNameErrors: Form[String] = form.withError(key = "saCode", message = Messages("update-contact-details.sa-code.error.empty"))

  "enter_sa_code" when {

    def testServiceStaticContent(doc: Document): Unit = {

      "have the correct service name link" in {
        doc.select(".govuk-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".govuk-header__service-name").first.attr("href") mustBe "/agent-services-account"
      }
      "have the correct sign out link" in {
        doc.select(".hmrc-sign-out-nav__link").first.text() mustBe "Sign out"
        doc.select(".hmrc-sign-out-nav__link").first.attr("href") mustBe "/agent-services-account/sign-out"
      }
      "have the correct back link" in {
        doc.select(".govuk-back-link").first.text() mustBe "Back"
        doc.select(".govuk-back-link").first.attr("href") mustBe "#"
      }
    }

    def testPageStaticContent(doc: Document): Unit = {

      "have the correct h1 heading and introduction" in {
        doc.select("h1").first.text() mustBe "What's the agent code you use for Self Assessment?"
      }

      "have the correct hint" in {
        doc.select(".govuk-hint").first.text() mustBe "This is a 6-character code made up of numbers and letters. For example, A1234B or 5678CD."
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }

      "have the correct continue without  button" in {
        doc.select(".govuk-button").last().text() mustBe "Continue without code"
      }

    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form)(
        messages,
        fakeRequest,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe "What's the agent code you use for Self Assessment? - Agent services account - GOV.UK"
      }
    }

    "form is submitted with name errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithNameErrors)(
        messages,
        fakeRequest,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: What's the agent code you use for Self Assessment? - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Enter the agent code you use for Self Assessment."
        errorLink.attr("href") mustBe "#saCode"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Enter the agent code you use for Self Assessment."
      }
    }

  }

}
