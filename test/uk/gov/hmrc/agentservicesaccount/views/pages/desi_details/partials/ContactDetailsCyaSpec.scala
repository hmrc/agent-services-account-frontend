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

package uk.gov.hmrc.agentservicesaccount.views.pages.desi_details.partials

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.i18n.Lang
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.i18n.MessagesImpl
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.desi_details.partials.contact_details_cya_partial

import scala.jdk.CollectionConverters.CollectionHasAsScala

class ContactDetailsCyaSpec
extends BaseISpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private implicit val lang: Lang = Lang("en")
  val view: contact_details_cya_partial = app.injector.instanceOf[contact_details_cya_partial]
  implicit val messages: Messages = MessagesImpl(lang, messagesApi)

  private val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress(
      "Test & Street",
      Some("Test Town"),
      None,
      None,
      Some("TE5 7ED"),
      "GB"
    ))
  )

  private val selectChanges: Set[String] = Set(
    "businessName",
    "address",
    "email",
    "telephone"
  )

  object MessageLookup {

    object English {

      val addressKey: String = "Address for agent services account"
      val emailKey: String = "Email address"
      val telephoneKey: String = "Telephone number"
      val businessNameKey: String = "Business name shown to clients"

      val rawFirstLineAddressValue: String = "Test &amp; Street<br>"
      val addressValue: String = "Test & Street Test Town TE5 7ED GB"

      val emailValue: String = "test@email.com"
      val telephoneValue: String = "01234 567890"
      val businessNameValue: String = "Test Name"

      val labelMap: Map[String, String] = Map(
        "businessName" -> businessNameKey,
        "address" -> addressKey,
        "email" -> emailKey,
        "telephone" -> telephoneKey
      )

    }
  }

  "contact details partial" should {
    "render correctly with forPdf flag set to true" when {
      val doc: Document = Jsoup.parse(view.apply(
        testAgencyDetailsFull,
        selectChanges,
        forPdf = true
      )(messages, FakeRequest()).body)
      "The agency address contains special characters" in {
        val summaryListKeys = doc.select(".govuk-summary-list__key").asScala.toList.map(_.text)
        val summaryListValuesRaw = doc.select(".govuk-summary-list__value")
        val summaryListValues = summaryListValuesRaw.asScala.toList.map(_.text)

        summaryListKeys.head mustBe MessageLookup.English.businessNameKey
        summaryListValues.head mustBe MessageLookup.English.businessNameValue
        summaryListKeys(1) mustBe MessageLookup.English.addressKey
        summaryListValues(1) mustBe MessageLookup.English.addressValue

        // check ampersand is escaped before parsing
        summaryListValuesRaw.toString should include(MessageLookup.English.rawFirstLineAddressValue)
      }
    }
  }

}
