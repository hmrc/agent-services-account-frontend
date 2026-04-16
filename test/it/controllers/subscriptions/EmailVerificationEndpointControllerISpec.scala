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

package it.controllers.subscriptions

import play.api.test.Helpers._
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.EmailVerificationStubs.givenCheckEmailSuccess
import stubs.EmailVerificationStubs.givenVerifyEmailSuccess
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptions
import uk.gov.hmrc.agentservicesaccount.models.emailverification.CompletedEmail
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.{CT, SA}
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class EmailVerificationEndpointControllerISpec
extends ComponentBaseISpec {

  private val legacyRegimes = List(CT, SA)
  private val repo = inject[SessionCacheRepository]

  legacyRegimes.foreach(legacyRegime => {
    val finishEmailVerificationPath = s"$subscriptionStartPath/$legacyRegime/email-verification-finish"

    s"GET $finishEmailVerificationPath" should {
      "store the new email address in session and redirect to the continue url" in {

        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)
        givenCheckEmailSuccess(credId = "cred-id", verificationStatusResponse = VerificationStatusResponse(emails = List.empty[CompletedEmail]))
        givenVerifyEmailSuccess(redirectUri = "/continue-url")

        await(repo.putSession(emailPendingVerificationKey, "new@email.com"))

        val result = get(finishEmailVerificationPath)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "http://localhost:9890/continue-url"
      }

      "return Internal Server Error if authProviderId is not available" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        await(repo.putSession(emailPendingVerificationKey, "email@emaul.com"))

        val result = get(finishEmailVerificationPath)

        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "redirect to CT update address page if email is already verified" in {

        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true,
          email = "abc@abc.com"
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)
        givenCheckEmailSuccess(
          credId = "cred-id",
          verificationStatusResponse = VerificationStatusResponse(
            emails = List(CompletedEmail(
              "new@abc.com",
              verified = true,
              locked = false
            ))
          )
        )

        await(repo.putSession(emailPendingVerificationKey, "new@abc.com"))

        val result = get(finishEmailVerificationPath)

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe s"${subscriptions.routes.UpdateAddressController.showPage(legacyRegime)}"
      }
    }
  })

}
