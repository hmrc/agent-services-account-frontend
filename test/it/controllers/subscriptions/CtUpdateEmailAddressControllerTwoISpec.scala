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
import stubs.AgentServicesAccountStubs.{givenGetAgentRecord, stubASAGetResponseError}
import stubs.EmailVerificationStubs.{givenCheckEmailSuccess, givenVerifyEmailSuccess}
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.{currentSelectedChangesKey, desiDetails, draftNewContactDetailsKey, draftSubmittedByKey, emailPendingVerificationKey}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.emailverification.{CompletedEmail, VerificationStatusResponse}
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class CtUpdateEmailAddressControllerTwoISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val updateEmailAddressPath = s"$ctSubscriptionStartPath/email-address"

  s"GET $updateEmailAddressPath" should {
    "display the enter email address page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(updateEmailAddressPath)

      result.status shouldBe OK
      assertPageHasTitle("What email address should we use to contact you about Corporation Tax?")(result)
    }
  }

  s"POST $updateEmailAddressPath" should {

    "(if the email is unverified) redirect to the verify-email external journey" in {

      givenFullAuthorisedAsAgentWith(
        arn = arn.value,
        providerId = "cred-id",
        email = "abc@abc.com"
      )
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenCheckEmailSuccess(
        "cred-id",
        VerificationStatusResponse(emails =
          List(CompletedEmail(
            "abc@abc.com",
            verified = true,
            locked = false
          ))
        )
      )
      givenVerifyEmailSuccess("/continue-url")

      await(repo.putSession(emailPendingVerificationKey, "new@abc.com"))

      val result = post(updateEmailAddressPath)(body = Map(
        "emailAddressUseAsaData" -> Seq("false"),
        "emailAddressNew" -> Seq("jane@bloggs.com")
      ))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "http://localhost:9890/continue-url"
    }
  }

}
