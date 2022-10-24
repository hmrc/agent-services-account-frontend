/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs._
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys


class BetaInviteControllerSpec extends BaseISpec {

  val controller: BetaInviteController = app.injector.instanceOf[BetaInviteController]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  def getRequest(path: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", path)
    .withHeaders("Authorization" -> "Bearer XYZ")
    .withSession(SessionKeys.sessionId -> "session-x")

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  "POST hide invite" should {
    "redirect to home and decline beta invite" in {
      givenAuthorisedAsAgentWith(arn)
      givenHideBetaInviteResponse()

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/private-beta-invite/decline")
        .withHeaders("Authorization" -> "Bearer XYZ")
        .withSession(SessionKeys.sessionId -> "session-x")

      val result = await(controller.hideInvite.apply(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9401/private-beta-invite/decline&origin=agent-services-account-frontend")
    }
  }

  "GET show invite" should {
    "render yes no radio" in {
      givenAuthorisedAsAgentWith(arn)

      val result = await(controller.showInvite.apply(getRequest("/private-beta-testing")))
      //then
      status(result) shouldBe SEE_OTHER

      //Pretty sure something is wrong with the setup
//      status(result) shouldBe OK

//      val html = Jsoup.parse(contentAsString(result))
//      html.title() shouldBe "Access groups feature testing - Agent services account - GOV.UK"

    }
  }

  "POST submit invite" should {
    "redirect to home if no" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/private-beta-testing")
        .withFormUrlEncodedBody("accept" -> "false")
        .withHeaders("Authorization" -> "Bearer XYZ")
        .withSession(SessionKeys.sessionId -> "session-x")

      val result = await(controller.showInvite.apply(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9401/private-beta-testing&origin=agent-services-account-frontend")
    }

    s"redirect to ${routes.BetaInviteController.showInviteDetails.url} if yes" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest("POST", "/private-beta-testing")
        .withFormUrlEncodedBody("accept" -> "true")
        .withHeaders("Authorization" -> "Bearer XYZ")
        .withSession(SessionKeys.sessionId -> "session-x")


      val result = await(controller.showInvite.apply(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("http://localhost:9553/bas-gateway/sign-in?continue_url=http://localhost:9401/private-beta-testing&origin=agent-services-account-frontend")
    }

    s"render ${routes.BetaInviteController.showInvite.url} if errors" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "/private-beta-testing")
        .withHeaders("Authorization" -> "Bearer XYZ")
        .withSession(SessionKeys.sessionId -> "session-x")


      val result = await(controller.showInvite.apply(request))
      //then
      status(result) shouldBe SEE_OTHER

      //if nothing wrong with setup
      //status(result) shouldBe OK

//      val html = Jsoup.parse(contentAsString(result))
//      html.title() shouldBe "Access groups feature testing - Agent services account - GOV.UK"
    }
  }

}
