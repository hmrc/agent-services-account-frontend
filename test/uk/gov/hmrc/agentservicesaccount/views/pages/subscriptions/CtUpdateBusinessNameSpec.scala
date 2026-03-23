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

package uk.gov.hmrc.agentservicesaccount.views.pages.subscriptions

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionForms.businessNameNewKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionForms.businessNameUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionForms.newBusinessNameForm
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtBusinessNameFormValues
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.ct_update_business_name

class CtUpdateBusinessNameSpec
extends ViewBaseSpec {

  val view: ct_update_business_name = inject[ct_update_business_name]
  val subscriptionBusinessName = "ABC-No.1 Accountants"

  val form: Form[CtBusinessNameFormValues] = newBusinessNameForm
  val formWithUseAsaError: Form[CtBusinessNameFormValues] = form.withError(
    key = businessNameUseAsaDataKey,
    message = messages("asa.legacy.ct.business-name.use-asa.error.required")
  )
  val formWithNewBusinessNameError: Form[CtBusinessNameFormValues] = form.withError(
    key = businessNameNewKey,
    message = messages("asa.legacy.ct.business-name.new-input.error.empty")
  )

  "ct_update_business_name" when {

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

    val title: String = messages("asa.legacy.ct.business-name.title")

    def testPageStaticContent(doc: Document): Unit = {

      "have the correct h1 heading and introduction" in {
        doc.select("h1").first.text() mustBe title
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }
    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form, subscriptionBusinessName)(
        messages,
        fakeRequest,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe s"$title - Agent services account - GOV.UK"
      }

      "display correct radio options" in {
        val radios = doc.select(".govuk-radios__item")
        radios.size() mustBe 2
        radios.get(0).text() mustBe subscriptionBusinessName
        radios.get(0).select("input").attr("name") mustBe businessNameUseAsaDataKey
        radios.get(1).text() mustBe messages("asa.legacy.ct.business-name.use-asa.false")
        radios.get(1).select("input").attr("name") mustBe businessNameUseAsaDataKey
      }

      "hide the conditional new business name input" in {
        val conditionalHidden = doc.select(".govuk-radios__conditional--hidden")
        conditionalHidden.size() mustBe 1
        conditionalHidden.text() mustBe messages("asa.legacy.ct.business-name.new-input.hint")
        conditionalHidden.select(".govuk-input").attr("name") mustBe businessNameNewKey
      }
    }

    "form is submitted with useAsa errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithUseAsaError, subscriptionBusinessName)(
        messages,
        fakeRequest,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe messages("asa.legacy.ct.business-name.use-asa.error.required")
        errorLink.attr("href") mustBe s"#$businessNameUseAsaDataKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages("asa.legacy.ct.business-name.use-asa.error.required")}"
      }
    }

    "form is submitted with newBusinessName errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithNewBusinessNameError, subscriptionBusinessName)(
        messages,
        fakeRequest,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe messages("asa.legacy.ct.business-name.new-input.error.empty")
        errorLink.attr("href") mustBe s"#$businessNameNewKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages("asa.legacy.ct.business-name.new-input.error.empty")}"
      }
    }
  }

}
