/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.stubs.AgentPermissionsStubs.givenHideBetaInviteResponse
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}
import uk.gov.hmrc.http.SessionKeys


class BetaInviteControllerSpec extends BaseISpec {

  val controller: BetaInviteController = app.injector.instanceOf[BetaInviteController]
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  implicit val mockSessionCacheService: SessionCacheService = app.injector.instanceOf[SessionCacheService]
  implicit val mockSessionCacheRepo: SessionCacheRepository = app.injector.instanceOf[SessionCacheRepository]

  override implicit lazy val app: Application =
    appBuilder(Map("mongodb.uri" -> s"mongodb://localhost:27017/test-BetaInviteControllerSpec"))
      .build()

  def getRequest(path: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", path)
    .withHeaders("Authorization" -> "Bearer XYZ")
    .withSession(SessionKeys.authToken -> "Bearer XYZ")
    .withSession(SessionKeys.sessionId -> "session-x")

  def postRequestNoBody(path: String): FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", path)
    .withHeaders("Authorization" -> "Bearer XYZ")
    .withSession(SessionKeys.authToken -> "Bearer XYZ")
    .withSession(SessionKeys.sessionId -> "session-x")

  val arn = "TARN0000001"
  val agentEnrolment: Enrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  "POST hide invite" should {
    "redirect to home and decline beta invite" in {
      givenAuthorisedAsAgentWith(arn)
      givenHideBetaInviteResponse()

      implicit val request: FakeRequest[AnyContentAsEmpty.type] =
        postRequestNoBody("/private-beta-invite/decline")

      val result = await(controller.hideInvite(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/agent-services-account/home")
    }
  }

  s"GET ${routes.BetaInviteController.showInvite.url}" should {
    "render yes no radio" in {
      givenAuthorisedAsAgentWith(arn)

      val result = await(controller.showInvite.apply(getRequest("/private-beta-testing")))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Access groups feature testing - Agent services account - GOV.UK"
      // TODO add checks on view
    }
  }

  s"POST ${routes.BetaInviteController.submitInvite().url}" should {
    "redirect to ASA home if no" in {
      givenAuthorisedAsAgentWith(arn)
      givenHideBetaInviteResponse()

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing")
          .withFormUrlEncodedBody("accept" -> "false")

      val result = await(controller.submitInvite()(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/agent-services-account/home")
    }

    s"redirect to ${routes.BetaInviteController.showInviteDetails.url} if yes" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing")
          .withFormUrlEncodedBody("accept" -> "true")

      val result = await(controller.submitInvite().apply(request))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/agent-services-account/private-beta-testing-details")
    }

    s"render ${routes.BetaInviteController.showInvite.url} if errors" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing")
          .withFormUrlEncodedBody("bad" -> "req")

      val result = await(controller.submitInvite().apply(request))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Error: Access groups feature testing - Agent services account - GOV.UK"
    }
  }

  "GET showInviteDetails" should {
    "render number of clients radio" in {
      givenAuthorisedAsAgentWith(arn)

      val result = await(controller.showInviteDetails.apply(getRequest("/private-beta-testing-details")))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "How many clients do you manage using your agent services account? - Agent services account - GOV.UK"
    }
  }

  "POST showInviteDetails" should {
    "redirect to show contact details" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing-details")
          .withFormUrlEncodedBody("size" -> "small")

      val result = await(controller.submitInviteDetails().apply(req))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/agent-services-account/private-beta-testing-contact-details")
    }

    "error if no option selected" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val req: FakeRequest[AnyContentAsEmpty.type] =
        postRequestNoBody("/private-beta-testing-details")

      val result = await(controller.submitInviteDetails().apply(req))
      //then
      status(result) shouldBe OK
      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Error: How many clients do you manage using your agent services account? - Agent services account - GOV.UK"
    }
  }

  "GET showInviteContactDetails" should {
    "render form for contact details" in {
      givenAuthorisedAsAgentWith(arn)

      val result = await(controller.showInviteContactDetails.apply(getRequest("/private-beta-testing-contact-details")))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Contact details - Agent services account - GOV.UK"
    }
  }

  "POST submit invite contact details" should {
    "redirect to check your answers" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing-contact-details")
          .withFormUrlEncodedBody("name" -> "Fang", "email" -> "a@s.a")

      val result = await(controller.submitInviteContactDetails().apply(req))
      //then
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/agent-services-account/private-beta-check-your-answers")
    }

    "render form with errors" in {
      givenAuthorisedAsAgentWith(arn)

      implicit val req: FakeRequest[AnyContentAsFormUrlEncoded] =
        postRequestNoBody("/private-beta-testing-contact-details")
          .withFormUrlEncodedBody("name" -> "Fang", "email" -> "bAD")

      val result = await(controller.submitInviteContactDetails().apply(req))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Error: Contact details - Agent services account - GOV.UK"
    }
  }


  "GET showInviteCheckYourAnswers" should {
    "render check your answers page" in {
      givenAuthorisedAsAgentWith(arn)

      val result = await(controller.showInviteCheckYourAnswers.apply(getRequest("/private-beta-check-your-answers")))
      //then
      status(result) shouldBe OK

      val html = Jsoup.parse(contentAsString(result))
      html.title() shouldBe "Check your answers - Agent services account - GOV.UK"
    }
  }

}
