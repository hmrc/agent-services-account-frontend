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

import play.api.http.Status.OK
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.stubs.SessionServiceMocks
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.SessionKeys
import play.api.http.MimeTypes.HTML


import scala.concurrent.Future




  class AmslJourneyControllerSpec extends BaseISpec with SessionServiceMocks {
    implicit val lang: Lang = Lang("en")
    implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
    val controller: AmlsJourneyController = app.injector.instanceOf[AmlsJourneyController]
    implicit lazy val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]
    implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    val arn = "TARN0000001"

    private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

    private def fakeRequest(method: String = "GET", uri: String = "/") =
      FakeRequest(method, uri)
        .withSession(SessionKeys.authToken -> "Bearer XYZ")
        .withSession(SessionKeys.sessionId -> "session-x")


    "showAmlsDetailsUpdatedConfirmation" should {
      "return Ok and show the confirmation page for AMLS details updated" in {
        givenAuthorisedAsAgentWith(arn)

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
    }
  }

