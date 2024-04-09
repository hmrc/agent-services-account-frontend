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

package uk.gov.hmrc.agentservicesaccount.views.pages.desi_details

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.data.Form
import play.api.i18n._
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.forms.UpdateDetailsForms.yourDetailsForm
import uk.gov.hmrc.agentservicesaccount.models.YourDetails
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.your_details

class YourDetailsSpec extends BaseISpec {


  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val lang: Lang = Lang("en")
  val view: your_details  = app.injector.instanceOf[your_details]
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  val form: Form[YourDetails] = yourDetailsForm
  val formWithNameErrors: Form[YourDetails] = form.withError(key ="fullName", message = Messages("update-contact-details.your-details.name.error.empty"))
  val formWithTelephoneErrors: Form[YourDetails] = form.withError(key ="telephone", message = Messages("update-contact-details.your-details.telephone.error.empty"))


  "update_your_details" when {

    def testServiceStaticContent(doc: Document): Unit = {

      "have the correct service name link" in {
        doc.select(".hmrc-header__service-name").first.text() mustBe "Agent services account"
        doc.select(".hmrc-header__service-name").first.attr("href") mustBe "/agent-services-account"
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
        doc.select("h1").first.text() mustBe "Your details"
      }

      "have the correct introduction" in {
        doc.select("#main-content > div > div > p.govuk-body").first.text() mustBe "We’ll use these if we need to speak to you about changes you’ve made."
      }

      "have the correct continue button" in {
        doc.select(".govuk-button").first.text() mustBe "Save and continue"
      }
    }

    "first viewing page" should {

      val doc: Document = Jsoup.parse(view.apply(form)(messages, FakeRequest(), appConfig).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display the correct page title" in {
        doc.title() mustBe "Your details - Agent services account - GOV.UK"
      }
    }


    "form is submitted with name errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithNameErrors)(messages, FakeRequest(), appConfig).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: Your details - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Enter your full name"
        errorLink.attr("href") mustBe "#fullName"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Enter your full name"
      }
    }

    "form is submitted with telephone errors should" should {

      val doc: Document = Jsoup.parse(view.apply(formWithTelephoneErrors)(messages, FakeRequest(), appConfig).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display error prefix on page title" in {
        doc.title() mustBe "Error: Your details - Agent services account - GOV.UK"
      }

      "display correct error summary link" in {
        val errorLink: Element = doc.select(".govuk-error-summary__list a").first()
        errorLink.text() mustBe "Enter your telephone number"
        errorLink.attr("href") mustBe "#telephone"
      }

      "display error styling on form" in {
        doc.select(".govuk-form-group--error").size() mustBe 1
      }

      "display error message on form" in {
        doc.select(".govuk-error-message").text() mustBe "Error: Enter your telephone number"
      }
    }
  }

}
