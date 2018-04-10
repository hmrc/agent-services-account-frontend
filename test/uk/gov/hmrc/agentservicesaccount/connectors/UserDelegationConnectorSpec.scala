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

package uk.gov.hmrc.agentservicesaccount.connectors

import java.net.URL

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.agentservicesaccount.support.WireMockSupport
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet}
import uk.gov.hmrc.play.test.UnitSpec

class UserDelegationConnectorSpec extends UnitSpec with GuiceOneAppPerTest with WireMockSupport  {

  override def fakeApplication(): Application = appBuilder.build()

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.agent-services-account.port" -> wireMockPort,
        "microservice.services.auth.port" -> wireMockPort,
        "auditing.enabled" -> false
      )

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val connector = new UserDelegationConnector(new URL(s"http://localhost:$wireMockPort"), app.injector.instanceOf[HttpGet])
  private implicit val hc = HeaderCarrier()



}
