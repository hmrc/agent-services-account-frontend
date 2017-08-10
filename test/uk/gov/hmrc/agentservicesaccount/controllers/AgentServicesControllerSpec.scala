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

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AgentRequest, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls

class AgentServicesControllerSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val externalUrls: ExternalUrls = mock[ExternalUrls]
  val signOutUrl = "http://example.com/gg/sign-out?continue=http://example.com/go-here-after-sign-out"
  when(externalUrls.signOutUrl).thenReturn(signOutUrl)


  "root" should {
    "return Status: OK and body containing correct content" in {
      val arn = "TARN0000001"
      val authActions = new AuthActions(null, null, null) {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action.async { implicit request =>
            body(AgentRequest(Arn(arn), request))
          }
      }

      val controller = new AgentServicesController(messagesApi, authActions, externalUrls)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include("Agent Services account")
      content should not include("Agent Services Account")
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("agent.services.account.heading.summary"))
      content should include(arn)
      content should include(signOutUrl)
    }

    "return the redirect returned by authActions when authActions denies access" in {
      val authActions = new AuthActions(null, null, null) with Results {
        override def AuthorisedWithAgentAsync(body: AsyncPlayUserRequest): Action[AnyContent] =
          Action { implicit request =>
            Redirect("/gg/sign-in", 303)
          }
      }

      val controller = new AgentServicesController(messagesApi, authActions, externalUrls)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("/gg/sign-in")
    }
  }
}


