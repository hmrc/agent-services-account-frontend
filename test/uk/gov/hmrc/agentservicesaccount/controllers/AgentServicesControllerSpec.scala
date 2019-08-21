/*
 * Copyright 2019 HM Revenue & Customs
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

import com.kenshoo.play.metrics.Metrics
import org.mockito.ArgumentMatchers.{any => anyArg, eq => eqArg}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.{BinderOption, GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Configuration, Environment}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.FrontendModule
import uk.gov.hmrc.agentservicesaccount.auth.{AgentInfo, AuthActions, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.config.ExternalUrls
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentServicesAccountConnector, SsoConnector}
import uk.gov.hmrc.auth.core.InvalidBearerToken
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}


class AgentServicesControllerSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach with UnitSpec {

  override implicit lazy val app: Application = appBuilder
    .build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(new GuiceableModule {
        override def guiced(env: Environment, conf: Configuration, binderOptions: Set[BinderOption]) = Seq(
          new FrontendModule(env, conf) {
            override def configure(): Unit = {

              bind(classOf[SsoConnector]).toInstance(new SsoConnector(null, null, new Metrics() {
                override def defaultRegistry = null

                override def toJson = null
              }) {
                val whitelistedSSODomains = Set("www.foo.com", "foo.org")

                override def validateExternalDomain(domain: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
                  Future.successful(whitelistedSSODomains.contains(domain))
                }
              })

              bind(classOf[AgentServicesAccountConnector]).toInstance(desConnector)
            }
          })

        override def disable(classes: Seq[Class[_]]) = this
      })


  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val externalUrls: ExternalUrls = mock[ExternalUrls]
  val signOutUrl = routes.SignOutController.signOut().url
  when(externalUrls.signOutUrl).thenReturn(signOutUrl)
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
  lazy val desConnector = mock[AgentServicesAccountConnector]
  when(desConnector.getAgencyName(eqArg(Arn(arn)))(anyArg[HeaderCarrier], anyArg[ExecutionContext])).thenReturn(Future.successful(None))

  private implicit val messages: Messages = messagesApi.preferred(Seq.empty[Lang])

  private implicit val configuration = app.injector.instanceOf[Configuration]

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  val authActions = new AuthActions(null, null, null) {

    override def authorisedWithAgent[A, R](body: AgentInfo => Future[R])(implicit headerCarrier: HeaderCarrier): Future[Option[R]] = {
      body(AgentInfo(Arn(arn), None)) map Option.apply
    }

  }

  object NoPasscodeVerification extends PasscodeVerification {
    override def apply[A](body: Boolean => Future[Result])(implicit request: Request[A], headerCarrier: HeaderCarrier, ec: ExecutionContext) = body(true)
  }

  "root" should {
    "return Status: OK and body containing correct content" in {
      val controller = new AgentServicesController(messagesApi, authActions, desConnector, NoPasscodeVerification, "")

      val response = controller.root()(FakeRequest("GET", "/"))

      status(response) shouldBe OK
      contentType(response).get shouldBe HTML
      val content = contentAsString(response)
      content should include(messagesApi("agent.services.account.heading", "servicename.titleSuffix"))
      content should include(messagesApi("agent.services.account.heading"))
      content should include(messagesApi("app.name"))
      content should include(messagesApi("agent.accountNumber","TARN0000001"))
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
      content should include(messagesApi("agent.services.account.section3.col1.p"))
      content should include(messagesApi("agent.services.account.section3.col1.link"))
      content should include(invitationsUrl)
      content should include(messagesApi("agent.services.account.section3.col2.h3"))
      content should include(messagesApi("agent.services.account.section3.col2.link1"))
      content should include(messagesApi("agent.services.account.section3.col2.link2"))
      content should include(htmlEscapedMessage("agent.services.account.section3.col2.link3"))
      content should include(invitationsTrackUrl)
      content should include(mappingUrl)
      content should include(agentCancelAuthUrl)

    }

    "return the redirect returned by authActions when authActions denies access" in {

      implicit val externalUrls = new ExternalUrls(Configuration.from(Map())) {
        override lazy val agentSubscriptionUrl: String = "foo"
      }

      val authActions = new AuthActions(null, externalUrls, null) {

        override def authorisedWithAgent[A, R](body: AgentInfo => Future[R])(implicit headerCarrier: HeaderCarrier): Future[Option[R]] = {
          Future failed new InvalidBearerToken("")
        }
      }

      val controller = new AgentServicesController(messagesApi, authActions, desConnector, NoPasscodeVerification, "")

      val response = controller.root()(FakeRequest("GET", "/").withSession(("otacTokenParam", "BAR1 23/")))

      status(response) shouldBe 303
      redirectLocation(response) shouldBe Some("foo?continue=%2Fagent-services-account%3Fp%3DBAR1%2B23%252F")
    }

    "do not fail without continue url parameter" in {
      val controller = new AgentServicesController(messagesApi, authActions, desConnector, NoPasscodeVerification, "")
      val response = controller.root().apply(FakeRequest("GET", "/"))
      status(response) shouldBe OK
      contentAsString(response) should {
        not include "<a href=\"/\" class=\"btn button\" id=\"continue\">"
      }
    }

  }
}
