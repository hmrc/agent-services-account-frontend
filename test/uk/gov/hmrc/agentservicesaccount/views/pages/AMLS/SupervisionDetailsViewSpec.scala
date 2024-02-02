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

package uk.gov.hmrc.agentservicesaccount.views.pages.AMLS

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.supervision_details

import java.time.LocalDate

class SupervisionDetailsViewSpec extends PlaySpec with GuiceOneAppPerSuite {

  private val ukAMLSDetails = AmlsDetails(
    "HMRC",
    Some("123456789"),
    Some("safeId"),
    Some("bprSafeId"),
    Some(LocalDate.of(2022, 1, 25)),
    Some(LocalDate.of(2023, 12, 7))
  )

  private val overseasAMLSDetails = AmlsDetails("notHMRC")

  private val messagesControllerComponentsForView: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  private val messagesApi: Messages = messagesControllerComponentsForView.messagesApi.preferred(Seq(Lang("en")))
  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private val suspensionDetailsView: supervision_details = app.injector.instanceOf[supervision_details]
  private val fakeRequest = FakeRequest()

  "supervision_details view" when {

    def testStaticContent(doc: Document): Unit = {
      "have the correct page title" in {
        doc.select("title").first.text() mustBe "Money laundering supervision details - Agent services account - GOV.UK"
      }
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
        doc.select(".govuk-back-link").first.attr("href") mustBe "/agent-services-account/manage-account"
      }
      "have the correct h1 heading" in {
        doc.select("h1").first.text() mustBe "Money laundering supervision details"
      }
      "have the correct h2 heading" in {
        doc.select("h2").first.text() mustBe "Keep your details up to date"
      }
      "have the correct paragraph text" in {
        doc.select("p").first.text() mustBe
          "Tell us when you renew your money laundering supervision registration each year. You can do this once your new registration is approved."
      }
      "have the correct update button" in {
        doc.select(".govuk-button").first.text() mustBe "Update money laundering supervision details"
      }
    }

    "rendering content for UK AMLS details" should {
      val html: Html = suspensionDetailsView(ukAMLSDetails)(messagesApi, fakeRequest, appConfig)
      val doc = Jsoup.parse(html.body)

      testStaticContent(doc)

      "display the name of the supervisory body correctly in the first row" in {
        doc.select("dt").get(0).text() mustBe "Name of money laundering supervisory body"
        doc.select("dd").get(0).text() mustBe ukAMLSDetails.supervisoryBody
      }

      "display the registration number correctly in the second row" in {
        doc.select("dt").get(1).text() mustBe "Your registration number"
        doc.select("dd").get(1).text() mustBe ukAMLSDetails.membershipNumber.get
      }

      "display the renewal date correctly in the third row" in {
        doc.select("dt").get(2).text() mustBe "Your next registration renewal date"
        doc.select("dd").get(2).text() mustBe "07/12/2023"
      }
    }

    "rendering content for Overseas AMLS details" should {
      val html: Html = suspensionDetailsView(overseasAMLSDetails)(messagesApi, fakeRequest, appConfig)
      val doc = Jsoup.parse(html.body)

      testStaticContent(doc)

      "display the name of the supervisory body correctly in the first row" in {
        doc.select("dt").get(0).text() mustBe "Name of money laundering supervisory body"
        doc.select("dd").get(0).text() mustBe overseasAMLSDetails.supervisoryBody
      }

      "display the registration number correctly in the second row" in {
        doc.select("dt").get(1).text() mustBe "Your registration number"
        doc.select("dd").get(1).text() mustBe "Not provided"
      }

      "display the renewal date correctly in the third row" in {
        doc.select("dt").get(2).text() mustBe "Your next registration renewal date"
        doc.select("dd").get(2).text() mustBe "Not provided"
      }
    }

  }

}
