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

import org.mockito.Mockito._
import play.api.Configuration
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.{AgentInfo, AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.models.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.stubs.AgentClientAuthorisationStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseUnitSpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, User}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AgentServicesControllerSpec extends BaseUnitSpec {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val externalUrls: ExternalUrls = mock[ExternalUrls]
  val signOutUrl = routes.SignOutController.signOut().url
  when(externalUrls.signOutUrlWithSurvey).thenReturn(signOutUrl)
  val mappingUrl = "http://example.com/agent-mapping/start"
  when(externalUrls.agentMappingUrl).thenReturn(mappingUrl)
  val invitationsUrl = "http://example.com/agent-invitations/agents"
  when(externalUrls.agentInvitationsUrl).thenReturn(invitationsUrl)
  val invitationsTrackUrl = "http://example.com/agent-invitations/track"
  when(externalUrls.agentInvitationsTrackUrl).thenReturn(invitationsTrackUrl)
  val agentAfiUrl = "http://example.com/agent-services/individuals"
  when(externalUrls.agentAfiUrl).thenReturn(agentAfiUrl)
  val agentCancelAuthUrl = "http://example.com/agent-invitations/cancel-authorisation"
  when(externalUrls.agentCancelAuthUrl).thenReturn(agentCancelAuthUrl)
  val arn = "TARN0000001"
  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", arn)), state = "Activated", delegatedAuthRule = None)

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  val authActions: AuthActions = new AuthActions(null, null, null, env, configuration) {
    override def withAuthorisedAsAgent(body: AgentInfo => Future[Result])(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] = {
      body(AgentInfo(Arn(arn), Some(User)))
    }
  }

  object NoPasscodeVerification extends PasscodeVerification {
    override def apply[A](body: Boolean => Future[Result])(implicit request: Request[A], headerCarrier: HeaderCarrier, ec: ExecutionContext) = body(true)
  }

  "root" should {
    "redirect to agent services account when suspension is disabled" in {
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", false)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to agent service account when suspension is enabled but user is not suspended" in {
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showAgentServicesAccount().url)
    }

    "redirect to suspended warning when suspension is enables and user is suspended" in {
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = true, Some(Set("ITSA"))))
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe SEE_OTHER
      redirectLocation(response) shouldBe Some(routes.AgentServicesController.showSuspendedWarning().url)
    }
  }

  "home" should {
    "return Status: OK and body containing correct content" in {
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)
      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
      content should include(messagesApi("agent.accountNumber","TARN 000 0001"))
      content should include(messagesApi("agent.services.account.inset"))
      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.col1.h3"))
      content should include(messagesApi("agent.services.account.section1.col1.link"))
      content should include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.h3"))
      content should include(messagesApi("agent.services.account.section1.col2.p"))
      content should include(htmlEscapedMessage("agent.services.account.section1.col2.link"))
      content should include(htmlEscapedMessage("agent.services.account.section2.h2"))
      content should include(htmlEscapedMessage("agent.services.account.section2.col1.p"))
      content should include(htmlEscapedMessage("agent.services.account.section2.col1.link"))
      content should include(agentAfiUrl)
      content should include(messagesApi("agent.services.account.section3.h2"))
      content should include(messagesApi("agent.services.account.section3.col1.h3"))
      content should include(messagesApi("agent.services.account.section3.col2.h3"))
      content should include(messagesApi("agent.services.account.section3.col2.p"))
      content should include("<p><a target=\"_blank\" href=\"https://www.tax.service.gov.uk/capital-gains-tax-uk-property/start\">How to manage your client’s account (opens in a new window or tab)</a></p>")
      content should include(messagesApi("agent.services.account.section3.col1.link1.href"))
      content should include("<p><a target=\"_blank\" href=\"\">What you’ll need to report the tax (opens in a new window or tab)</a></p>")
      content should include("<p><a target=\"_blank\" href=\"\">Manage a client’s Capital Gains Tax on UK property account</a></p>")

      content should include(messagesApi("agent.services.account.section4.h2"))
      content should include(messagesApi("agent.services.account.section4.col1.h3"))
      content should include(messagesApi("agent.services.account.section4.col1.p"))
      content should include(messagesApi("agent.services.account.section4.col1.link"))
      content should include(invitationsUrl)
      content should include(messagesApi("agent.services.account.section4.col2.h3"))
      content should include(messagesApi("agent.services.account.section4.col2.link1"))
      content should include(messagesApi("agent.services.account.section4.col2.link2"))
      content should include(htmlEscapedMessage("agent.services.account.section4.col2.link3"))
      content should include(invitationsTrackUrl)
      content should include(mappingUrl)
      content should include(agentCancelAuthUrl)
    }

    "return Status: OK and body containing correct content when suspension details are in the session and agent is suspended for VATC" in {

      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)
      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home").withSession("isSuspendedForVat" -> "true"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("agent.services.account.section1.h2"))
      content should include(messagesApi("agent.services.account.section1.suspended.h3"))
      content should include(messagesApi("agent.services.account.section1.suspended.p1"))
      content should include(messagesApi("agent.services.account.section1.suspended.p2"))

      content should not include(messagesApi("agent.services.account.section1.col1.h3"))
      content should not include(messagesApi("agent.services.account.section1.col1.link"))
      content should not include("https://www.gov.uk/guidance/sign-up-for-making-tax-digital-for-vat")
    }

    "return Status: OK and body containing correct content when agent suspension is not enabled" in {
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", false)
      val response = controller.showAgentServicesAccount()(FakeRequest("GET", "/home"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
    }

    "return the redirect returned by authActions when authActions denies access" in {

      implicit val externalUrls = new ExternalUrls(Configuration.from(Map())) {
        override lazy val agentSubscriptionUrl: String = "foo"
      }

      val authActions = new AuthActions(null, null, null, env, configuration) {
        override def withAuthorisedAsAgent(body: AgentInfo => Future[Result])(implicit ec: ExecutionContext, hc: HeaderCarrier, request: Request[AnyContent]): Future[Result] =
          Future.successful(Results.SeeOther("foo?continue=%2Fagent-services-account%3Fp%3DBAR1%2B23%252F"))
        }

      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)

      val response = controller.root()(FakeRequest("GET", "/").withSession(("otacTokenParam", "BAR1 23/")))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("foo?continue=%2Fagent-services-account%3Fp%3DBAR1%2B23%252F")
    }

    "do not fail without continue url parameter" in {
      givenSuspensionStatus(SuspensionDetails(suspensionStatus = false, None))

      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)
      val response = controller.showAgentServicesAccount().apply(FakeRequest("GET", "/home"))
      status(response) shouldBe OK
      contentAsString(response) should {
        not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
      }
    }
  }

  "showSuspensionWarning" should {
    "return Ok and show the suspension warning page" in {
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)

      val response = controller.showSuspendedWarning()(FakeRequest("GET", "/home").withSession("suspendedServices" -> "HMRC-MTD-IT,HMRC-MTD-VAT"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)

      content should include(messagesApi("suspension-warning.header"))
      content should include(messagesApi("suspension-warning.p1"))
      content should include(messagesApi("suspension-warning.p2.multi"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-IT"))
      content should include(messagesApi("suspension-warning.multi.HMRC-MTD-VAT"))
      content should include(messagesApi("suspension-warning.p3"))
      content should include(htmlEscapedMessage("suspension-warning.p4"))
      content should include(messagesApi("suspension-warning.button"))
    }
  }

  "manage-account" should {

    "return Status: OK and body containing correct content" in {
      val controller = new AgentServicesController(authActions, agentClientAuthorisationConnector, NoPasscodeVerification, "", true)

      val response = controller.manageAccount().apply(FakeRequest("GET", "/manage-account"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include(messagesApi("manage.account.heading"))
      content should include(messagesApi("manage.account.p"))
      content should include(messagesApi("manage.account.add-user"))
      content should include(messagesApi("manage.account.manage-user-access"))

    }
  }
}
