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
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AgentRequest, AuthActions, SignOutUrl}
import views.html.helper.urlEncode

class AgentServicesControllerSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val ggSignOutBaseUrl = "http://gg-sign-in-host:1234"
  val ggSignOutPath = "/blah/sign-out"
  val ggSignOutContinueUrl = "http://www.example.com/foo"
  val completeGgSignOutUrl = s"$ggSignOutBaseUrl$ggSignOutPath?continue=${urlEncode(ggSignOutContinueUrl)}"

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "authentication.government-gateway.sign-out.base-url" -> ggSignOutBaseUrl,
        "authentication.government-gateway.sign-out.path" -> ggSignOutPath,
        "authentication.government-gateway.sign-out.continue-url" -> ggSignOutContinueUrl
      )
      .build()

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val signOutUrl: SignOutUrl = app.injector.instanceOf[SignOutUrl]


  "root" should {
    "return Status: OK and body containing correct content" in {
      val arn = "TARN0000001"
      val authActions = new AuthActions(null, null, null) {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action.async { implicit request =>
            body(AgentRequest(Arn(arn), request))
          }
      }

      val controller = new AgentServicesController(messagesApi, authActions, signOutUrl)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("agent.services.account.heading.summary"))
      content should include(messagesApi("agent.services.account.subHeading"))
      content should include(messagesApi("agent.services.account.subHeading.summary"))
      content should include(arn)
      content should include(completeGgSignOutUrl)
    }

    "return the redirect returned by authActions when authActions denies access" in {
      val authActions = new AuthActions(null, null, null) with Results {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action { implicit request =>
            Redirect("/gg/sign-in", 303)
          }
      }

      val controller = new AgentServicesController(messagesApi, authActions, signOutUrl)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("/gg/sign-in")

    }
  }
}


