/*
 * Copyright 2025 HM Revenue & Customs
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

package support

import org.jsoup.Jsoup
import org.mongodb.scala.MongoDatabase
import org.scalatest.Assertion
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.DefaultWSCookie
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSCookie
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Request
import play.api.mvc.Session
import play.api.mvc.SessionCookieBaker
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.test.Injecting
import stubs.AuthStubs
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DAYS
import scala.concurrent.duration.Duration

trait ComponentBaseISpec
extends UnitSpec
with GuiceOneServerPerSuite
with Injecting
with AuthStubs
with WiremockHelper
with BeforeAndAfterAll
with BeforeAndAfterEach
with TestConstants
with IntegrationPatience {

  // Add all services required by our (or bootstrap's) connectors here
  def downstreamServices: Map[String, String] =
    Seq(
      "auth",
      "agent-services-account",
      "agent-assurance",
      "agent-permissions",
      "agent-user-client-details",
      "email",
      "address-lookup-frontend",
      "email-verification"
    ).flatMap { service =>
      Seq(
        s"microservice.services.$service.host" -> mockHost,
        s"microservice.services.$service.port" -> mockPort
      )
    }.toMap

  def extraConfig(): Map[String, String] = Map.empty

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config ++ extraConfig() ++ downstreamServices)
    .build()

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "fieldLevelEncryption.enabled" -> "true",
    "suspendedContactDetails.sendEmail" -> "false",
    "mongodb.desi-details.lockout-period" -> Duration(28, DAYS).toMinutes.toString,
    "enable-backend-pcr-database" -> "true"
  )

  val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]

  val mongoDatabase: MongoDatabase = mongoComponent.database

  protected def prepareDatabase(): Unit =
    mongoDatabase
      .drop()
      .toFuture()
      .futureValue

  implicit val ws: WSClient = app.injector.instanceOf[WSClient]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  protected val amlsStartPath: String = "/agent-services-account/manage-account/money-laundering-supervision"
  protected val desiDetailsStartPath: String = "/agent-services-account/manage-account/contact-details"

  override def beforeAll(): Unit = {
    startWiremock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    resetWiremock()
    prepareDatabase()
    super.beforeEach()
  }

  def get(uri: String): WSResponse = await(buildClient(uri).get())

  def assertPageHasTitle(pageTitle: String)(result: WSResponse): Assertion = {
    Jsoup.parse(result.body).select("title").first().text() shouldBe s"$pageTitle - Agent services account - GOV.UK"
  }

  def postQ(uri: String)(body: Map[String, Seq[String]])(queryParam: Seq[(String, String)]): WSResponse = await(
    buildClient(uri)
      .withHttpHeaders("Csrf-Token" -> "nocheck")
      .withQueryStringParameters(queryParam: _*)
      .post(body)
  )

  def post(uri: String)(body: Map[String, Seq[String]]): WSResponse = await(
    buildClient(uri)
      .withHttpHeaders("Csrf-Token" -> "nocheck")
      .post(body)
  )

  val baseUrl: String = "/agent-services-account"

  private def buildClient(path: String): WSRequest = ws.url(s"http://localhost:$port$baseUrl${path.replace(baseUrl, "")}")
    .withFollowRedirects(false)
    .withCookies(
      DefaultWSCookie("PLAY_LANG", "en"),
      mockSessionCookie
    )

  val sessionHeaders: Map[String, String] = Map(
    SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
    SessionKeys.authToken -> "mock-bearer-token",
    SessionKeys.sessionId -> "mock-sessionid"
  )

  implicit val request: Request[AnyContentAsFormUrlEncoded] = FakeRequest().withSession(sessionHeaders.toSeq: _*).withFormUrlEncodedBody()

  def mockSessionCookie: WSCookie = {

    val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
    val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
    val sessionCookie = cookieBaker.encodeAsCookie(Session(sessionHeaders))
    val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
    val cookie = sessionCookie.copy(value = encryptedValue.value)

    new WSCookie() {
      override def name: String = cookie.name
      override def value: String = cookie.value
      override def domain: Option[String] = cookie.domain
      override def path: Option[String] = Some(cookie.path)
      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)
      override def secure: Boolean = cookie.secure
      override def httpOnly: Boolean = cookie.httpOnly
    }
  }

}
