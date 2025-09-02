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

package it.controllers.amls

import play.api.http.Status.OK
import support.ComponentBaseISpec
import stubs.AgentAssuranceStubs._

class AmlsConfirmationControllerISpec
extends ComponentBaseISpec {

  val confirmationNewSupervisionPath = "/manage-account/money-laundering-supervision/confirmation-new-supervision"

  s"GET $confirmationNewSupervisionPath" should {
    "return Ok and show the confirmation page for AMLS details updated" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      val result = get(confirmationNewSupervisionPath)

      result.status shouldBe OK
    }
  }

}
