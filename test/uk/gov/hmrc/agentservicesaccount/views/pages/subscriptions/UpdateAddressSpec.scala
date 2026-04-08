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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionAddressForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.CtSubscriptionAddressForm.addressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.{CtAddressFormValues, LegacyRegime}
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.update_address

class UpdateAddressSpec
extends ViewBaseSpec {

  private val view: update_address = inject[update_address]

  private val legacyRegime = LegacyRegime.CT

  private val legacyRegimePrefix = s"asa.legacy.${legacyRegime.toString.toLowerCase}"

  private def formatAddress(address: BusinessAddress): String = List(
    Some(address.addressLine1),
    address.addressLine2,
    address.addressLine3,
    address.addressLine4,
    address.postalCode,
    Some(address.countryCode)
  ).flatten.map(play.twirl.api.HtmlFormat.escape)
    .map(_.body)
    .mkString(", ")

  private val subscriptionBusinessAddress = BusinessAddress(
    "25 Any Street",
    Some("Central Grange"),
    Some("Telford"),
    None,
    Some("TF4 3TR"),
    "GB"
  )

  private val subscriptionAddress = formatAddress(subscriptionBusinessAddress)

  private val addressForm: Form[CtAddressFormValues] = CtSubscriptionAddressForm.form

  private val formWithUseAsaError: Form[CtAddressFormValues] = addressForm.withError(
    key = addressUseAsaDataKey,
    message = messages(s"$legacyRegimePrefix.address.use-asa.error.required")
  )

  def render(form: Form[CtAddressFormValues]): Document = Jsoup.parse(
    view(form, subscriptionAddress, legacyRegime)(
      messages,
      fakeRequest,
      appConfig
    ).body
  )

  private val title: String = messages(s"$legacyRegimePrefix.address.title")

  "update_address" when {

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

      val doc: Document = render(addressForm)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe s"$title - Agent services account - GOV.UK"
      }

      "display correct radio options" in {
        val radios = doc.select(".govuk-radios__item")
        radios.size() mustBe 2
        radios.get(0).text() mustBe subscriptionAddress + " " + messages(s"$legacyRegimePrefix.address.use-asa.true.hint")
        radios.get(0).select("input").attr("name") mustBe addressUseAsaDataKey
        radios.get(1).text() mustBe messages(s"$legacyRegimePrefix.address.use-asa.false")
        radios.get(1).select("input").attr("name") mustBe addressUseAsaDataKey
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
        errorLink.text() mustBe messages(s"$legacyRegimePrefix.address.use-asa.error.required")
        errorLink.attr("href") mustBe s"#$addressUseAsaDataKey"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe s"Error: ${messages(s"$legacyRegimePrefix.address.use-asa.error.required")}"
      }
    }

  }

}
