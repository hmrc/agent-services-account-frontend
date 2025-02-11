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

package uk.gov.hmrc.agentservicesaccount.views.pages.amls.partials

import org.jsoup.Jsoup
import play.api.i18n._
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.amls.partials.amls_details_summary_list

import java.time.LocalDate

class AmlsDetailsSummaryListViewSpec extends BaseISpec {


  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val  lang: Lang = Lang("en")
  val view: amls_details_summary_list  = app.injector.instanceOf[amls_details_summary_list]
  val messages: Messages = MessagesImpl(lang, messagesApi)


  "amls_details_summary_list" should {

    "display full amls registered details " in {

      val aDate = LocalDate.parse("2024-12-07")

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("1234"), membershipExpiresOn = Some(aDate))
      val doc = Jsoup.parse(view.apply(amlsDetails)(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 3
      summaryListValues.size() shouldBe 3

      summaryListKeys.get(0).text() shouldBe "Supervisory body"
      summaryListValues.get(0).text() shouldBe "HMRC"

      summaryListKeys.get(1).text() shouldBe "Registration number"
      summaryListValues.get(1).text() shouldBe "1234"

      summaryListKeys.get(2).text() shouldBe "Next renewal date"
      summaryListValues.get(2).text() shouldBe "8 December 2024"
    }

    "display amls details without a renewal date" in {

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("1234"), membershipExpiresOn = None)
      val doc = Jsoup.parse(view.apply(amlsDetails)(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 2
      summaryListValues.size() shouldBe 2

      summaryListKeys.get(0).text() shouldBe "Supervisory body"
      summaryListValues.get(0).text() shouldBe "HMRC"

      summaryListKeys.get(1).text() shouldBe "Registration number"
      summaryListValues.get(1).text() shouldBe "1234"

    }

    "display amls details when supervisory body is the only available field" in {

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = None, membershipExpiresOn = None)
      val doc = Jsoup.parse(view.apply(amlsDetails)(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 1
      summaryListValues.size() shouldBe 1

      summaryListKeys.get(0).text() shouldBe "Supervisory body"
      summaryListValues.get(0).text() shouldBe "HMRC"
    }

    "display pending amls details" in {

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("1234"), membershipExpiresOn = None)
      val doc = Jsoup.parse(view.apply(amlsDetails, Some("Pending"))(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 3
      summaryListValues.size() shouldBe 3

      summaryListKeys.get(0).text() shouldBe "Supervisory body"
      summaryListValues.get(0).text() shouldBe "HMRC"

      summaryListKeys.get(1).text() shouldBe "Registration number"
      summaryListValues.get(1).text() shouldBe "1234"

      summaryListKeys.get(2).text() shouldBe "Registration status"
      summaryListValues.get(2).text() shouldBe "Pending"
    }

    "display pending rejected amls details" in {

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("1234"), membershipExpiresOn = None)
      val doc = Jsoup.parse(view.apply(amlsDetails, Some("Rejected"))(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 3
      summaryListValues.size() shouldBe 3

      summaryListKeys.get(0).text() shouldBe "Supervisory body"
      summaryListValues.get(0).text() shouldBe "HMRC"

      summaryListKeys.get(1).text() shouldBe "Registration number"
      summaryListValues.get(1).text() shouldBe "1234"

      summaryListKeys.get(2).text() shouldBe "Registration status"
      summaryListValues.get(2).text() shouldBe "Rejected"
    }

    "display dates in Welsh when cy is the lang" in {

      val aDate = LocalDate.parse("2024-12-07")

      val lang: Lang = Lang("cy")
      val view: amls_details_summary_list  = app.injector.instanceOf[amls_details_summary_list]
      val messages: Messages = MessagesImpl(lang, messagesApi)

      val amlsDetails = AmlsDetails(supervisoryBody = "HMRC", membershipNumber = Some("1234"), membershipExpiresOn = Some(aDate))
      val doc = Jsoup.parse(view.apply(amlsDetails)(messages).body)

      val summaryListKeys = doc.select(".govuk-summary-list__key")
      val summaryListValues = doc.select(".govuk-summary-list__value")

      summaryListKeys.size() shouldBe 3
      summaryListValues.size() shouldBe 3

      summaryListKeys.get(2).text() shouldBe "Dyddiad nesaf ar gyfer adnewyddu"
      summaryListValues.get(2).text() shouldBe "8 Rhagfyr 2024"

    }
  }

}


