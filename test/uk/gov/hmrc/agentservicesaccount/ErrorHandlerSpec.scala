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

package uk.gov.hmrc.agentservicesaccount

import akka.stream.Materializer
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers
import uk.gov.hmrc.http.BadGatewayException
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentservicesaccount.support.{LogCapturing, UnitSpec}

import scala.concurrent.Future

class ErrorHandlerSpec extends UnitSpec with GuiceOneAppPerSuite with LogCapturing {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val handler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
  implicit val lang: Lang = Lang("en")
  implicit val mat: Materializer = app.injector.instanceOf[Materializer]

  "ErrorHandler should show the error page" when {
    "a server error occurs" in {
      withCaptureOfLoggingFrom(handler.theLogger) { logEvents =>
        val result = handler.onServerError(FakeRequest(), new BadGatewayException("some error"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        Helpers.contentType(result) shouldBe Some(HTML)
        checkIncludesMessages(result, "global.error.500.title", "global.error.500.heading", "global.error.500.message")

        logEvents.count(_.getMessage.contains(s"resolveError uk.gov.hmrc.http.BadGatewayException: some error")) shouldBe 1
      }
    }

    "a client error (400) occurs" in {
      withCaptureOfLoggingFrom(handler.theLogger) { logEvents =>
        val result = handler.onClientError(FakeRequest(), BAD_REQUEST, "some error")

        status(result) shouldBe BAD_REQUEST
        Helpers.contentType(result) shouldBe Some(HTML)
        checkHtmlResultWithBodyText(result.futureValue, "Bad request", "Please check that you have entered the correct web address.")

        logEvents.count(_.getMessage.contains(s"onClientError some error")) shouldBe 1
      }
    }

    "a client error (404) occurs" in {
      withCaptureOfLoggingFrom(handler.theLogger) { logEvents =>
        val result = handler.onClientError(FakeRequest(), NOT_FOUND, "some error")

        status(result) shouldBe NOT_FOUND
        Helpers.contentType(result) shouldBe Some(HTML)
        checkHtmlResultWithBodyText(result.futureValue, "This page can’t be found", "Please check that you have entered the correct web address.")

        logEvents.count(_.getMessage.contains(s"onClientError some error")) shouldBe 1
      }

    }
  }

  private def checkIncludesMessages(result: Future[Result], messageKeys: String*): Unit =
    messageKeys.foreach { messageKey =>
      messagesApi.isDefinedAt(messageKey) shouldBe true
      Helpers.contentAsString(result) should include(HtmlFormat.escape(messagesApi(messageKey)).toString)
    }

  private def checkHtmlResultWithBodyText(result: Result, expectedSubstrings: String*): Unit = {
    contentType(result) shouldBe Some("text/html")
    charset(result) shouldBe Some("utf-8")
    expectedSubstrings.foreach(s => bodyOf(result) should include(s))
  }
}
