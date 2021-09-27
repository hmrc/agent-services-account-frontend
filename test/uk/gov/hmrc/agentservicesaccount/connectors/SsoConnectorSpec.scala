/*
 * Copyright 2021 HM Revenue & Customs
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

import uk.gov.hmrc.agentservicesaccount.stubs.SsoStubs
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

import play.api.test.Helpers._

class SsoConnectorSpec extends BaseISpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  private lazy val connector = app.injector.instanceOf[SsoConnector]

  private implicit val hc = HeaderCarrier()

  "SsoConnector" should {
    "return 204" in {
      SsoStubs.givenDomainIsWhitelisted("foo.com")
      val result = await(connector.validateExternalDomain("foo.com"))
      result shouldBe true
    }

    "return 400" in {
      SsoStubs.givenDomainIsNotWhitelisted("Imnotvalid.com")
      val result = await(connector.validateExternalDomain("Imnotvalid.com"))
      result shouldBe false
    }

    "return false when request fails" in {
      SsoStubs.givenDomainCheckFails("foo.com")
      val result = await(connector.validateExternalDomain("foo.com"))
      result shouldBe false
    }
  }
}
