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
import uk.gov.hmrc.agentservicesaccount.controllers.ctJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.emailverification.CompletedEmail
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.CtJourney
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class CtUpdateEmailAddressControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val updateEmailAddressPath = s"$ctSubscriptionStartPath/email-address"

  private val baseJourney: CtJourney = CtJourney(
    asaDetails = AgencyDetails(
      agencyName = None,
      agencyEmail = Some("joe@bloggs.com"),
      agencyTelephone = None,
      agencyAddress = None
    ),
    useCustomBusinessName = None,
    businessNameAnswer = None,
    useCustomPhoneNumber = None,
    phoneNumberAnswer = None,
    useCustomEmail = None,
    emailAnswer = None,
    useCustomAddress = None,
    addressAnswer = None
  )

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

    "update journey and redirect when using ASA email address" in {
      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      repo.putSession(ctJourneyKey, baseJourney).futureValue

      val result =
        post(updateEmailAddressPath)(body =
          Map(
            "emailAddressUseAsaData" -> Seq("true")
          )
        )

      result.status shouldBe SEE_OTHER

      val updated = await(repo.getFromSession(ctJourneyKey))
      updated shouldBe defined
      updated.get.useCustomEmail shouldBe Some(false)
      updated.value.emailAnswer shouldBe None
    }

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

      val result =
        post(updateEmailAddressPath)(body =
          Map(
            "emailAddressUseAsaData" -> Seq("false"),
            "emailAddressNew" -> Seq("jane@bloggs.com")
          )
        )

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "http://localhost:9890/continue-url"
    }
  }

}
