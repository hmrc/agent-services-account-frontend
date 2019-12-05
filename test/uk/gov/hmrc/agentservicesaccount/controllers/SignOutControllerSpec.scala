/*
 * Copyright 2019 HM Revenue & Customs
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
  implicit val messagesApi: MessagesApi = mock[MessagesApi]

  "SignOutController" should {
    "remove session and redirect to /gg/sign-out" in {
      val signOutController = new SignOutController()(externalUrls,config, messagesApi)
      val signOutUrl = "http://example.com/gg/sign-out?continue=http://example.com/go-here-after-sign-out"
      when(externalUrls.signOutUrlWithSurvey).thenReturn(signOutUrl)

      val request = signOutController.signOut(timeout = None)(FakeRequest("GET","/")).withSession("otacTokenParam" -> "token")

      status(request) shouldBe 303
      redirectLocation(request).get shouldBe externalUrls.signOutUrlWithSurvey
      request.header.headers.get("otacTokenParam") shouldBe empty
    }
  }

}
