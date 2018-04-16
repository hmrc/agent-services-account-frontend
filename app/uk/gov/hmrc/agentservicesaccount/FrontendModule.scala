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
import javax.inject.{Inject, Provider, Singleton}

import com.google.inject.AbstractModule
import com.google.inject.name.{Named, Names}
import org.slf4j.MDC
import play.api.{Configuration, Environment, Logger, LoggerLike}
import uk.gov.hmrc.agentservicesaccount.auth.{FrontendPasscodeVerification, PasscodeVerification}
import uk.gov.hmrc.agentservicesaccount.connectors.FrontendAuthConnector
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.otac.OtacAuthConnector
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

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

    bind(classOf[HttpGet]).to(classOf[HttpVerbs])
    bind(classOf[HttpPost]).to(classOf[HttpVerbs])
    bind(classOf[LoggerLike]).toInstance(Logger)
    bind(classOf[AuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[OtacAuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[PasscodeVerification]).to(classOf[FrontendPasscodeVerification])

    bindBaseUrl("auth")
    bindBaseUrl("sso")
    bindBaseUrl("agent-services-account")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

  private def bindProperty(propertyName: String) =
    bind(classOf[String]).annotatedWith(Names.named(propertyName)).toProvider(new PropertyProvider(propertyName))

  private class PropertyProvider(confKey: String) extends Provider[String] {
    override lazy val get = configuration.getString(confKey)
      .getOrElse(throw new IllegalStateException(s"No value found for configuration property $confKey"))
  }

  import scala.reflect.ClassTag
  import com.google.inject.binder.ScopedBindingBuilder
  import com.google.inject.name.Names.named

  private def bindServiceConfigProperty[A](propertyName: String)(implicit classTag: ClassTag[A], ct: ServiceConfigPropertyType[A]): ScopedBindingBuilder =
    ct.bindServiceConfigProperty(classTag.runtimeClass.asInstanceOf[Class[A]])(propertyName)

  sealed trait ServiceConfigPropertyType[A] {
    def bindServiceConfigProperty(clazz: Class[A])(propertyName: String): ScopedBindingBuilder
  }

  object ServiceConfigPropertyType {

    implicit val stringServiceConfigProperty: ServiceConfigPropertyType[String] = new ServiceConfigPropertyType[String] {
      def bindServiceConfigProperty(clazz: Class[String])(propertyName: String): ScopedBindingBuilder =
        bind(clazz).annotatedWith(named(s"$propertyName")).toProvider(new StringServiceConfigPropertyProvider(propertyName))

      private class StringServiceConfigPropertyProvider(propertyName: String) extends Provider[String] {
        override lazy val get = getConfString(propertyName, throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
      }
    }

    implicit val intServiceConfigProperty: ServiceConfigPropertyType[Int] = new ServiceConfigPropertyType[Int] {
      def bindServiceConfigProperty(clazz: Class[Int])(propertyName: String): ScopedBindingBuilder =
        bind(clazz).annotatedWith(named(s"$propertyName")).toProvider(new IntServiceConfigPropertyProvider(propertyName))

      private class IntServiceConfigPropertyProvider(propertyName: String) extends Provider[Int] {
        override lazy val get = getConfInt(propertyName, throw new RuntimeException(s"No service configuration value found for '$propertyName'"))
      }
    }

    implicit val booleanServiceConfigProperty: ServiceConfigPropertyType[Boolean] = new ServiceConfigPropertyType[Boolean] {
      def bindServiceConfigProperty(clazz: Class[Boolean])(propertyName: String): ScopedBindingBuilder =
        bind(clazz).annotatedWith(named(s"$propertyName")).toProvider(new BooleanServiceConfigPropertyProvider(propertyName))

      private class BooleanServiceConfigPropertyProvider(propertyName: String) extends Provider[Boolean] {
        override lazy val get = getConfBool(propertyName, false)
      }
    }
  }
}

@Singleton
class HttpVerbs @Inject() (val auditConnector: AuditConnector, @Named("appName") val appName: String)
  extends HttpGet with HttpPost with HttpPut with HttpPatch with HttpDelete with WSHttp
    with HttpAuditing {
  override val hooks = Seq(AuditingHook)
}
