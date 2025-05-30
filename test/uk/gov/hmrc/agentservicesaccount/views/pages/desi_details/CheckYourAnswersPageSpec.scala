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
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, OtherServices, SaChanges, YourDetails}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.check_updated_details
import uk.gov.hmrc.domain.{CtUtr, SaUtr}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class CheckYourAnswersPageSpec extends BaseISpec {

  implicit private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit private val langs: Seq[Lang] = Seq(Lang("en"), Lang("cy"))

  private val view: check_updated_details = app.injector.instanceOf[check_updated_details]

  private val selectChanges1: Set[String] = Set("businessName")
  private val selectChanges2: Set[String] = Set("email", "telephone")
  private val selectChangesAll: Set[String] = Set("businessName", "address", "email", "telephone")

  private val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB"))
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

  object MessageLookup {

    object English {
      private val govUkSuffix: String = " - Agent services account - GOV.UK"
      val heading: String = "Check your answers"
      val title: String = heading + govUkSuffix

      val insetText: String = {
        "It takes 4 weeks for HMRC to apply these changes to your agent services account. " +
          "During that time, you cannot amend the contact details again."
      }

      val selectChangesKey: String = "What you want to change"
      val addressKey: String = "Address for agent services account"
      val emailKey: String = "Email address"
      val telephoneKey: String = "Telephone number"
      val businessNameKey: String = "Business name shown to clients"

      val addressValue: String = "Test Street Test Town TE5 7ED GB"
      val emailValue: String = "test@email.com"
      val telephoneValue: String = "01234 567890"
      val businessNameValue: String = "Test Name"

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

      val declarationText1: String = "I am authorised to make changes to the business contact details."
      val declarationText2: String = "I am a director, company secretary, sole trader, proprietor or partner in the " +
        "business, or I have permission from someone in one of those roles."

      val labelMap: Map[String, String] = Map(
        "businessName" -> businessNameKey,
        "address" -> addressKey,
        "email" -> emailKey,
        "telephone" -> telephoneKey
      )
    }

  }

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

  "check_updated_details" should {
      "render correctly with businessName as the only updated answer" when {
        val messages: Messages = MessagesImpl(langs.head, messagesApi)
        val doc: Document = Jsoup.parse(view.apply(
          agencyDetails = testAgencyDetailsFull,
          isAdmin = true,
          otherServices = emptyOtherServices,
          submittedBy = submittedByDetails,
          selectChanges = selectChanges1
        )(messages, FakeRequest(), appConfig).body)

        testServiceStaticContent(doc)

        "businessName is the only selected update" in {

          doc.title() mustBe MessageLookup.English.title
          doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
          doc.select(".govuk-inset-text").text() mustBe MessageLookup.English.insetText
          val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
          val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)
          val selectedChanges = doc.select(".govuk-summary-list__value > ul > li").asScala.toList.map(_.text).toSet

          summaryListKeys.head mustBe MessageLookup.English.selectChangesKey
          selectedChanges mustBe selectChanges1.map(key => MessageLookup.English.labelMap(key))
          summaryListKeys(1) mustBe MessageLookup.English.businessNameKey
          summaryListValues(1) mustBe MessageLookup.English.businessNameValue

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
      val messages: Messages = MessagesImpl(langs.head, messagesApi)
      val doc: Document = Jsoup.parse(view.apply(
        agencyDetails = testAgencyDetailsFull,
        isAdmin = true,
        otherServices = emptyOtherServices,
        submittedBy = submittedByDetails,
        selectChanges = selectChanges2
      )(messages, FakeRequest(), appConfig).body)

      testServiceStaticContent(doc)

      "email and telephone are the only selected changes" in {

        doc.title() mustBe MessageLookup.English.title
        doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
        doc.select(".govuk-inset-text").text() mustBe MessageLookup.English.insetText
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)
        val selectedChanges = doc.select(".govuk-summary-list__value > ul > li").asScala.toList.map(_.text).toSet

        summaryListKeys.head mustBe MessageLookup.English.selectChangesKey
        selectedChanges mustBe selectChanges2.map(key => MessageLookup.English.labelMap(key))
        summaryListKeys(1) mustBe MessageLookup.English.emailKey
        summaryListValues(1) mustBe MessageLookup.English.emailValue
        summaryListKeys(2) mustBe MessageLookup.English.telephoneKey
        summaryListValues(2) mustBe MessageLookup.English.telephoneValue

        summaryListKeys(3) mustBe MessageLookup.English.otherServicesApplySAKey
        summaryListValues(3) mustBe MessageLookup.English.otherServicesApplySAValue
        summaryListKeys(4) mustBe MessageLookup.English.otherServicesApplyCTKey
        summaryListValues(4) mustBe MessageLookup.English.otherServicesApplyCTValue

        summaryListKeys(5) mustBe MessageLookup.English.yourDetailsNameKey
        summaryListValues(5) mustBe MessageLookup.English.yourDetailsNameValue
        summaryListKeys(6) mustBe MessageLookup.English.yourDetailsTelephoneKey
        summaryListValues(6) mustBe MessageLookup.English.yourDetailsTelephoneValue

      }
    }
  }

  "render correctly with all contact details and agent codes updated" when {
    val messages: Messages = MessagesImpl(langs.head, messagesApi)
    val doc: Document = Jsoup.parse(view.apply(
      agencyDetails = testAgencyDetailsFull,
      isAdmin = true,
      otherServices = fullOtherServices,
      submittedBy = submittedByDetails,
      selectChanges = selectChangesAll
    )(messages, FakeRequest(), appConfig).body)

    testServiceStaticContent(doc)

    "all contact details and agent codes have been selected" in {

      doc.title() mustBe MessageLookup.English.title
      doc.select("h1").asScala.head.text mustBe MessageLookup.English.heading
      doc.select(".govuk-inset-text").text() mustBe MessageLookup.English.insetText
      val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
      val summaryListValues = doc.select(".govuk-summary-list__value").asScala.toList.map(_.text)
      val selectedChanges = doc.select(".govuk-summary-list__value > ul > li").asScala.toList.map(_.text).toSet

      summaryListKeys.head mustBe MessageLookup.English.selectChangesKey
      selectedChanges mustBe selectChangesAll.map(key => MessageLookup.English.labelMap(key))
      summaryListKeys(1) mustBe MessageLookup.English.businessNameKey
      summaryListValues(1) mustBe MessageLookup.English.businessNameValue
      summaryListKeys(2) mustBe MessageLookup.English.addressKey
      summaryListValues(2) mustBe MessageLookup.English.addressValue
      summaryListKeys(3) mustBe MessageLookup.English.emailKey
      summaryListValues(3) mustBe MessageLookup.English.emailValue
      summaryListKeys(4) mustBe MessageLookup.English.telephoneKey
      summaryListValues(4) mustBe MessageLookup.English.telephoneValue
      
      summaryListKeys(5) mustBe MessageLookup.English.otherServicesApplySAKey
      summaryListValues(5) mustBe MessageLookup.English.fullOtherServicesApplySAValue
      summaryListKeys(6) mustBe MessageLookup.English.otherServicesSACodeKey
      summaryListValues(6) mustBe MessageLookup.English.otherServicesSACodeValue
      summaryListKeys(7) mustBe MessageLookup.English.otherServicesApplyCTKey
      summaryListValues(7) mustBe MessageLookup.English.fullOtherServicesApplyCTValue
      summaryListKeys(8) mustBe MessageLookup.English.otherServicesCTCodeKey
      summaryListValues(8) mustBe MessageLookup.English.otherServicesCTCodeValue

      summaryListKeys(9) mustBe MessageLookup.English.yourDetailsNameKey
      summaryListValues(9) mustBe MessageLookup.English.yourDetailsNameValue
      summaryListKeys(10) mustBe MessageLookup.English.yourDetailsTelephoneKey
      summaryListValues(10) mustBe MessageLookup.English.yourDetailsTelephoneValue

    }
  }

}
