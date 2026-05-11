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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressNewKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.EmailAddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.ctsa_custom_email_address

class CtSaCustomEmailAddressSpec
extends ViewBaseSpec {

  private val view: ctsa_custom_email_address = inject[ctsa_custom_email_address]
  private val asaDetailsAgencyEmail = "joe@bloggs.com"

  private val legacyRegime = LegacyRegime.PAYE

  private val legacyRegimePrefix = legacyRegime.msgPrefix

  private val emailAddressForm: Form[EmailAddressFormValues] = SubscriptionEmailAddressForm.form(legacyRegime)

  private val formWithNewEmailAddressError: Form[EmailAddressFormValues] = emailAddressForm.withError(
    key = emailAddressNewKey,
    message = messages(s"$legacyRegimePrefix.custom-email-address.new-input.error.empty")
  )

  def render(form: Form[EmailAddressFormValues]): Document = Jsoup.parse(
    view(
      form,
      asaDetailsAgencyEmail,
      legacyRegime
    )(
      messages,
      fakeRequest,
      appConfig
    ).body
  )

  private val title: String = messages(s"$legacyRegimePrefix.custom-email-address.title")

  "ctsa_custom_email_address" when {

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
        doc.select("h1").first.text() mustBe title
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Continue"
      }
    }

    "first viewing page" should {

      val doc: Document = render(emailAddressForm)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe s"$title - Agent services account - GOV.UK"
      }

      "display the agencyDetails emailAddress as inset text" in {
        val hint = doc.select(".govuk-inset")
        hint.first().text() mustBe asaDetailsAgencyEmail
      }

      "display the correct label" in {
        val hint = doc.select(".govuk-label")
        hint.first().text() mustBe messages(s"$legacyRegimePrefix.custom-email-address.input.label")
      }

      "display the correct hint" in {
        val hint = doc.select(".govuk-hint")
        hint.first().text() mustBe messages(s"$legacyRegimePrefix.custom-email-address.input.hint")
      }

      "display the contact name input" in {
        val input = doc.select(".govuk-input")
        input.first().attr("name") mustBe emailAddressNewKey
      }
    }

    "form is submitted with newEmailAddress errors should" should {

      val doc: Document = render(formWithNewEmailAddressError)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe messages(s"$legacyRegimePrefix.custom-email-address.new-input.error.empty")
        errorLink.attr("href") mustBe s"#$emailAddressNewKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages(s"$legacyRegimePrefix.custom-email-address.new-input.error.empty")}"
      }
    }
  }

}
