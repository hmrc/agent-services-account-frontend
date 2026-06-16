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
import play.api.data.Form
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressNewKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.EmailAddressFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.update_email_address

class UpdateEmailAddressSpec
extends ViewBaseSpec {

  private val view: update_email_address = inject[update_email_address]
  private val asaDetailsAgencyName = "ABC-No.1 Accountants"
  private val asaDetailsAgencyEmail = "joe@bloggs.com"

  private val legacyRegime = LegacyRegime.PAYE

  private val legacyRegimePrefix = legacyRegime.msgPrefix

  private val emailAddressForm: Form[EmailAddressFormValues] = SubscriptionEmailAddressForm.form(legacyRegime, "Agency Name")

  private val formWithUseAsaError: Form[EmailAddressFormValues] = emailAddressForm.withError(
    key = emailAddressUseAsaDataKey,
    message = messages(s"$legacyRegimePrefix.email-address.use-asa.error.required")
  )
  private val formWithNewEmailAddressError: Form[EmailAddressFormValues] = emailAddressForm.withError(
    key = emailAddressNewKey,
    message = messages(s"$legacyRegimePrefix.email-address.input.error.empty")
  )

  def render(form: Form[EmailAddressFormValues]): Document = Jsoup.parse(
    view(
      form,
      asaDetailsAgencyName,
      asaDetailsAgencyEmail,
      legacyRegime
    )(
      messages,
      fakeRequest,
      appConfig
    ).body
  )

  private val title: String = messages(s"$legacyRegimePrefix.email-address.title")
  private val heading: String = messages(s"$legacyRegimePrefix.email-address.heading", asaDetailsAgencyName)

  "update_email_address" when {

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

      "have the correct h1 heading" in {
        doc.select("h1").first.text() mustBe heading
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

      "display correct radio options" in {
        val radios = doc.select(".govuk-radios__item")
        radios.size() mustBe 2
        radios.get(0).text() mustBe asaDetailsAgencyEmail
        radios.get(0).select("input").attr("name") mustBe emailAddressUseAsaDataKey
        radios.get(1).text() mustBe messages(s"$legacyRegimePrefix.email-address.use-asa.false")
        radios.get(1).select("input").attr("name") mustBe emailAddressUseAsaDataKey
      }

      "hide the conditional new email address input" in {
        val conditionalHidden = doc.select(".govuk-radios__conditional--hidden")
        conditionalHidden.size() mustBe 1
        conditionalHidden.text() mustBe messages(s"$legacyRegimePrefix.email-address.new-input.label") + " " + messages(
          s"$legacyRegimePrefix.email-address.new-input.hint"
        )
        conditionalHidden.select(".govuk-input").attr("name") mustBe emailAddressNewKey
      }
    }

    "when 'new email address' option is selected" should {

      val filledForm: Form[EmailAddressFormValues] = emailAddressForm.fill(
        EmailAddressFormValues(
          useAsaData = false,
          newEmailAddress = Some("hello@new.com")
        )
      )

      val doc: Document = render(filledForm)

      "show the conditional input section" in {
        val conditional = doc.select(".govuk-radios__conditional").first()
        conditional.hasClass("govuk-radios__conditional--hidden") mustBe false
      }

      "have the new email address input present" in {
        doc.select("#emailAddressNew").size() mustBe 1
      }

      "pre-fill the new email address input" in {
        doc.select("#emailAddressNew").`val`() mustBe "hello@new.com"
      }

      "have the correct radio selected" in {
        val radios = doc.select("input[name=emailAddressUseAsaData]")
        radios.get(1).hasAttr("checked") mustBe true
      }
    }

    "form is submitted with useAsa errors should" should {

      val doc: Document = render(formWithUseAsaError)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe messages(s"$legacyRegimePrefix.email-address.use-asa.error.required")
        errorLink.attr("href") mustBe s"#$emailAddressUseAsaDataKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages(s"$legacyRegimePrefix.email-address.use-asa.error.required")}"
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
        errorLink.text() mustBe messages(s"$legacyRegimePrefix.email-address.input.error.empty")
        errorLink.attr("href") mustBe s"#$emailAddressNewKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages(s"$legacyRegimePrefix.email-address.input.error.empty")}"
      }
    }
  }

}
