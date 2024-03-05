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

package uk.gov.hmrc.agentservicesaccount.views.pages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.contact_details.view_contact_details

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class ViewContactDetailsViewSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val lang: Lang = Lang("en")

  private val view: view_contact_details = app.injector.instanceOf[view_contact_details]
  private val messages: Messages = MessagesImpl(lang, messagesApi)

  private val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB"))
  )

  private val testAgencyDetailsEmpty = AgencyDetails(None, None, None, None)

  private val testArn = Arn("XXARN0123456789")

  private val agencyDetails = AgencyDetails(
    agencyName = Some("My Agency"),
    agencyEmail = Some("abc@abc.com"),
    agencyTelephone = Some("07345678901"),
    agencyAddress = Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"))
  )

  private val pendingChangeOfDetails = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyName = Some("New and Improved Agency")),
    timeSubmitted = Instant.now
  )

  object MessageLookup {
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

    val link1 = "Update contact details"
    val link2 = "Return to Manage account"
  }

  //TODO create test constants file for reuse of the test variable


  "view_contact_details" should {
    "render correctly" when {
      "there are contact details" in {
        val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

        doc.title() mustBe MessageLookup.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.heading
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.businessNameKey
        summaryListKeys(1) mustBe MessageLookup.addressKey
        summaryListKeys(2) mustBe MessageLookup.emailKey
        summaryListKeys(3) mustBe MessageLookup.telephoneKey

        summaryListValues.head.trim mustBe MessageLookup.businessNameValue
        summaryListValues(1) mustBe MessageLookup.addressValue
        summaryListValues(2) mustBe MessageLookup.emailValue
        summaryListValues(3) mustBe MessageLookup.telephoneValue

        val links = doc.select(".govuk-link").asScala.toList

        links(2).text() mustBe MessageLookup.link1
        links(2).attributes().get("href") mustBe "/" //TODO - update to relevant route

        links(3).text() mustBe MessageLookup.link2
        links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
      }

      "there are pending changes to the contract details" in {
        val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsFull, Some(pendingChangeOfDetails), isAdmin = true)(messages, FakeRequest(), appConfig).body)

        doc.title() mustBe MessageLookup.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.heading

        doc.select(".govuk-inset-text").text() mustBe MessageLookup.insetText(LocalDate.now)

        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.businessNameKey
        summaryListKeys(1) mustBe MessageLookup.addressKey
        summaryListKeys(2) mustBe MessageLookup.emailKey
        summaryListKeys(3) mustBe MessageLookup.telephoneKey

        summaryListValues.head.trim mustBe MessageLookup.businessNameValue
        summaryListValues(1) mustBe MessageLookup.addressValue
        summaryListValues(2) mustBe MessageLookup.emailValue
        summaryListValues(3) mustBe MessageLookup.telephoneValue

        val links = doc.select(".govuk-link").asScala.toList

        links(2).text() mustBe MessageLookup.link1
        links(2).attributes().get("href") mustBe "/" //TODO - update to relevant route

        links(3).text() mustBe MessageLookup.link2
        links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
      }

      "there are no contact details" in {
        val doc: Document = Jsoup.parse(view.apply(testAgencyDetailsEmpty, None, isAdmin = true)(messages, FakeRequest(), appConfig).body)

        doc.title() mustBe MessageLookup.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.heading
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.businessNameKey
        summaryListKeys(1) mustBe MessageLookup.addressKey
        summaryListKeys(2) mustBe MessageLookup.emailKey
        summaryListKeys(3) mustBe MessageLookup.telephoneKey

        summaryListValues.head mustBe "None"
        summaryListValues(1) mustBe "None"
        summaryListValues(2) mustBe "None"
        summaryListValues(3) mustBe "None"

        val links = doc.select(".govuk-link").asScala.toList

        links(2).text() mustBe MessageLookup.link1
        links(2).attributes().get("href") mustBe "/" //TODO - update to relevant route

        links(3).text() mustBe MessageLookup.link2
        links(3).attributes().get("href") mustBe routes.AgentServicesController.manageAccount.url
      }
    }
  }

}
