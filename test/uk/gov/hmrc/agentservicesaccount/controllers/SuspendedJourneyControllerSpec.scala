/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.controllers

import org.jsoup.Jsoup
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers.defaultAwaitTimeout
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.SessionKeys
import play.api.http.MimeTypes.HTML
import play.api.mvc.Result
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs.givenSuspensionStatus

import scala.concurrent.Future

class SuspendedJourneyControllerSpec extends BaseISpec {

  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: SuspendedJourneyController = app.injector.instanceOf[SuspendedJourneyController]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val arn = "TARN0000001"

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  private def fakeRequest(method: String = "GET", uri: String = "/") = FakeRequest(method, uri).withSession(SessionKeys.authToken -> "Bearer XYZ")
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response = controller.showSuspendedWarning()(fakeRequest("GET", "/home"))

      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)

      content should include(messagesApi("suspension-warning.header1"))
      content should include(messagesApi("suspension-warning.p1"))
      content should include(messagesApi("suspension-warning.p2"))
      content should include(messagesApi("suspension-warning.p5"))
      content should include(messagesApi("suspension-warning.list1"))
      content should include(messagesApi("suspension-warning.list2"))
      content should include(messagesApi("suspension-warning.list3"))
      content should include(htmlEscapedMessage("suspension-warning.p4"))
      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2Fhome"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
      val continueLink = Jsoup.parse(content).select(Css.linkStyledAsButton)
      continueLink.attr("href") shouldBe "/agent-services-account/recovery-contact-details"
      continueLink.text shouldBe "Continue"
      val signoutLink = Jsoup.parse(content).select(Css.signoutLink)
      signoutLink.attr("href") shouldBe "/agent-services-account/signed-out"
      signoutLink.text shouldBe "Return to Government Gateway sign in"
    }

    "redirect to home page when the agent is not suspended" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response = controller.showSuspendedWarning()(fakeRequest())

      status(response) shouldBe SEE_OTHER
      redirectLocation(response.futureValue) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }
  }
  "showContactDetails" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))
      val response = controller.showContactDetails()(fakeRequest("GET", "/home"))
      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("suspend.contact-details.invite.details.label1"))
      content should include(messagesApi("suspend.contact-details.invite.details.label2"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3.hint"))
      content should include(messagesApi("suspend.contact-details.invite.details.label4"))
      content should include(messagesApi("suspend.contact-details.invite.details.heading"))


      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2Fhome"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
  }
  "submitContactDetails" should {
    "return Bad request and show error messages if the data is wrong" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))
      val response: Future[Result] = controller.submitContactDetails()(fakeRequest("POST", "/home"))

      status(response) shouldBe BAD_REQUEST
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("suspend.contact-details.invite.details.label1"))
      content should include(messagesApi("suspend.contact-details.invite.details.label2"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3.hint"))
      content should include(messagesApi("suspend.contact-details.invite.details.label4"))
      content should include(messagesApi("suspend.contact-details.invite.details.label4"))
//      content should include(messagesApi("error.required.name"))
//      content should include(messagesApi("error.required.email"))


      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2Fhome"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
    "return SEE_OTHER  if the data is correct" in {
      givenAuthorisedAsAgentWith(arn)
      val request = fakeRequest("POST", "/")

        .withFormUrlEncodedBody(
          "name" -> "colm",
        "email" -> "colm@colm.com",
          "phone" -> "01234542")
      val response = controller.submitContactDetails()(request)

      status(response) shouldBe SEE_OTHER


    }
  }
}
