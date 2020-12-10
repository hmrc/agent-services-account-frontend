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

package uk.gov.hmrc.agentservicesaccount.support

import org.scalatest.{BeforeAndAfterEach, Matchers, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentservicesaccount.stubs.AuthStubs
import uk.gov.hmrc.play.test.UnitSpec

class BaseISpec
    extends UnitSpec with Matchers with OptionValues with GuiceOneAppPerSuite with BeforeAndAfterEach
    with WireMockSupport with AkkaMaterializerSpec with AuthStubs with MetricsTestSupport {

  override implicit lazy val app: Application = appBuilder().build()

  protected def appBuilder(
    additionalConfiguration: Map[String, Any] = Map.empty[String, Any]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port"                       -> wireMockPort,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "microservice.services.sso.port"                        -> wireMockPort,
        "auditing.enabled"                                      -> false
      )
      .configure(additionalConfiguration)

  override def beforeEach() {
    super.beforeEach()
    givenCleanMetricRegistry()
    ()
  }
}
