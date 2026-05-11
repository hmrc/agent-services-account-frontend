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

import play.api.test.Helpers
import play.api.test.Helpers._
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.EmailVerificationStubs.givenCheckEmailSuccess
import stubs.EmailVerificationStubs.givenVerifyEmailSuccess
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressNewKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.emailverification.CompletedEmail
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

class UpdateEmailAddressControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val legacyRegimes = List(CT, PAYE, SA)

  legacyRegimes.foreach(legacyRegime => {
    val updateEmailAddressPath = s"$subscriptionStartPath/$legacyRegime/email-address"

    s"GET $updateEmailAddressPath" should {
      "display the enter email address page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val result = get(updateEmailAddressPath)

        result.status shouldBe OK
        val expectedTitle: String =
          (legacyRegime: LegacyRegime) match {
            case CT => "What email address should we use to contact you about Corporation Tax?"
            case PAYE => "What email address should we use to contact you about PAYE?"
            case SA => "What email address should we use to contact you about Self Assessment?"
          }
        assertPageHasTitle(expectedTitle)(result)
      }
    }

    s"POST $updateEmailAddressPath" should {

      val journeyWithRedirectLocations = List(
        (subscriptionBaseJourney, "address"),
        (subscriptionFullJourney(legacyRegime), "check-your-answers")
      )

      journeyWithRedirectLocations.foreach(journeyWithRedirectLocation => {
        s"update journey and redirect to ${journeyWithRedirectLocation._2} when using ASA email address " +
          s"and journey ${completeString(journeyWithRedirectLocation._1, legacyRegime)}}" in {
            givenAuthorisedAsAgentWith(arn.value)
            givenGetAgentRecord(agentRecord)
            stubASAGetResponseError(arn, NOT_FOUND)

            repo.putSession(subscriptionJourneyKey(legacyRegime), journeyWithRedirectLocation._1).futureValue

            val result =
              post(updateEmailAddressPath)(body =
                Map(
                  emailAddressUseAsaDataKey -> Seq("true")
                )
              )
            result.status shouldBe SEE_OTHER
            result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/${journeyWithRedirectLocation._2}")

            val updated = await(repo.getFromSession(subscriptionJourneyKey(legacyRegime)))
            updated shouldBe defined
            updated.get.useCustomEmail shouldBe Some(false)
            updated.value.emailAnswer shouldBe None
          }

      })

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
              emailAddressUseAsaDataKey -> Seq("false"),
              emailAddressNewKey -> Seq("jane@bloggs.com")
            )
          )

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "http://localhost:9890/continue-url"
      }
    }

  })

  List(CT, SA).foreach(legacyRegime => {
    val customEmailAddressPath = s"$subscriptionStartPath/$legacyRegime/email-address-too-long"

    s"GET $customEmailAddressPath" should {
      "display the custom email address page" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val result = get(customEmailAddressPath)

        result.status shouldBe OK
        val expectedTitle: String = "Your agent services account email address is too long"
        assertPageHasTitle(expectedTitle)(result)
      }
    }

    s"POST $customEmailAddressPath" should {

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
          post(customEmailAddressPath)(body =
            Map(
              emailAddressUseAsaDataKey -> Seq("false"),
              emailAddressNewKey -> Seq("jane@bloggs.com")
            )
          )

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "http://localhost:9890/continue-url"
      }
    }

  })

  val customEmailAddressPathFromPaye = s"$subscriptionStartPath/PAYE/email-address-too-long"

  s"GET $customEmailAddressPathFromPaye" should {
    "redirect to /subscription/PAYE/email-address" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      val result = get(customEmailAddressPathFromPaye)

      result.status shouldBe SEE_OTHER
      result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/PAYE/email-address")
    }
  }

}
