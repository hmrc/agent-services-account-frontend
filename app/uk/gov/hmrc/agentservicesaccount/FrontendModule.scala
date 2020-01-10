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

package uk.gov.hmrc.agentservicesaccount

import java.net.URL

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import javax.inject.Provider
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger, LoggerLike}
import uk.gov.hmrc.agentservicesaccount.auth.{FrontendPasscodeVerification, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.connectors.FrontendAuthConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.otac.OtacAuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.{DefaultHttpClient, HttpClient}
import uk.gov.hmrc.play.config.ServicesConfig

class FrontendModule(val environment: Environment, val configuration: Configuration) extends AbstractModule with ServicesConfig {

  override val runModeConfiguration: Configuration = configuration
  override protected def mode = environment.mode

  def configure(): Unit = {

    val appName = "agent-services-account-frontend"

    val loggerDateFormat: Option[String] = configuration.getString("logger.json.dateformat")
    Logger.info(s"Starting microservice : $appName : in mode : ${environment.mode}")
    MDC.put("appName", appName)
    loggerDateFormat.foreach(str => MDC.put("logger.json.dateformat", str))

    bindProperty("appName")
    bindProperty("customDimension")
    bindProperty("microservice.services.agent-services-account-frontend.external-url")
    bindBooleanProperty("features.enable-agent-suspension")
    bindIntegerProperty("timeoutDialog.timeout-seconds")
    bindIntegerProperty("timeoutDialog.timeout-countdown-seconds")

    bind(classOf[HttpGet]).to(classOf[DefaultHttpClient])
    bind(classOf[HttpPost]).to(classOf[DefaultHttpClient])
    bind(classOf[HttpClient]).to(classOf[DefaultHttpClient])
    bind(classOf[LoggerLike]).toInstance(Logger)
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[OtacAuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[PasscodeVerification]).to(classOf[FrontendPasscodeVerification])

    bindBaseUrl("auth")
    bindBaseUrl("sso")
    bindBaseUrl("agent-suspension")
    ()
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindIntegerProperty(propertyName: String) =
    bind(classOf[Int])
      .annotatedWith(Names.named(propertyName))
      .toProvider(new IntegerPropertyProvider(propertyName))

  private class IntegerPropertyProvider(confKey: String) extends Provider[Int] {
    override lazy val get: Int = configuration
      .getInt(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  private def bindProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(propertyName)).toProvider(new PropertyProvider(propertyName))

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration.getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  private def bindBooleanProperty(propertyName: String) =
    bind(classOf[Boolean])
      .annotatedWith(Names.named(propertyName))
      .toProvider(new BooleanPropertyProvider(propertyName))

  private class BooleanPropertyProvider(confKey: String) extends Provider[Boolean] {
    override lazy val get: Boolean = configuration
      .getBoolean(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }
}