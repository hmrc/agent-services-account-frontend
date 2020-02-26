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

import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.support.BaseUnitSpec
import uk.gov.hmrc.play.language.LanguageUtils
import play.api.test.Helpers.cookies
import scala.concurrent.duration._

class AgentServicesLanguageControllerSpec extends BaseUnitSpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val externalUrls: ExternalUrls = app.injector.instanceOf[ExternalUrls]
  val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]

  val timeout = 3.seconds

  implicit val request = FakeRequest()

  val controller = new AgentServicesLanguageController(configuration,languageUtils,messagesApi, externalUrls)

  "Calling the .switchToLanguage function" when {

    "providing the parameter 'english'" should {

      val result = await(controller.switchToLanguage("english")(request))

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "use the English language" in {
        cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
      }
    }
  }

    "providing the parameter 'cymraeg'" should {

      val result = controller.switchToLanguage("cymraeg")(request)

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "use the Welsh language" in {
        cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "cy"
      }
    }

    "providing an unsupported language parameter" should {

      controller.switchToLanguage("english")(FakeRequest())
      lazy val result = controller.switchToLanguage("orcish")(request)

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "keep the current language" in {
        cookies(result)(timeout).get("PLAY_LANG").get.value shouldBe "en"
      }
    }
}
