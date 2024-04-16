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
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.view_contact_details

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class ViewContactDetailsViewSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: view_contact_details = app.injector.instanceOf[view_contact_details]

  private val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB"))
  )

  private val testAgencyDetailsEmpty = AgencyDetails(None, None, None, None)


  private val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
  )

  private val pendingChangeOfDetails = PendingChangeOfDetails(
    arn = arn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyName = Some("New and Improved Agency")),
    otherServices = emptyOtherServices,
    timeSubmitted = Instant.now,
    submittedBy = submittedByDetails
  )

  object MessageLookup {

    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "Contact details"
      val title: String = heading + govUkSuffix

      def insetText(localDate: LocalDate) = {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
        s"New contact details were submitted on ${localDate.format(dateTimeFormatter)}. " +
          s"You cannot change contact details again until ${localDate.plusMonths(1).format(dateTimeFormatter)}."
      }

      val addressKey: String = "Address for agent services account"
      val emailKey: String = "Email address"
      val telephoneKey: String = "Telephone number"
      val businessNameKey: String = "Business name shown to clients"

      val addressValue: String = "Test Street Test Town TE5 7ED GB"
      val emailValue: String = "test@email.com"
      val telephoneValue: String = "01234 567890"
      val businessNameValue: String = "Test Name"
      val noneValue: String = "None"

      val link1 = "Update contact details"
      val link2 = "Return to Manage account"
    }

    object Welsh {
      private val govUkSuffix: String = " - Cyfrif gwasanaethau asiant - GOV.UK"
      val heading: String = "Manylion cyswllt"
      val title: String = heading + govUkSuffix

      def insetText(localDate: LocalDate) = {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
        s"Cyflwynwyd manylion newydd ar ${localDate.format(dateTimeFormatter)}. " +
          s"Ni allwch wneud unrhyw newidiadau pellach i’r manylion cyswllt tan ${localDate.plusMonths(1).format(dateTimeFormatter)}."
      }

      val addressKey: String = "Cyfeiriad ar gyfer y cyfrif gwasanaethau asiant"
      val emailKey: String = "Cyfeiriad e-bost"
      val telephoneKey: String = "Rhif ffôn"
      val businessNameKey: String = "Enw’r busnes a ddangosir i gleientiaid"

      val addressValue: String = "Test Street Test Town TE5 7ED GB"
      val emailValue: String = "test@email.com"
      val telephoneValue: String = "01234 567890"
      val businessNameValue: String = "Test Name"
      val noneValue: String = "Dim"

      val link1 = "Diweddaru manylion cyswllt"
      val link2 = "Dychwelyd i ‘Rheoli’r cyfrif’"
    }
  }

  //TODO create test constants file for reuse of the test variable


  "view_contact_details" should {
    "render correctly" when {
      "the selected lang is english" when {
        val messages: Messages = MessagesImpl(langs.head, messagesApi)
        "there are contact details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.English.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.English.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.English.addressKey
          summaryListKeys(2) mustBe MessageLookup.English.emailKey
          summaryListKeys(3) mustBe MessageLookup.English.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.English.businessNameValue
          summaryListValues(1) mustBe MessageLookup.English.addressValue
          summaryListValues(2) mustBe MessageLookup.English.emailValue
          summaryListValues(3) mustBe MessageLookup.English.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.English.link1
          links(2).attributes().get("href") mustBe "/agent-services-account/manage-account/contact-details/start-update"

          links(3).text() mustBe MessageLookup.English.link2
          links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }
        "'update contact details' link hidden for standard user" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, None, isAdmin = false)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.English.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.English.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.English.addressKey
          summaryListKeys(2) mustBe MessageLookup.English.emailKey
          summaryListKeys(3) mustBe MessageLookup.English.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.English.businessNameValue
          summaryListValues(1) mustBe MessageLookup.English.addressValue
          summaryListValues(2) mustBe MessageLookup.English.emailValue
          summaryListValues(3) mustBe MessageLookup.English.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.English.link2
          links(2).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }

        "there are pending changes to the contract details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, Some(pendingChangeOfDetails), isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.English.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading

          doc.select(".govuk-inset-text").text() mustBe MessageLookup.English.insetText(LocalDate.now)

          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.English.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.English.addressKey
          summaryListKeys(2) mustBe MessageLookup.English.emailKey
          summaryListKeys(3) mustBe MessageLookup.English.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.English.businessNameValue
          summaryListValues(1) mustBe MessageLookup.English.addressValue
          summaryListValues(2) mustBe MessageLookup.English.emailValue
          summaryListValues(3) mustBe MessageLookup.English.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.English.link2
          links(2).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }

        "there are no contact details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsEmpty, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.English.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.English.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.English.addressKey
          summaryListKeys(2) mustBe MessageLookup.English.emailKey
          summaryListKeys(3) mustBe MessageLookup.English.telephoneKey

          summaryListValues.head mustBe MessageLookup.English.noneValue
          summaryListValues(1) mustBe MessageLookup.English.noneValue
          summaryListValues(2) mustBe MessageLookup.English.noneValue
          summaryListValues(3) mustBe MessageLookup.English.noneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.English.link1
          links(2).attributes().get("href") mustBe "/agent-services-account/manage-account/contact-details/start-update"

          links(3).text() mustBe MessageLookup.English.link2
          links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }
      }

      "the selected lang is welsh" when {
        val messages: Messages = MessagesImpl(langs.last, messagesApi)
        "there are contact details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.Welsh.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.Welsh.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.Welsh.addressKey
          summaryListKeys(2) mustBe MessageLookup.Welsh.emailKey
          summaryListKeys(3) mustBe MessageLookup.Welsh.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.Welsh.businessNameValue
          summaryListValues(1) mustBe MessageLookup.Welsh.addressValue
          summaryListValues(2) mustBe MessageLookup.Welsh.emailValue
          summaryListValues(3) mustBe MessageLookup.Welsh.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.Welsh.link1
          links(2).attributes().get("href") mustBe "/agent-services-account/manage-account/contact-details/start-update"

          links(3).text() mustBe MessageLookup.Welsh.link2
          links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }
        "'update contact details' link hidden for standard user" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, None, isAdmin = false)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.Welsh.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.Welsh.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.Welsh.addressKey
          summaryListKeys(2) mustBe MessageLookup.Welsh.emailKey
          summaryListKeys(3) mustBe MessageLookup.Welsh.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.Welsh.businessNameValue
          summaryListValues(1) mustBe MessageLookup.Welsh.addressValue
          summaryListValues(2) mustBe MessageLookup.Welsh.emailValue
          summaryListValues(3) mustBe MessageLookup.Welsh.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.Welsh.link2
          links(2).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }

        "there are pending changes to the contract details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, Some(pendingChangeOfDetails), isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.Welsh.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.heading

          doc.select(".govuk-inset-text").text() mustBe MessageLookup.Welsh.insetText(LocalDate.now)

          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.Welsh.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.Welsh.addressKey
          summaryListKeys(2) mustBe MessageLookup.Welsh.emailKey
          summaryListKeys(3) mustBe MessageLookup.Welsh.telephoneKey

          summaryListValues.head.trim mustBe MessageLookup.Welsh.businessNameValue
          summaryListValues(1) mustBe MessageLookup.Welsh.addressValue
          summaryListValues(2) mustBe MessageLookup.Welsh.emailValue
          summaryListValues(3) mustBe MessageLookup.Welsh.telephoneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.Welsh.link2
          links(2).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }

        "there are no contact details" in {
          val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsEmpty, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

          doc.title() mustBe MessageLookup.Welsh.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.Welsh.heading
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

          summaryListKeys.head mustBe MessageLookup.Welsh.businessNameKey
          summaryListKeys(1) mustBe MessageLookup.Welsh.addressKey
          summaryListKeys(2) mustBe MessageLookup.Welsh.emailKey
          summaryListKeys(3) mustBe MessageLookup.Welsh.telephoneKey

          summaryListValues.head mustBe MessageLookup.Welsh.noneValue
          summaryListValues(1) mustBe MessageLookup.Welsh.noneValue
          summaryListValues(2) mustBe MessageLookup.Welsh.noneValue
          summaryListValues(3) mustBe MessageLookup.Welsh.noneValue

          val links = doc.select(".govuk-link").asScala.toList

          links(2).text() mustBe MessageLookup.Welsh.link1
          links(2).attributes().get("href") mustBe "/agent-services-account/manage-account/contact-details/start-update"

          links(3).text() mustBe MessageLookup.Welsh.link2
          links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
        }
      }
    }
  }

}
