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

import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentservicesaccount.support.{BaseISpec, Css}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.http.SessionKeys
import play.api.http.MimeTypes.HTML
import play.api.mvc.Result
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs.givenSuspensionStatus
import uk.gov.hmrc.agentservicesaccount.stubs.SessionServiceMocks

import scala.concurrent.Future

class SuspendedJourneyControllerSpec extends BaseISpec with SessionServiceMocks{
  implicit val lang: Lang = Lang("en")
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: SuspendedJourneyController = app.injector.instanceOf[SuspendedJourneyController]
  implicit lazy val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  val arn = "TARN0000001"

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  override def moduleWithOverrides = new AbstractModule() {
    override def configure(): Unit = {
      bind(classOf[SessionCacheService]).toInstance(mockSessionCacheService)
    }
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri)
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withSession(SessionKeys.sessionId -> "session-x")
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response = controller.showSuspendedWarning()(fakeRequest())

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
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2F"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
      val continueLink = Jsoup.parse(content).select(Css.linkStyledAsButton)
      continueLink.attr("href") shouldBe "/agent-services-account/recovery-contact-details"
      continueLink.text shouldBe "Continue"
      val signoutLink = Jsoup.parse(content).select(Css.signoutLink).get(1)
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
    "return Ok and show the contact details page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      getAllSessionItems(List(Some(""),Some(""),Some(""), Some("")))

      val response = controller.showContactDetails()(fakeRequest())
      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("suspend.contact-details.invite.details.label1"))
      content should include(messagesApi("suspend.contact-details.invite.details.label2"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3"))
      content should include(messagesApi("suspend.contact-details.invite.details.label3.hint"))
      content should include(messagesApi("suspend.contact-details.invite.details.heading"))


      val getHelpLink = Jsoup.parse(content).select(Css.getHelpWithThisPageLink)
      getHelpLink.attr("href") shouldBe "http://localhost:9250/contact/report-technical-problem?newTab=true&service=AOSS&referrerUrl=%2F"
      getHelpLink.text shouldBe "Is this page not working properly? (opens in new tab)"
    }
  }
  "submitContactDetails" should {
    "return Bad request and show error messages if the data is wrong" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))
      val response: Future[Result] = controller.submitContactDetails()(fakeRequest("POST", "/recovery-contact-details")

        .withFormUrlEncodedBody(
          "name" -> "",
          "email" -> " ",
          "phone" -> " ")
      )

      status(response) shouldBe BAD_REQUEST
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("error.suspended-details.required.name"))
      content should include(messagesApi("error.suspended-details.required.email"))
      content should include(messagesApi("error.suspended-details.required.telephone"))
    }
    "return SEE_OTHER  if the data is correct" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      expectPutSessionItem[String](NAME, "Romel")
      expectPutSessionItem[String](EMAIL, "romel@romel.com")
      expectPutSessionItem[String](PHONE, "01711111111")
      expectPutSessionItem[String](ARN, "TARN0000001")

      val request = fakeRequest("POST", "/recovery-contact-details")

        .withFormUrlEncodedBody(
          "name" -> "Romel",
        "email" -> "romel@romel.com",
          "phone" -> "01711111111")
      val response = controller.submitContactDetails()(request)

      status(response) shouldBe SEE_OTHER

      redirectLocation(await(response)) shouldBe Some(routes.SuspendedJourneyController.showSuspendedDescription().url)
    }
  }

  "showSuspendedDescription" should {
    "return Ok and show the description recovery page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, None))

      expectGetSessionItemNone[String](DESCRIPTION)

      val response: Future[Result] = controller.showSuspendedDescription()(fakeRequest())
      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML

      val content = Helpers.contentAsString(response)
      content should include(messagesApi("suspend.description.title"))
      content should include(messagesApi("suspend.description.hint1"))
      content should include(messagesApi("suspend.description.h1"))
      content should include(messagesApi("suspend.description.label"))
      content should include(messagesApi("suspend.description.hint"))
      content should include(messagesApi("common.continue-save"))
    }
  }

  "showSuspendedSummary" should {
    s"redirect to ${routes.SuspendedJourneyController.showContactDetails().url} when no details are present" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, None))

      expectGetSessionItemNone[String](NAME)
      expectGetSessionItemNone[String](EMAIL)
      expectGetSessionItemNone[String](PHONE)
      expectGetSessionItemNone[String](DESCRIPTION)
      expectGetSessionItemNone[String](ARN)

      val response: Future[Result] = controller.showSuspendedSummary()(fakeRequest())
      status(response) shouldBe 303
      redirectLocation(await(response)) shouldBe Some(routes.SuspendedJourneyController.showContactDetails().url)
    }
    s"return Ok when session details are found" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, None))

      expectGetSessionItem[String](NAME, "Romel", 1)
      expectGetSessionItem[String](EMAIL, "Romel@romel.com", 1)
      expectGetSessionItem[String](PHONE, "017111111111", 1)
      expectGetSessionItem[String](DESCRIPTION, "Some description", 1)
      expectGetSessionItem[String](ARN, "XARN000001122", 1)


      val response: Future[Result] = controller.showSuspendedSummary()(fakeRequest())
      status(response) shouldBe OK
    }
  }

  "submitSuspendedSummary" should {
    s"redirect to ${routes.SuspendedJourneyController.showContactDetails().url} when no summary details are present" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, None))

      expectGetSessionItemNone[String](NAME)
      expectGetSessionItemNone[String](EMAIL)
      expectGetSessionItemNone[String](PHONE)
      expectGetSessionItemNone[String](DESCRIPTION)
      expectGetSessionItemNone[String](ARN)

      val response: Future[Result] = controller.showSuspendedSummary()(fakeRequest())
      status(response) shouldBe 303
      redirectLocation(await(response)) shouldBe Some(routes.SuspendedJourneyController.showContactDetails().url)
    }
    s"redirect to ${routes.SuspendedJourneyController.showSuspendedConfirmation().url} when session details are found and send email" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, None))

      expectGetSessionItem[String](NAME, "Romel", 1)
      expectGetSessionItem[String](EMAIL, "Romel@romel.com", 1)
      expectGetSessionItem[String](PHONE, "017111111111", 1)
      expectGetSessionItem[String](DESCRIPTION, "Some description", 1)
      expectGetSessionItem[String](ARN, "XARN000001122", 1)


      val response: Future[Result] = controller.submitSuspendedSummary()(fakeRequest())
      status(response) shouldBe 303
      redirectLocation(await(response)) shouldBe Some(routes.SuspendedJourneyController.showSuspendedConfirmation().url)

    }
  }

  "showSuspendedConfirmation" should {
    "return Ok and show the confirmation page" in {
      givenAuthorisedAsAgentWith(arn)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV"))))

      val response: Future[Result] = controller.showSuspendedConfirmation()(fakeRequest("GET", "/home"))
      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val content = Helpers.contentAsString(response)
      content should include(messagesApi("suspend.confirmation.title"))
      content should include(messagesApi ("suspend.confirmation.h1"))
      content should include(messagesApi  ("common.what.happens.next"))
      content should include(messagesApi("suspend.confirmation.p1"))
      content should include(messagesApi ( "common.continue.gov"))
    }
  }
}
