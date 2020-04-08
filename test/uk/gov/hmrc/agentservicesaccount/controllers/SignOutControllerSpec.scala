/*
 * Copyright 2020 HM Revenue & Customs
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

import akka.stream.Materializer
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.{Application, Configuration}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.play.test.UnitSpec

class SignOutControllerSpec extends UnitSpec with GuiceOneAppPerSuite with MockitoSugar{

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()

  implicit val externalUrls: ExternalUrls = mock[ExternalUrls]
  implicit val requestHeader = RequestHeader
  implicit val config: Configuration = mock[Configuration]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val materializer: Materializer = app.materializer

  "SignOutController" should {
    "remove session and redirect to /home/survey" in {
      val signOutController = new SignOutController()(externalUrls,config, messagesApi)
      val signOutUrl = "/agent-services-account/home/survey"
      when(externalUrls.signOutUrlWithSurvey("key")).thenReturn(signOutUrl)

      val request = signOutController.signOut(FakeRequest("GET","/")).withSession("otacTokenParam" -> "token")

      status(request) shouldBe 303
      request.header.headers.get("otacTokenParam") shouldBe empty
    }

    "show the sign out form" in {
      val signOutController = new SignOutController()(externalUrls, config, messagesApi)

      val result = signOutController.showSurvey(FakeRequest("GET", "/"))

      status(result) shouldBe 200
      bodyOf(result).toString.contains("Feedback") shouldBe true
    }

    "redirect to survey" in {
      val signOutController = new SignOutController()(externalUrls, config, messagesApi)
      val signOutUrl = "http://example.com/gg/sign-out?continue=http://example.com/go-here-after-sign-outAGENTSUB"

      when(externalUrls.signOutUrlWithSurvey("AGENTSUB")).thenReturn(signOutUrl)

      val result = signOutController.submitSurvey(FakeRequest("POST", "/").withFormUrlEncodedBody("surveyKey" -> "AGENTSUB"))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe externalUrls.signOutUrlWithSurvey("AGENTSUB")
    }

    "/signed-out redirect to GG sign in with continue url back to /agent-services-account" in {
      val signOutController = new SignOutController()(externalUrls, config, messagesApi)
      val signedOutUrl = "http://example.com/gg/sign-out?continue=http://example.com/go-here-after-sign-out"
      when(externalUrls.continueFromGGSignIn).thenReturn(signedOutUrl)

      val request = signOutController.signedOut(FakeRequest("GET","/")).withSession("otacTokenParam" -> "token")

      status(request) shouldBe 303
      redirectLocation(request).get shouldBe externalUrls.continueFromGGSignIn
      request.header.headers.get("otacTokenParam") shouldBe empty
    }
  }

}
