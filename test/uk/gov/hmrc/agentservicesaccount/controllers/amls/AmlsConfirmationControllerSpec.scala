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

package uk.gov.hmrc.agentservicesaccount.controllers.amls

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.MimeTypes.HTML
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs.givenSuspensionStatus
import uk.gov.hmrc.agentservicesaccount.stubs.AuthStubs
import uk.gov.hmrc.agentservicesaccount.support.{UnitSpec, WireMockSupport}
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future


class AmlsConfirmationControllerSpec extends UnitSpec with AuthStubs with GuiceOneAppPerSuite with WireMockSupport {

  class Setup(isEnabled: Boolean) {
    def application(isEnabled: Boolean): Application = new GuiceApplicationBuilder().configure(
      "features.enable-non-hmrc-supervisory-body" -> isEnabled,
      "auditing.enabled" -> false,
      "microservice.services.auth.port" -> wireMockPort,
      "microservice.services.agent-client-authorisation.port" -> wireMockPort,
      "microservice.services.agent-permissions.port" -> wireMockPort,
      "microservice.services.agent-user-client-details.port" -> wireMockPort,
      "microservice.services.agent-permissions-frontend.external-url" -> wireMockBaseUrlAsString,
      "metrics.enabled" -> false
    ).build()

    implicit val lang: Lang = Lang("en")
    implicit val messagesApi: MessagesApi = application(isEnabled).injector.instanceOf[MessagesApi]
    val controller: AmlsConfirmationController = application(isEnabled).injector.instanceOf[AmlsConfirmationController]
    implicit val appConfig: AppConfig = application(isEnabled).injector.instanceOf[AppConfig]
    val arn = "TARN0000001"

    private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  }

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri)
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withSession(SessionKeys.sessionId -> "session-x")


  "showAmlsDetailsUpdatedConfirmation" should {
    "return Ok and show the confirmation page for AMLS details updated" when {
      "the non-hmrc-supervisory-body feature switch is enabled" in new Setup(isEnabled = true) {
        givenAuthorisedAsAgentWith(arn)
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

        val response: Future[Result] = controller.showUpdatedAmlsConfirmationPage(fakeRequest("GET", "/home"))
        status(response) shouldBe OK
        Helpers.contentType(response).get shouldBe HTML
        val content = Helpers.contentAsString(response)

        content should include(messagesApi("generic.title", messagesApi("amls.confirmation.h1"), messagesApi("service.name")))
        content should include(messagesApi("amls.confirmation.h1"))
        content should include(messagesApi("common.what.happens.next"))
        content should include(messagesApi("amls.confirmation.p1"))
        content should include(messagesApi("amls.confirmation.link"))
      }
      "the non-hmrc-supervisory-body feature switch is disabled" in new Setup(isEnabled = false) {
        givenAuthorisedAsAgentWith(arn)
        givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

        val response: Future[Result] = controller.showUpdatedAmlsConfirmationPage(fakeRequest("GET", "/home"))
        status(response) shouldBe FORBIDDEN
      }
    }
  }
}