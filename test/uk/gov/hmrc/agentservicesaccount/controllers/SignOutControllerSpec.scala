/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.i18n.MessagesApi
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec

class SignOutControllerSpec extends BaseISpec {

  implicit val appConfig = app.injector.instanceOf[AppConfig]
  implicit val requestHeader = RequestHeader
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller = app.injector.instanceOf[SignOutController]

  "SignOutController" should {
    "remove session and redirect to /home/survey" in {
      val signOutUrl = "/agent-services-account/home/survey"

      val response = controller.signOut(FakeRequest("GET","/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some(signOutUrl)
    }

    "show the sign out form" in {

      val result = controller.showSurvey(FakeRequest("GET", "/"))

      status(result) shouldBe 200
      await(bodyOf(result)).contains("Feedback") shouldBe true
    }

    "redirect to survey" in {
      val result = controller.submitSurvey(FakeRequest("POST", "/").withFormUrlEncodedBody("surveyKey" -> "AGENTSUB"))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe appConfig.signOutUrlWithSurvey("AGENTSUB")
    }

    "return bad request if missing survey body" in {
      val result = controller.submitSurvey(FakeRequest("POST", "/"))

      status(result) shouldBe 400
    }

    "/signed-out redirect to GG sign in with continue url back to /agent-services-account" in {
      val request = controller.signedOut(FakeRequest("GET","/"))

      status(request) shouldBe 303
      redirectLocation(request) shouldBe Some(appConfig.continueFromGGSignIn)
    }

    "remove session and redirect to HMRC Online sign-in page" in {
      val onlineSignInUrl = "https://www.access.service.gov.uk/login/signin/creds"

      val response = controller.onlineSignIn(FakeRequest("GET","/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some(onlineSignInUrl)
    }

    "timedOut should return forbidden with new session" in {
      val request = controller.timedOut(FakeRequest("GET", "/"))

      status(request) shouldBe 403
    }

    "keepAlive should return OK" in {
      val response = controller.keepAlive(FakeRequest("GET", "/"))

      status(response) shouldBe 200
      await(bodyOf(response)).contains("OK") shouldBe true
    }
  }

}
