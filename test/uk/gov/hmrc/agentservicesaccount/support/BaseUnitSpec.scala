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

package uk.gov.hmrc.agentservicesaccount.support

import com.kenshoo.play.metrics.Metrics
import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.guice.{BinderOption, GuiceApplicationBuilder, GuiceableModule}
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.agentservicesaccount.FrontendModule
import uk.gov.hmrc.agentservicesaccount.connectors.{AgentClientAuthorisationConnector, SsoConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class BaseUnitSpec
    extends UnitSpec with Matchers with OptionValues with MockitoSugar with GuiceOneAppPerSuite with BeforeAndAfterEach
    with ResettingMockitoSugar with WireMockSupport {

  override implicit lazy val app: Application = appBuilder.build()

  lazy val agentClientAuthorisationConnector = app.injector.instanceOf[AgentClientAuthorisationConnector]

  lazy implicit val configuration = app.injector.instanceOf[Configuration]
  lazy implicit val env = app.injector.instanceOf[Environment]

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(new GuiceableModule {
        override def guiced(env: Environment, conf: Configuration, binderOptions: Set[BinderOption]) =
          Seq(new FrontendModule(env, conf) {
            override def configure(): Unit = {

              bind(classOf[SsoConnector]).toInstance(new SsoConnector(null, null, new Metrics() {
                override def defaultRegistry = null

                override def toJson = null
              }) {
                val whitelistedSSODomains = Set("www.foo.com", "foo.org")

                override def validateExternalDomain(
                  domain: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
                  Future.successful(whitelistedSSODomains.contains(domain))
              })
            }
          })

        override def disable(classes: Seq[Class[_]]) = this
      })
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.agent-suspension.port" -> wireMockPort,
        "auditing.enabled" -> false
      )
}
