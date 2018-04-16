/*
 * Copyright 2018 HM Revenue & Customs
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

import com.kenshoo.play.metrics.Metrics
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import play.api.mvc.Request
import play.api.mvc.Results._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.auth.AgentRequest
import uk.gov.hmrc.agentservicesaccount.connectors.SsoConnector
import uk.gov.hmrc.agentservicesaccount.services.HostnameWhiteListService

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class ContinueUrlActionsSpec extends WordSpec with Matchers with GuiceOneAppPerSuite {

  implicit val hc = new HeaderCarrier()

  override implicit lazy val app: Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
      "continueUrl.domainWhiteList" -> Seq("foo.com","bar.org")
    )

  "ContinueUrlActions" should {

    "refine action with Some(continueUrl)" when {

      "valid absolute external continue url" in new Fixture1 {
        val result = call(requestWithContinue("http://www.foo.com/bar?abc=xyz"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe "http://www.foo.com/bar?abc=xyz"
      }

      "valid absolute internal continue url" in new Fixture1 {
        val result = call(requestWithContinue("http://bar.org/foo?abc=xyz&def=pqr"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe "http://bar.org/foo?abc=xyz&def=pqr"
      }

      "valid relative continue url" in new Fixture1 {
        val result = call(requestWithContinue("/foo/bar?abc=xyz&def=pqr"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe "/foo/bar?abc=xyz&def=pqr"
      }

      "valid absolute internal continue url and sso connector fails" in new Fixture2 {
        val result = call(requestWithContinue("http://bar.org/foo?abc=xyz&def=pqr"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe "http://bar.org/foo?abc=xyz&def=pqr"
      }
    }

    "refine action with None" when {

      "invalid malformed url" in new Fixture1 {
        val result = call(requestWithContinue("://foo/"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe ""
      }

      "invalid schema url" in new Fixture1 {
        val result = call(requestWithContinue("mailto:foo@foo.com"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe ""
      }

      "invalid domain absolute continue url" in new Fixture1 {
        val result = call(requestWithContinue("http://www.foo.org/bar?abc=xyz"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe ""
      }

      "invalid sub-domain absolute continue url" in new Fixture1 {
        val result = call(requestWithContinue("http://www.bar.org/bar?abc=xyz"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe ""
      }

      "valid absolute external continue url if sso connector fails" in new Fixture2 {
        val result = call(requestWithContinue("http://www.foo.com/bar?abc=xyz&def=pqr"))
        status(result) shouldBe 200
        contentAsString(result) shouldBe ""
      }
    }
  }

  val validInternalDomains = Set("bar.org")
  val validExternalDomains = Set("www.foo.com")

  private val configuration = app.injector.instanceOf[Configuration]

  val successfulSsoConnector = new SsoConnector(null,null,new Metrics() {
    override def defaultRegistry = null
    override def toJson = null
  }){
    override def validateExternalDomain(domain: String)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
      Future.successful(validExternalDomains.contains(domain))
  }

  val failingSsoConnector = new SsoConnector(null,null,new Metrics() {
    override def defaultRegistry = null
    override def toJson = null
  }){
    override def validateExternalDomain(domain: String)
                                       (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
      Future.failed(new Exception("some reason"))
  }

  val underTest1 = new ContinueUrlActions(new HostnameWhiteListService(configuration, successfulSsoConnector))
  val underTest2 = new ContinueUrlActions(new HostnameWhiteListService(configuration, failingSsoConnector))

  def requestWithContinue(continueUrl: String) =
    FakeRequest("GET","/some/endpoint?continue="+URLEncoder.encode(continueUrl,"UTF-8"))

  trait Fixture1 {

    def call(implicit request: Request[Any]) = underTest1.withMaybeContinueUrl {
      continueUrlOpt =>
        Future.successful(Ok(continueUrlOpt.map(_.url).getOrElse("")))
    }
  }

  trait Fixture2 {

    def call(implicit request: Request[Any]) = underTest2.withMaybeContinueUrl {
      continueUrlOpt =>
        Future.successful(Ok(continueUrlOpt.map(_.url).getOrElse("")))
    }
  }

}


