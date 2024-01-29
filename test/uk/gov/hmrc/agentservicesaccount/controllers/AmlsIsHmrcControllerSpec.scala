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


import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.MimeTypes.HTML
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout, stubMessagesControllerComponents}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, SuspensionDetails}
import uk.gov.hmrc.agentservicesaccount.actions.Actions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs.givenSuspensionStatus
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs.givenOptinRecordExistsForArn
import uk.gov.hmrc.agentservicesaccount.stubs.AuthStubs
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.views.html.pages.AMLS.is_amls_hmrc
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future


class AmlsIsHmrcControllerSpec extends UnitSpec with AuthStubs {

  private val cc: MessagesControllerComponents = stubMessagesControllerComponents()
  trait Setup {

    protected val appConfig: AppConfig = mock[AppConfig]
    protected val actions: Actions = mock[Actions]
    protected val view: is_amls_hmrc = mock[is_amls_hmrc]

    object TestController
      extends AmlsIsHmrcController(actions, view, cc)(appConfig)
  }

  private val arn = "BARN1234567"

  private def fakeRequest(method: String = "GET", uri: String = "/") =
    FakeRequest(method, uri)
      .withSession(SessionKeys.authToken -> "Bearer XYZ")
      .withSession(SessionKeys.sessionId -> "session-x")


  "showAmlsIsHMRC" should {
    "return Ok and show the 'is AMLS body HMRC?' page" in new Setup {
      givenAuthorisedAsAgentWith(arn)
      givenOptinRecordExistsForArn(Arn(arn), exists = false)
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val response: Future[Result] = TestController.showAmlsIsHmrc(fakeRequest())
      status(response) shouldBe OK
      Helpers.contentType(response).get shouldBe HTML
      val html: Document = Jsoup.parse(contentAsString(await(response)))

      html.title shouldBe "not this"
      html.select("h1") shouldBe "not this 2"
    }
  }

  "submitAmlsIsHmrc" should {
    "redirect to [not-implemented-hmrc-page]" in new Setup {
      givenAuthorisedAsAgentWith(arn)

      val response: Future[Result] = TestController.submitAmlsIsHmrc(
        fakeRequest("POST")
          .withFormUrlEncodedBody("accept" -> "true")
      )
      status(response) shouldBe SEE_OTHER

    }

    "redirect to capture-new-amls-details" in new Setup {
      givenAuthorisedAsAgentWith(arn)

      val response: Future[Result] = TestController.submitAmlsIsHmrc(
        fakeRequest("POST")
          .withFormUrlEncodedBody("accept" -> "false")
      )
      status(response) shouldBe SEE_OTHER

    }

    "return form with errors" in new Setup {
      givenAuthorisedAsAgentWith(arn)

      val response: Future[Result] = TestController.submitAmlsIsHmrc(fakeRequest("POST") /* with no form body */)

      status(response) shouldBe OK
    }
  }
}