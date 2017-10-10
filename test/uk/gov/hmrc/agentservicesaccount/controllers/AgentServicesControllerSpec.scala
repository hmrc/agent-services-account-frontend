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

import java.net.URLEncoder

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc.{ActionBuilder, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.GuiceModule
import uk.gov.hmrc.agentservicesaccount.auth.{AgentRequest, AuthActions}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.connectors.SsoConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import play.twirl.api.HtmlFormat
import scala.concurrent.Future


class AgentServicesControllerSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach with UnitSpec{

  override implicit lazy val app: Application = appBuilder
    .build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().overrides(new GuiceModule(){
      override def configure(): Unit = {
        bind(classOf[SsoConnector]).toInstance(new SsoConnector(null, null) {
          val whitelistedSSODomains = Set("www.foo.com", "foo.org")

          override def validateExternalDomain(domain: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
            Future.successful(whitelistedSSODomains.contains(domain))
          }
        })
      }
    })



  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val externalUrls: ExternalUrls = mock[ExternalUrls]
  val signOutUrl = "http://example.com/gg/sign-out?continue=http://example.com/go-here-after-sign-out"
  when(externalUrls.signOutUrl).thenReturn(signOutUrl)
  val mappingUrl = "http://example.com/agent-mapping/start"
  when(externalUrls.agentMappingUrl).thenReturn(mappingUrl)
  val arn = "TARN0000001"

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])
  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  val authActions = new AuthActions(null, null, null) {
    override def AuthorisedWithAgentAsync = new ActionBuilder[AgentRequest] {
      override def invokeBlock[A](request: Request[A], block: (AgentRequest[A]) => Future[Result]): Future[Result] = {
        block(AgentRequest(Arn(arn), request))
      }
    }
  }

  lazy val continueUrlActions: ContinueUrlActions = app.injector.instanceOf[ContinueUrlActions]

  "root" should {
    "return Status: OK and body containing correct content" in {

      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include("Agent Services account")
      content should not include "Agent Services Account"
      content should include(messagesApi("agent.services.account.heading"))
      content should include(htmlEscapedMessage(messagesApi("agent.services.account.heading.summary")))
      content should include(messagesApi("agent.services.account.additional.links.title"))
      content should include(messagesApi("agent.services.account.additional.links.mapping.body1"))
      content should include(messagesApi("agent.services.account.additional.links.mapping.body2", mappingUrl, "agentMappingLinkId"))
      content should include(arn)
      content should include(signOutUrl)
      content should include(mappingUrl)
    }

    "return the redirect returned by authActions when authActions denies access" in {

      val authActions = new AuthActions(null, null, null) {
        override def AuthorisedWithAgentAsync = new ActionBuilder[AgentRequest] {
          override def invokeBlock[A](request: Request[A], block: (AgentRequest[A]) => Future[Result]): Future[Result] = {
            Future.successful(Redirect("/gg/sign-in", 303))
          }
        }
      }

      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("/gg/sign-in")
    }

    "do not fail without continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", "/"))
      status(response) shouldBe OK
      contentType(response).get should not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
    }

    "support relative continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", "/?continue=/foo"))
      status(response) shouldBe OK
      contentAsString(response) should {
        include ("<a href=\"/foo\" class=\"btn button\" id=\"continue\">")
      }
    }

    "support absolute localhost continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", "/?continue=http://localhost/foobar/"))
      status(response) shouldBe OK
      contentAsString(response) should {
        include ("<a href=\"http://localhost/foobar/\" class=\"btn button\" id=\"continue\">")
      }
    }

    "support absolute www.tax.service.gov.uk continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", s"/?continue=${URLEncoder.encode("http://www.tax.service.gov.uk/foo/bar?some=true", "UTF-8")}"))
      status(response) shouldBe OK
      contentAsString(response) should {
        include ("<a href=\"http://www.tax.service.gov.uk/foo/bar?some=true\" class=\"btn button\" id=\"continue\">")
      }
    }

    "support whitelisted absolute external continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", s"/?continue=${URLEncoder.encode("http://www.foo.com/bar?some=false", "UTF-8")}"))
      status(response) shouldBe OK
      contentAsString(response) should {
        include ("<a href=\"http://www.foo.com/bar?some=false\" class=\"btn button\" id=\"continue\">")
      }
    }

    "silently reject not-whitelisted absolute external continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, continueUrlActions, externalUrls)
      val response = controller.root().apply(FakeRequest("GET", s"/?continue=${URLEncoder.encode("http://www.foo.org/bar?some=false", "UTF-8")}"))
      status(response) shouldBe OK
      contentAsString(response) should {
        not include "<a href=\"http://www.foo.org/bar?some=false\" class=\"btn button\" id=\"continue\">"
      }
    }



  }
}
