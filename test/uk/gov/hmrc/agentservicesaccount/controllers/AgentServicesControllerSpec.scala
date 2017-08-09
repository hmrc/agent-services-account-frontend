/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AgentRequest, AuthActions}

class AgentServicesControllerSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val mockConfig: Configuration = app.injector.instanceOf[Configuration]


  "AgentServicesController" should {
    "return Status: OK and body should contain correct content" in {
      val authActions = new AuthActions(null, null, null) {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action.async { implicit request =>
            body(AgentRequest(Arn("TARN0000001"), request))
          }
      }

      val controller = new AgentServicesController(messagesApi, mockConfig, authActions)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      contentAsString(response) should include(messagesApi("agent.services.account.heading"))
      contentAsString(response) should include(messagesApi("agent.services.account.heading.summary"))
      contentAsString(response) should include(messagesApi("agent.services.account.subHeading"))
      contentAsString(response) should include(messagesApi("agent.services.account.subHeading.summary"))
      contentAsString(response) should include("ARN123098-12")
    }

    "return the redirect returned by authActions when authActions denies access" in {
      val authActions = new AuthActions(null, null, null) with Results {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action { implicit request =>
            Redirect("/gg/sign-in", 303)
          }
      }

      val controller = new AgentServicesController(messagesApi, mockConfig, authActions)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("/gg/sign-in")

    }
  }
}


