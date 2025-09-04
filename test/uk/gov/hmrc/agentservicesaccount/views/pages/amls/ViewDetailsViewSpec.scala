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

package uk.gov.hmrc.agentservicesaccount.views.pages.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.view_details

import java.time.LocalDate

class ViewDetailsViewSpec
extends ViewBaseSpec {

  val view: view_details = inject[view_details]

  "view_details" when {

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

      "display the correct page title" in {
        doc.title() mustBe "Anti-money laundering supervision details - Agent services account - GOV.UK"
      }

      "have the correct h1 heading" in {
        doc.select("h1").first.text() mustBe "Anti-money laundering supervision details"
      }

      "have a link to Manage account" in {
        doc.select("#manage-account").first().text() mustBe "Return to Manage account"
        doc.select("#manage-account").first().attr("href") mustBe "/agent-services-account/manage-account"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.NoAmlsDetailsUK}" should {

      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.NoAmlsDetailsUK, None)(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display paragraph content" in {
        doc.select(".govuk-body").get(0).text() shouldBe "Agents need to register with an anti-money laundering supervisor."
        doc.select(".govuk-body").get(1).text() shouldBe "Tell us your supervision details, including:"
      }
      "display unordered list content" in {
        doc.select(".govuk-list--item").get(0).text() shouldBe "the name of your supervisory body"
        doc.select(".govuk-list--item").get(1).text() shouldBe "your registration number"
        doc.select(".govuk-list--item").get(2).text() shouldBe "the renewal date of your registration"
      }
      "display a link styled as button" in {
        doc.select(".govuk-button").first().text() shouldBe "Add supervision details"
        doc.select(".govuk-button").first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"
      }
      "display link to more information" in {
        doc.select("#registration-info").get(0).text() shouldBe "Read more about registration (opens in a new tab)"
        doc.select("#registration-info").get(0).attr("target") shouldBe "_blank"
        doc.select("#registration-info").get(0).attr("rel") shouldBe "noreferrer noopener"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.NoAmlsDetailsNonUK}" should {

      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.NoAmlsDetailsNonUK, None)(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display paragraph content" in {
        doc.select(".govuk-body").first().text() shouldBe "If you are registered, tell us your anti-money laundering supervision details, including:"
      }
      "display unordered list content" in {
        doc.select(".govuk-list--item").get(0).text() shouldBe "the name of your supervisory body"
        doc.select(".govuk-list--item").get(1).text() shouldBe "your registration number"
      }
      "display a link styled as button" in {
        doc.select(".govuk-button").first().text() shouldBe "Add supervision details"
        doc.select(".govuk-button").first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.ExpiredAmlsDetailsUK}" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "HMRC",
        membershipNumber = Some("123"),
        membershipExpiresOn = Some(LocalDate.parse("2024-02-10"))
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.ExpiredAmlsDetailsUK, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display inset text content" in {
        doc.select(".govuk-inset-text").first.text() shouldBe "This registration has expired."
      }
      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "HMRC"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
        doc.select(".govuk-summary-list__key").get(2).text() shouldBe "Next renewal date"
        doc.select(".govuk-summary-list__value").get(2).text() shouldBe "10 February 2024"
      }
      "display a link styled as button" in {
        doc.select(".govuk-button").first().text() shouldBe "Update details"
        doc.select(".govuk-button").first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-supervisory-body"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.ValidAmlsDetailsUK} when HMRC registered" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "HMRC",
        membershipNumber = Some("123"),
        membershipExpiresOn = Some(LocalDate.parse("2025-02-10"))
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.ValidAmlsDetailsUK, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "HMRC"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
        doc.select(".govuk-summary-list__key").get(2).text() shouldBe "Next renewal date"
        doc.select(".govuk-summary-list__value").get(2).text() shouldBe "10 February 2025"
      }
      "display h2" in {
        doc.select("h2").first().text() shouldBe "Keep your details up to date"
      }
      "display paragraph content" in {
        doc.select(
          ".govuk-body"
        ).first().text() shouldBe "You need to confirm your anti-money laundering supervisions details with us whenever you change to a new provider."
      }

      "display a link to update supervisory details" in {
        doc.select("#start-journey").first().text() shouldBe "Update anti-money laundering supervision details"
        doc.select(
          "#start-journey"
        ).first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-supervisory-body"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.ValidAmlsDetailsUK} when not HMRC registered" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "ICAEW",
        membershipNumber = Some("123"),
        membershipExpiresOn = Some(LocalDate.parse("2025-02-10"))
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.ValidAmlsDetailsUK, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "ICAEW"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
        doc.select(".govuk-summary-list__key").get(2).text() shouldBe "Next renewal date"
        doc.select(".govuk-summary-list__value").get(2).text() shouldBe "10 February 2025"
      }
      "display h2" in {
        doc.select("h2").first().text() shouldBe "Keep your details up to date"
      }
      "display paragraph content" in {
        doc.select(".govuk-body").first().text() shouldBe "You need to confirm your anti-money laundering supervisions details with us:"
      }
      "display unordered list content" in {
        doc.select(".govuk-list--item").get(0).text() shouldBe "once a year, after you renew your registration with the same provider"
        doc.select(".govuk-list--item").get(1).text() shouldBe "whenever you change to a new supervision provider"
      }

      "display a link to update supervisory details" in {
        doc.select("#start-journey").first().text() shouldBe "Update anti-money laundering supervision details"
        doc.select(
          "#start-journey"
        ).first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-supervisory-body"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.ValidAmlsNonUK}" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "ICA",
        membershipNumber = Some("123"),
        membershipExpiresOn = None
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.ValidAmlsNonUK, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "ICA"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
      }
      "display h2" in {
        doc.select("h2").first().text() shouldBe "Keep your details up to date"
      }
      "display paragraph content" in {
        doc.select(
          ".govuk-body"
        ).first().text() shouldBe "You need to confirm your anti-money laundering supervisions details with us whenever you change them."
      }
      "display a link to update supervisory details" in {
        doc.select("#start-journey").first().text() shouldBe "Update anti-money laundering supervision details"
        doc.select(
          "#start-journey"
        ).first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/confirm-supervisory-body"
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.PendingAmlsDetails}" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "HMRC",
        membershipNumber = Some("123"),
        appliedOn = Some(LocalDate.parse("2024-10-10"))
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.PendingAmlsDetails, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "HMRC"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
        doc.select(".govuk-summary-list__key").get(2).text() shouldBe "Registration status"
        doc.select(".govuk-summary-list__value").get(2).text() shouldBe "Pending"
      }
      "display inset text content" in {
        doc.select(".govuk-inset-text").first.text() shouldBe "If HMRC agrees to act as your supervisory body, we’ll update these automatically."
      }
      "display details element content" in {
        doc.select(".govuk-details__summary-text").first().text() shouldBe "What to do if HMRC cannot act as your supervisory body"
        doc.select(".govuk-details__text p").get(0).text() shouldBe "If HMRC rejects your application, you’ll still need to find a supervisory."
        doc.select(
          ".govuk-details__text p"
        ).get(1).text() shouldBe "When you’ve found an organisation to act as your supervisor, you’ll need to provide the details to HMRC."
      }
    }

    s"AmlsStatuses is ${AmlsStatuses.PendingAmlsDetailsRejected}" should {

      val amlsDetails = AmlsDetails(
        supervisoryBody = "HMRC",
        membershipNumber = Some("123"),
        appliedOn = Some(LocalDate.parse("2024-10-10"))
      )
      val doc: Document = Jsoup.parse(view.apply(AmlsStatuses.PendingAmlsDetailsRejected, Some(amlsDetails))(
        fakeRequest,
        messages,
        appConfig
      ).body)

      testServiceStaticContent(doc)

      testPageStaticContent(doc)

      "display inset text content" in {
        doc.select(".govuk-inset-text p").get(0).text() shouldBe "You need to find a supervisor."
        doc.select(
          ".govuk-inset-text p"
        ).get(1).text() shouldBe "When you’ve found an organisation to act as your supervisor, you’ll need to tell us the details."
      }
      "display amls details summary list" in {
        doc.select(".govuk-summary-list__key").get(0).text() shouldBe "Supervisory body"
        doc.select(".govuk-summary-list__value").get(0).text() shouldBe "HMRC"
        doc.select(".govuk-summary-list__key").get(1).text() shouldBe "Registration number"
        doc.select(".govuk-summary-list__value").get(1).text() shouldBe "123"
        doc.select(".govuk-summary-list__key").get(2).text() shouldBe "Registration status"
        doc.select(".govuk-summary-list__value").get(2).text() shouldBe "Rejected"
      }
      "display a link styled as button" in {
        doc.select(".govuk-button").first().text() shouldBe "Add new supervision details"
        doc.select(".govuk-button").first().attr("href") shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-supervisory-body"
      }
    }

    "AmlsStatuses expects amlsDetails but none provided" should {
      "throw exception" in {
        an[RuntimeException] mustBe thrownBy {
          Jsoup.parse(view.apply(AmlsStatuses.PendingAmlsDetailsRejected, None)(
            fakeRequest,
            messages,
            appConfig
          ).body)
        }
      }
    }
    "AmlsStatuses does not expect amlsDetails but some are provided" should {
      "throw exception" in {
        val amlsDetails = AmlsDetails(
          supervisoryBody = "HMRC",
          membershipNumber = Some("123"),
          appliedOn = Some(LocalDate.parse("2024-10-10"))
        )
        an[RuntimeException] mustBe thrownBy {
          Jsoup.parse(view.apply(AmlsStatuses.NoAmlsDetailsUK, Some(amlsDetails))(
            fakeRequest,
            messages,
            appConfig
          ).body)
        }
      }
    }
  }

}
