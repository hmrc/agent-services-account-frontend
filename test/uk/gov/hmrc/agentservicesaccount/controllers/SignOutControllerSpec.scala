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

package uk.gov.hmrc.agentservicesaccount.controllers

import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import sttp.model.Uri.UriContext

class SignOutControllerSpec
extends BaseISpec {

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val controller: SignOutController = app.injector.instanceOf[SignOutController]

  def signOutUrlWithContinue(continue: String): String = {
    val signOutBaseUrl = "http://localhost:9099"
    val signOutPath = "/bas-gateway/sign-out-without-state"
    uri"""${signOutBaseUrl + signOutPath}?${Map("continue" -> continue)}""".toString
  }

  "GET /sign-out" should {
    "remove session and redirect to /home/survey" in {
      val continueUrl = uri"${appConfig.asaFrontendExternalUrl + "/agent-services-account/home/survey"}"
      val response = controller.signOut(fakeRequest("GET", "/")).futureValue
      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some(signOutUrlWithContinue(continueUrl.toString))
    }
  }

  "GET /signed-out" should {
    "redirect to sign in with continue url back to /agent-services-account" in {
      val request = controller.signedOut(FakeRequest("GET", "/"))
      status(request) shouldBe 303
      Helpers.redirectLocation(request) shouldBe Some(signOutUrlWithContinue(appConfig.continueFromGGSignIn))
    }
  }

  "GET /online/sign-in" should {
    "remove session and redirect to HMRC Online sign-in page" in {
      val onlineSignInUrl = "https://www.access.service.gov.uk/login/signin/creds"
      val response = controller.onlineSignIn(fakeRequest("GET", "/"))
      status(response) shouldBe 303
      Helpers.redirectLocation(response) shouldBe Some(signOutUrlWithContinue(onlineSignInUrl))
    }
  }

  "GET /time-out" should {
    "redirect to bas-gateway-frontend/sign-out-without-state with timed out page as continue" in {
      val continue = uri"${appConfig.asaFrontendExternalUrl + routes.SignOutController.timedOut().url}"
      val response = controller.timeOut()(fakeRequest())
      status(response) shouldBe 303
      Helpers.redirectLocation(response) shouldBe Some(signOutUrlWithContinue(continue.toString))
    }
  }

  "GET /timed-out" should {
    "should show the timed out page" in {
      val request = controller.timedOut(FakeRequest("GET", "/"))
      status(request) shouldBe 403
    }
  }

}
