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

import org.jsoup.Jsoup
import play.api.test.Helpers.*
import stubs.AgentServicesAccountStubs.givenGetAgentRecord
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.EmailVerificationStubs.givenCheckEmailNotOK
import stubs.EmailVerificationStubs.givenCheckEmailSuccess
import stubs.EmailVerificationStubs.givenVerifyEmailSuccess
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.subscriptionJourneyKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.CT_SA_EMAIL_MAX_LENGTH
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressNewKey
import uk.gov.hmrc.agentservicesaccount.forms.subscriptions.SubscriptionEmailAddressForm.emailAddressUseAsaDataKey
import uk.gov.hmrc.agentservicesaccount.models.emailverification.CompletedEmail
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.CT
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.SA
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository

import scala.util.Random

class UpdateEmailAddressControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val legacyRegimes = List(CT, PAYE, SA)

  legacyRegimes.foreach(legacyRegime => {
    val updateEmailAddressPath = s"$subscriptionStartPath/$legacyRegime/email-address"

    s"GET $updateEmailAddressPath" should {
      "display the enter email address page with option to select ASA Agency email address when ASA Agency email address is verified" in {
        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)

        val asaAgencyEmail = agentRecord.agencyDetails.flatMap(_.agencyEmail).getOrElse("")
        val completedEmail = CompletedEmail(asaAgencyEmail, verified = true, locked = false)
        givenCheckEmailSuccess(credId = "cred-id", verificationStatusResponse = VerificationStatusResponse(emails = List(completedEmail)))

        val result = get(updateEmailAddressPath)

        result.status shouldBe OK
        val expectedTitle: String =
          (legacyRegime: LegacyRegime) match {
            case CT => "What email address should we use to contact you about Corporation Tax?"
            case PAYE => "What email address should we use to contact you about PAYE?"
            case SA => "What email address should we use to contact you about Self Assessment?"
          }
        assertPageHasTitle(expectedTitle)(result)
        val doc = Jsoup.parse(result.body)
        doc.select(".govuk-radios__item").size() shouldBe 2
        doc.select(".govuk-radios__item").get(0).text() shouldBe asaAgencyEmail
        val expectedFalseText: String =
          (legacyRegime: LegacyRegime) match {
            case CT => "I want to use a different email address for Corporation Tax"
            case PAYE => "I want to use a different email address for PAYE"
            case SA => "I want to use a different email address for Self Assessment"
          }
        doc.select(".govuk-radios__item").get(1).text() shouldBe expectedFalseText
        val conditional = doc.select(".govuk-radios__conditional").first()
        conditional.hasClass("govuk-radios__conditional--hidden") shouldBe true
      }

      "display the enter email address page with single input box when ASA Agency email address is not verified" in {
        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)
        givenCheckEmailNotOK(credId = "cred-id", status = 404)

        val result = get(updateEmailAddressPath)

        result.status shouldBe OK
        val expectedTitle: String =
          (legacyRegime: LegacyRegime) match {
            case CT => "What email address should we use to contact you about Corporation Tax?"
            case PAYE => "What email address should we use to contact you about PAYE?"
            case SA => "What email address should we use to contact you about Self Assessment?"
          }
        assertPageHasTitle(expectedTitle)(result)
        val doc = Jsoup.parse(result.body)
        doc.select(".govuk-radios__item").size() shouldBe 0
        doc.html() should include(s"<input type=\"hidden\" name=\"$emailAddressUseAsaDataKey\" value=\"false\">")
        doc.select("#emailAddressNew").size() shouldBe 1
      }
    }

    s"POST $updateEmailAddressPath" should {

      "return BAD_REQUEST when form is invalid - ASA Agency email address is verified" in {
        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)
        givenCheckEmailSuccess(credId = "cred-id", verificationStatusResponse = VerificationStatusResponse(emails = List.empty[CompletedEmail]))

        val result =
          post(updateEmailAddressPath)(body =
            Map(
              emailAddressUseAsaDataKey -> Seq("")
            )
          )

        result.status shouldBe BAD_REQUEST
      }

      "return BAD_REQUEST when form is invalid - ASA Agency email address is not verified" in {
        givenFullAuthorisedAsAgentWith(
          arn.value,
          "cred-id",
          isAdmin = true
        )
        givenGetAgentRecord(agentRecord)
        stubASAGetResponseError(arn, NOT_FOUND)
        givenCheckEmailNotOK(credId = "cred-id", status = 404)

        val result =
          post(updateEmailAddressPath)(body =
            Map(
              emailAddressUseAsaDataKey -> Seq("")
            )
          )

        result.status shouldBe BAD_REQUEST
      }

      val journeyWithRedirectLocations = List(
        (subscriptionBaseJourney, "address"),
        (subscriptionFullJourney(legacyRegime), "check-your-answers")
      )

      if (legacyRegime != PAYE) {
        "update journey and redirect to email-address-too-long when using ASA email address that is too long" in {
          givenAuthorisedAsAgentWith(arn.value)
          givenGetAgentRecord(agentRecord)
          stubASAGetResponseError(arn, NOT_FOUND)

          val tooLongEmailAddress: String =
            Iterator.continually(Random.nextPrintableChar()).filter(_.isLetter).take(CT_SA_EMAIL_MAX_LENGTH).mkString + "@email.com"
          val newAsaDetails = subscriptionAgencyDetails.copy(agencyEmail = Some(tooLongEmailAddress))
          val subscriptionJourney = subscriptionBaseJourney.copy(asaDetails = newAsaDetails)

          repo.putSession(subscriptionJourneyKey(legacyRegime), subscriptionJourney).futureValue

          val result =
            post(updateEmailAddressPath)(body =
              Map(
                emailAddressUseAsaDataKey -> Seq("true")
              )
            )
          result.status shouldBe SEE_OTHER
          result.header(LOCATION) shouldBe Some(s"$subscriptionStartPath/$legacyRegime/email-address-too-long")
        }
      }

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
