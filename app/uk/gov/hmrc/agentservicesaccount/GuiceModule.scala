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

package uk.gov.hmrc.agentservicesaccount

import java.net.URL

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provider}
import play.api.{Configuration, Environment, Logger, LoggerLike}
import uk.gov.hmrc.agentservicesaccount.auth.{FrontendPasscodeVerification, PasscodeVerification}
import uk.gov.hmrc.auth.core.{AuthConnector, PlayAuthConnector}
import uk.gov.hmrc.auth.otac.OtacAuthConnector
import uk.gov.hmrc.http.{HttpGet, HttpPost}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig

class GuiceModule(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  override val runModeConfiguration: Configuration = configuration

  override def configure(): Unit = {
    bindProperty("appName")
    bindProperty("customDimension")
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[OtacAuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[PasscodeVerification]).to(classOf[FrontendPasscodeVerification])
    bind(classOf[AppConfig]).toInstance(FrontendAppConfig)
    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[LoggerLike]).toInstance(Logger)
    bind(classOf[AuditConnector]).toInstance(FrontendAuditConnector)
    bindBaseUrl("sso")
    bindBaseUrl("agent-services-account")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindServiceProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(s"$propertyName")).toProvider(new ServicePropertyProvider(propertyName))

  private class ServicePropertyProvider(propertyName: String) extends Provider[String] {
    override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No configuration value found for '$propertyName'"))
  }

  private def bindProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(propertyName)).toProvider(new PropertyProvider(propertyName))

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration.getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

}
