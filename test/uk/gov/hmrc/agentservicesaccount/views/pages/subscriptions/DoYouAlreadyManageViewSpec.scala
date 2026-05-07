/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.DoYouAlreadyManageForm.doYouAlreadyManageKey
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.DoYouAlreadyManageFormValues
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.subscriptions.do_you_already_manage

class DoYouAlreadyManageViewSpec
extends ViewBaseSpec {

  private val view: do_you_already_manage = inject[do_you_already_manage]

  private val asaDetailsAgencyName = "Test Agency"

  private val regimes = Seq(
    LegacyRegime.SA,
    LegacyRegime.CT,
    LegacyRegime.PAYE
  )

  regimes.foreach { regime =>
    s"do_you_already_manage view for regime $regime" when {

      val form: Form[DoYouAlreadyManageFormValues] = DoYouAlreadyManageForm.form(regime, asaDetailsAgencyName)

      val formWithError: Form[DoYouAlreadyManageFormValues] = form.withError(
        key = doYouAlreadyManageKey,
        message = messages(s"${regime.msgPrefix}.do-you-already-manage.error.required")
      )

      def render(form: Form[DoYouAlreadyManageFormValues]): Document = Jsoup.parse(
        view(
          form,
          regime,
          asaDetailsAgencyName
        )(
          messages,
          fakeRequest,
          appConfig
        ).body
      )

      val title: String = messages(
        s"asa.legacy.${regime.toString.toLowerCase}.do-you-already-manage.title",
        asaDetailsAgencyName
      )

      def testServiceStaticContent(doc: Document): Unit = {

        "have the correct service name link" in {
          doc.select(".govuk-header__service-name").text() mustBe "Agent services account"
        }

        "have the correct sign out link" in {
          doc.select(".hmrc-sign-out-nav__link").text() mustBe "Sign out"
        }

        "have the correct back link" in {
          doc.select(".govuk-back-link").text() mustBe "Back"
        }
      }

      def testPageStaticContent(doc: Document): Unit = {

        "have the correct heading" in {
          doc.select("h1").text() mustBe title
        }

        "have the correct continue button" in {
          doc.select(".govuk-button").text() mustBe "Continue"
        }
      }

      "first viewing page" should {

        val doc = render(form)

        testServiceStaticContent(doc)
        testPageStaticContent(doc)

        "display correct page title" in {
          doc.title() mustBe s"$title - Agent services account - GOV.UK"
        }

        "display correct radio options" in {
          val radios = doc.select(".govuk-radios__item")

          radios.size() mustBe 2

          radios.get(0).text() mustBe messages("asa.legacy.do-you-already-manage.existing.true")
          radios.get(0).select("input").attr("value") mustBe "true"

          radios.get(1).text() mustBe
            messages(s"asa.legacy.${regime.toString.toLowerCase}.do-you-already-manage.existing.false")
          radios.get(1).select("input").attr("value") mustBe "false"
        }
      }

      "when form is pre-filled with true" should {

        val doc = render(form.fill(DoYouAlreadyManageFormValues(true)))

        "have the correct radio selected" in {
          doc.select(s"input[name=$doYouAlreadyManageKey]").get(0).hasAttr("checked") mustBe true
        }
      }

      "when form is pre-filled with false" should {

        val doc = render(form.fill(DoYouAlreadyManageFormValues(false)))

        "have the correct radio selected" in {
          doc.select(s"input[name=$doYouAlreadyManageKey]").get(1).hasAttr("checked") mustBe true
        }
      }

      "form submitted with error" should {

        val doc = render(formWithError)

        testServiceStaticContent(doc)
        testPageStaticContent(doc)

        "display error prefix in title" in {
          doc.title() mustBe s"Error: $title - Agent services account - GOV.UK"
        }

        "display error summary" in {
          val errorLink = doc.select(".govuk-error-summary__list a").first()

          errorLink.text() mustBe
            messages(s"${regime.msgPrefix}.do-you-already-manage.error.required")

          errorLink.attr("href") mustBe s"#$doYouAlreadyManageKey"
        }

        "display error styling" in {
          doc.select(".govuk-form-group--error").size() mustBe 1
        }

        "display error message" in {
          doc.select(".govuk-error-message").text() mustBe
            s"Error: ${messages(s"${regime.msgPrefix}.do-you-already-manage.error.required")}"
        }
      }
    }
  }

}
