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
import play.api.i18n.Messages
import play.api.i18n.MessagesImpl
import support.TestConstants
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.summaryPdf
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr

import java.time.Instant
import scala.jdk.CollectionConverters.CollectionHasAsScala

class SummaryPdfPageSpec
extends ViewBaseSpec
with TestConstants {

  private val view: summaryPdf = inject[summaryPdf]

  private val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress(
      "Test Street",
      Some("Test Town"),
      None,
      None,
      Some("TE5 7ED"),
      "GB"
    ))
  )

  private val testArn = Arn("XXARN0123456789")
  private val testUtr = Utr("XXUTR12345667")

  private val selectChanges1: Set[String] = Set("businessName")
  private val selectChanges2: Set[String] = Set("email", "telephone")
  private val selectChangesAll: Set[String] = Set(
    "businessName",
    "address",
    "email",
    "telephone"
  )

  private val fullOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = true,
      saAgentReference = Some(SaUtr("A2345"))
    ),
    ctChanges = CtChanges(
      applyChanges = true,
      ctAgentReference = Some(CtUtr("C2345"))
    )
  )

  private val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
  )

  private val pendingChangeOfDetails1 = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyName = testAgencyDetailsFull.agencyName),
    otherServices = emptyOtherServices,
    timeSubmitted = Instant.now,
    submittedBy = submittedByDetails
  )
  private val pendingChangeOfDetails2 = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyEmail = testAgencyDetailsFull.agencyEmail, agencyTelephone = testAgencyDetailsFull.agencyTelephone),
    otherServices = emptyOtherServices,
    timeSubmitted = Instant.now,
    submittedBy = submittedByDetails
  )
  private val pendingChangeOfDetailsAll = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = testAgencyDetailsFull,
    otherServices = fullOtherServices,
    timeSubmitted = Instant.now,
    submittedBy = submittedByDetails
  )

  object MessageLookup {

    object English {

      val heading: String = "Request to amend contact details"
      val title: String = "Request to amend contact details"

      val addressKey: String = "Address for agent services account"
      val emailKey: String = "Email address"
      val telephoneKey: String = "Telephone number"
      val businessNameKey: String = "Business name shown to clients"

      val oldAddressValue: String = "25 Any Street Central Grange Telford TF4 3TR GB"
      val newAddressValue: String = "Test Street Test Town TE5 7ED GB"

      val otherServicesHeading: String = "Update other services"
      val otherServicesApplySAKey: String = "Apply changes to Self Assessment"
      val otherServicesSACodeKey: String = "Self Assessment agent code"
      val otherServicesApplyCTKey: String = "Apply changes to Corporation Tax"
      val otherServicesCTCodeKey: String = "Corporation Tax agent code"

      val otherServicesApplySAValue: String = "No"
      val otherServicesApplyCTValue: String = "No"

      val fullOtherServicesApplySAValue: String = "Yes"
      val fullOtherServicesApplyCTValue: String = "Yes"

      val otherServicesSACodeValue: String = "A2345"
      val otherServicesCTCodeValue: String = "C2345"

      val yourDetailsHeading: String = "Your details"
      val yourDetailsNameKey: String = "Full name"
      val yourDetailsTelephoneKey: String = "Telephone number"

      val yourDetailsNameValue: String = "John Tester"
      val yourDetailsTelephoneValue: String = "01903 209919"

    }

  }

  "first viewing page" should {

    "render correctly with all populated fields" when {
      val doc: Document = Jsoup.parse(view.apply(
        Some(testUtr),
        pendingChangeOfDetails1,
        selectChanges = selectChanges1
      )(
        messages,
        fakeRequest,
        appConfig
      ).body)

      "display the correct page title" in {
        doc.title() mustBe "Request to amend contact details"
      }

      "display the correct page h1" in {
        doc.select("h1").first.text() mustBe "Request to amend contact details"
      }

      "display the correct page h2" in {
        doc.select("h2").first.text() mustBe "Business details"
        doc.select("h2").get(1).text() mustBe "Existing contact details"
        doc.select("h2").get(2).text() mustBe "New contact details"
        doc.select("h2").get(3).text() mustBe "Other services to be amended with same details"
        doc.select("h2").get(4).text() mustBe "User’s contact details"
      }

      "display the correct page paragraph" in {
        doc.select("p").first().text() mustBe s"Unique Taxpayer Reference: ${testUtr.value}"
        doc.select("p").get(1).text() mustBe s"Agent reference number: ${testArn.value}"
      }
    }
  }

  "check_updated_details" should {
    "render correctly with businessName as the only updated answer" when {
      val messages: Messages = MessagesImpl(lang, messagesApi)
      val doc: Document = Jsoup.parse(view.apply(
        Some(testUtr),
        pendingChangeOfDetails1,
        selectChanges = selectChanges1
      )(
        messages,
        fakeRequest,
        appConfig
      ).body)

      "businessName is the only selected update" in {

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.English.businessNameKey
        summaryListValues.head mustBe pendingChangeOfDetails1.oldDetails.agencyName.get

        summaryListKeys(1) mustBe MessageLookup.English.businessNameKey
        summaryListValues(1) mustBe pendingChangeOfDetails1.newDetails.agencyName.get

        summaryListKeys(2) mustBe MessageLookup.English.otherServicesApplySAKey
        summaryListValues(2) mustBe MessageLookup.English.otherServicesApplySAValue
        summaryListKeys(3) mustBe MessageLookup.English.otherServicesApplyCTKey
        summaryListValues(3) mustBe MessageLookup.English.otherServicesApplyCTValue

        summaryListKeys(4) mustBe MessageLookup.English.yourDetailsNameKey
        summaryListValues(4) mustBe MessageLookup.English.yourDetailsNameValue
        summaryListKeys(5) mustBe MessageLookup.English.yourDetailsTelephoneKey
        summaryListValues(5) mustBe MessageLookup.English.yourDetailsTelephoneValue

      }
    }
    "render correctly with email and telephone as the only updated answers" when {
      val messages: Messages = MessagesImpl(lang, messagesApi)
      val doc: Document = Jsoup.parse(view.apply(
        Some(testUtr),
        pendingChangeOfDetails2,
        selectChanges = selectChanges2
      )(
        messages,
        fakeRequest,
        appConfig
      ).body)

      "email and telephone are the only selected changes" in {

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.English.emailKey
        summaryListValues.head mustBe pendingChangeOfDetails2.oldDetails.agencyEmail.get
        summaryListKeys(1) mustBe MessageLookup.English.telephoneKey
        summaryListValues(1) mustBe pendingChangeOfDetails2.oldDetails.agencyTelephone.get

        summaryListKeys(2) mustBe MessageLookup.English.emailKey
        summaryListValues(2) mustBe pendingChangeOfDetails2.newDetails.agencyEmail.get
        summaryListKeys(3) mustBe MessageLookup.English.telephoneKey
        summaryListValues(3) mustBe pendingChangeOfDetails2.newDetails.agencyTelephone.get

        summaryListKeys(4) mustBe MessageLookup.English.otherServicesApplySAKey
        summaryListValues(4) mustBe MessageLookup.English.otherServicesApplySAValue
        summaryListKeys(5) mustBe MessageLookup.English.otherServicesApplyCTKey
        summaryListValues(5) mustBe MessageLookup.English.otherServicesApplyCTValue

        summaryListKeys(6) mustBe MessageLookup.English.yourDetailsNameKey
        summaryListValues(6) mustBe MessageLookup.English.yourDetailsNameValue
        summaryListKeys(7) mustBe MessageLookup.English.yourDetailsTelephoneKey
        summaryListValues(7) mustBe MessageLookup.English.yourDetailsTelephoneValue

      }
    }
  }

  "render correctly with all contact details and agent codes updated" when {
    val messages: Messages = MessagesImpl(lang, messagesApi)
    val doc: Document = Jsoup.parse(view.apply(
      Some(testUtr),
      pendingChangeOfDetailsAll,
      selectChanges = selectChangesAll
    )(
      messages,
      fakeRequest,
      appConfig
    ).body)

    "all contact details and agent codes have been selected" in {

      doc.title() mustBe MessageLookup.English.title
      doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
      val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
      val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)

      summaryListKeys.head mustBe MessageLookup.English.businessNameKey
      summaryListValues.head mustBe pendingChangeOfDetailsAll.oldDetails.agencyName.get
      summaryListKeys(1) mustBe MessageLookup.English.addressKey
      summaryListValues(1) mustBe MessageLookup.English.oldAddressValue
      summaryListKeys(2) mustBe MessageLookup.English.emailKey
      summaryListValues(2) mustBe pendingChangeOfDetailsAll.oldDetails.agencyEmail.get
      summaryListKeys(3) mustBe MessageLookup.English.telephoneKey
      summaryListValues(3) mustBe pendingChangeOfDetailsAll.oldDetails.agencyTelephone.get

      summaryListKeys(4) mustBe MessageLookup.English.businessNameKey
      summaryListValues(4) mustBe pendingChangeOfDetailsAll.newDetails.agencyName.get
      summaryListKeys(5) mustBe MessageLookup.English.addressKey
      summaryListValues(5) mustBe MessageLookup.English.newAddressValue
      summaryListKeys(6) mustBe MessageLookup.English.emailKey
      summaryListValues(6) mustBe pendingChangeOfDetailsAll.newDetails.agencyEmail.get
      summaryListKeys(7) mustBe MessageLookup.English.telephoneKey
      summaryListValues(7) mustBe pendingChangeOfDetailsAll.newDetails.agencyTelephone.get

      summaryListKeys(8) mustBe MessageLookup.English.otherServicesApplySAKey
      summaryListValues(8) mustBe MessageLookup.English.fullOtherServicesApplySAValue
      summaryListKeys(9) mustBe MessageLookup.English.otherServicesSACodeKey
      summaryListValues(9) mustBe MessageLookup.English.otherServicesSACodeValue
      summaryListKeys(10) mustBe MessageLookup.English.otherServicesApplyCTKey
      summaryListValues(10) mustBe MessageLookup.English.fullOtherServicesApplyCTValue
      summaryListKeys(11) mustBe MessageLookup.English.otherServicesCTCodeKey
      summaryListValues(11) mustBe MessageLookup.English.otherServicesCTCodeValue

      summaryListKeys(12) mustBe MessageLookup.English.yourDetailsNameKey
      summaryListValues(12) mustBe MessageLookup.English.yourDetailsNameValue
      summaryListKeys(13) mustBe MessageLookup.English.yourDetailsTelephoneKey
      summaryListValues(13) mustBe MessageLookup.English.yourDetailsTelephoneValue

    }
  }

}
