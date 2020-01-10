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

package uk.gov.hmrc.agentservicesaccount.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.otac.PlayOtacAuthConnector
import uk.gov.hmrc.http.{HttpGet, HttpPost}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}

@Singleton
class FrontendAuthConnector @Inject()(
                                           val httpClient: HttpClient,
                                           override val runModeConfiguration: Configuration,
                                           override val environment: Environment,
                                           val _actorSystem: ActorSystem)
  extends DefaultAuthConnector(httpClient, runModeConfiguration, environment)
  with PlayOtacAuthConnector {
  override def http = new HttpPost with HttpGet with WSPost with WSGet {
    override val hooks = NoneRequired
    override protected def configuration: Option[Config] = Some(runModeConfiguration.underlying)
    override protected def actorSystem: ActorSystem = _actorSystem
  }
}