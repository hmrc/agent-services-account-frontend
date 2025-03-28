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

package uk.gov.hmrc.agentservicesaccount.support

import com.google.inject.AbstractModule
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentservicesaccount.stubs.AuthStubs
import uk.gov.hmrc.agentservicesaccount.views.ViewBaseSpec

import scala.concurrent.duration.{DAYS, Duration}

abstract class BaseISpec
  extends UnitSpec
    with GuiceOneAppPerSuite
    with WireMockSupport
    with PekkoMaterializerSpec
    with AuthStubs
    with MetricsTestSupport
    with ViewBaseSpec
    with TestConstants {

  override implicit lazy val app: Application = appBuilder().build()

  def moduleWithOverrides: AbstractModule = new AbstractModule() {}

  protected def appBuilder(
                            additionalConfiguration: Map[String, Any] = Map.empty[String, Any]): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.auth.host" -> wireMockHost,
        "microservice.services.agent-assurance.port" -> wireMockPort,
        "microservice.services.agent-assurance.host" -> wireMockHost,
        "microservice.services.agent-client-authorisation.port" -> wireMockPort,
        "microservice.services.agent-client-authorisation.host" -> wireMockHost,
        "microservice.services.agent-permissions.port" -> wireMockPort,
        "microservice.services.agent-permissions.host" -> wireMockHost,
        "microservice.services.agent-user-client-details.port" -> wireMockPort,
        "microservice.services.agent-user-client-details.host" -> wireMockHost,
        "microservice.services.agent-services-account.port" -> wireMockPort,
        "microservice.services.agent-services-account.host" -> wireMockHost,
        "microservice.services.agent-permissions-frontend.external-url" -> wireMockBaseUrlAsString,
        "auditing.enabled" -> false,
        "metrics.enabled" -> false,
        "suspendedContactDetails.sendEmail" -> false,
        "features.enable-non-hmrc-supervisory-body" -> true,
        "features.enable-ema-content" -> true,
        "mongodb.desi-details.lockout-period" -> Duration(28, DAYS).toMinutes
      )
      .configure(additionalConfiguration)
      .overrides(moduleWithOverrides)

  override def beforeEach(): Unit = {
    super.beforeEach()
    givenCleanMetricRegistry()
  }

}
