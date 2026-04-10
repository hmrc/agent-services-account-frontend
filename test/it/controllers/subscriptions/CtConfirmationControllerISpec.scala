/*
 * Copyright 2026 HM Revenue & Customs
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

package it.controllers.subscriptions

import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions.routes
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT

class ConfirmationControllerISpec
extends ComponentBaseISpec {

  private val legacyRegime = CT
  private val path = routes.ConfirmationController.showConfirmationPage(legacyRegime).url

  s"GET $path" should {

    "return OK and render the confirmation page when user is authorised" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      val result = get(path)

      result.status shouldBe OK

      result.body should include("We are processing your enrolment")
      result.body should include("This can take up to 5 days")
    }

    "redirect to sign in when user is not authorised" in {

      GivenIsNotLoggedIn()

      val result = get(path)

      result.status shouldBe SEE_OTHER
      result.header("Location").value should include("/bas-gateway/sign-in")
    }

    "redirect when agent is suspended" in {

      givenAuthorisedAsAgentWith(arn.value)

      givenGetAgentRecord(
        agentRecord.copy(
          suspensionDetails = Some(agentRecord.suspensionDetails.get.copy(suspensionStatus = true))
        )
      )

      val result = get(path)

      result.status shouldBe SEE_OTHER

      result.header("Location").value should include("/agent-services-account/account-limited")
    }
  }

}
