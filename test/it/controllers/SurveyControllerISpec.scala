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

package it.controllers

import play.api.test.FakeRequest
import play.api.test.Helpers
import play.api.test.Helpers._
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.SurveyController
import uk.gov.hmrc.agentservicesaccount.controllers.routes

class SurveyControllerISpec
extends BaseISpec {

  val controller: SurveyController = inject[SurveyController]

  "GET /home/survey" should {
    "show the survey type form" in {
      val result = controller.showSurvey(FakeRequest("GET", "/"))
      status(result) shouldBe 200
      Helpers.contentAsString(result).contains("Feedback") shouldBe true
      Helpers
        .contentAsString(result)
        .contains("We will ask you some questions about the service you used.") shouldBe true
    }
  }

  "POST /home/survey" should {
    "redirect to feedback frontend survey with the selected type of journey" in {
      val result = controller.submitSurvey(FakeRequest("POST", "/").withFormUrlEncodedBody("surveyKey" -> "AGENTSUB"))
      status(result) shouldBe 303
      Helpers.redirectLocation(result).get shouldBe appConfig.signOutUrlWithSurvey("AGENTSUB")
    }
    "redirect to select which service form if choosing 'accessing a service'" in {
      val result = controller.submitSurvey(FakeRequest("POST", "/").withFormUrlEncodedBody("surveyKey" -> "ACCESSINGSERVICE"))
      status(result) shouldBe 303
      Helpers.redirectLocation(result).get shouldBe routes.SurveyController.showWhichService().url
    }
  }

  "GET /home/survey-service" should {
    "show the tax service selection form" in {
      val result = controller.showWhichService(FakeRequest("GET", "/"))
      status(result) shouldBe 200
      Helpers.contentAsString(result).contains("Which tax service do you want to give us feedback about?") shouldBe true
    }
  }

  "POST /home/survey-service" should {
    "from 'which service' page, redirect to survey" in {
      val result = controller.submitWhichService(FakeRequest("POST", "/").withFormUrlEncodedBody("service" -> "VAT"))
      status(result) shouldBe 303
      Helpers.redirectLocation(result).get shouldBe appConfig.signOutUrlWithSurvey("VATCA")
    }
    "return bad request if missing survey body" in {
      val result = controller.submitSurvey(FakeRequest("POST", "/"))
      status(result) shouldBe 400
    }
  }

}
