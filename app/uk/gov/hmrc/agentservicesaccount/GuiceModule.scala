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

package uk.gov.hmrc.agentservicesaccount

import java.net.URL

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provider}
import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HttpGet, HttpPost}

class GuiceModule extends AbstractModule with ServicesConfig {

  override def configure(): Unit = {
    bind(classOf[PlayAuthConnector]).to(classOf[FrontendAuthConnector])
    bind(classOf[AppConfig]).toInstance(FrontendAppConfig)
    bind(classOf[HttpGet]).toInstance(WSHttp)
    bind(classOf[HttpPost]).toInstance(WSHttp)
    bind(classOf[LoggerLike]).toInstance(Logger)
    bindBaseUrl("sso")
  }

  private def bindBaseUrl(serviceName: String) =
    bind(classOf[URL]).annotatedWith(Names.named(s"$serviceName-baseUrl")).toProvider(new BaseUrlProvider(serviceName))

  private class BaseUrlProvider(serviceName: String) extends Provider[URL] {
    override lazy val get = new URL(baseUrl(serviceName))
  }

}
