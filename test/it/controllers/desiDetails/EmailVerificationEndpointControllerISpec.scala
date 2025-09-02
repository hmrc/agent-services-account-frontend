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

package it.controllers.desiDetails

import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.currentSelectedChangesKey
import uk.gov.hmrc.agentservicesaccount.controllers.desiDetails
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.controllers.draftSubmittedByKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailPendingVerificationKey
import uk.gov.hmrc.agentservicesaccount.models.desiDetails._
import uk.gov.hmrc.agentservicesaccount.models.emailverification.CompletedEmail
import uk.gov.hmrc.agentservicesaccount.models.emailverification.VerificationStatusResponse
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.EmailVerificationStubs.givenCheckEmailSuccess
import stubs.EmailVerificationStubs.givenVerifyEmailSuccess

class EmailVerificationEndpointControllerISpec
extends ComponentBaseISpec {

  private val finishEmailVerificationPath = s"$desiDetailsStartPath/email-verification-finish"

  private val repo = inject[SessionCacheRepository]

  private val designatoryDetails = DesignatoryDetails(
    agencyDetails = agentRecord.agencyDetails.get.copy(
      agencyEmail = Some("new@email.com")
    ),
    otherServices = OtherServices(saChanges = SaChanges(applyChanges = false, None), ctChanges = CtChanges(applyChanges = false, None))
  )

  private val yourDetails = YourDetails(fullName = "John Tester", telephone = "078187777831")

  s"POST $finishEmailVerificationPath" should {
    "store the new email address in session and redirect to the continue url" in {

      givenFullAuthorisedAsAgentWith(
        arn.value,
        "cred-id",
        isAdmin = true
      )
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenCheckEmailSuccess(credId = "cred-id", verificationStatusResponse = VerificationStatusResponse(emails = List.empty[CompletedEmail]))
      givenVerifyEmailSuccess(redirectUri = "/continue-url")

      await(repo.putSession(currentSelectedChangesKey, Set("email")))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, yourDetails))
      await(repo.putSession(emailPendingVerificationKey, "new@email.com"))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "http://localhost:9890/continue-url"
    }

    "redirect to /manage-account/contact-details/view if contact page request is invalid" in {

      givenFullAuthorisedAsAgentWith(
        arn.value,
        "cred-id",
        isAdmin = true
      )
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("businessEmail")))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ViewContactDetailsController.showPage.url}"
    }

    "return Internal Server Error if authProviderId is not available" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)

      await(repo.putSession(currentSelectedChangesKey, Set("email")))
      await(repo.putSession(emailPendingVerificationKey, "email@emaul.com"))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to /manage-account/contact-details/check-your-answers if email is already verified and journey is complete" in {

      givenFullAuthorisedAsAgentWith(
        arn.value,
        "cred-id",
        isAdmin = true,
        email = "abc@abc.com"
      )
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenCheckEmailSuccess(
        credId = "cred-id",
        verificationStatusResponse = VerificationStatusResponse(
          emails = List(CompletedEmail(
            "abc@abc.com",
            verified = true,
            locked = false
          ))
        )
      )

      await(repo.putSession(currentSelectedChangesKey, Set("email")))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(draftSubmittedByKey, yourDetails))
      await(repo.putSession(emailPendingVerificationKey, "abc@abc.com"))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.CheckYourAnswersController.showPage.url}"
    }

    "redirect to /manage-account/contact-details/apply-code-SA if email is already verified but journey is incomplete" in {

      givenFullAuthorisedAsAgentWith(
        arn.value,
        "cred-id",
        isAdmin = true,
        email = "abc@abc.com"
      )
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenCheckEmailSuccess(
        credId = "cred-id",
        verificationStatusResponse = VerificationStatusResponse(
          emails = List(CompletedEmail(
            "abc@abc.com",
            verified = true,
            locked = false
          ))
        )
      )

      await(repo.putSession(currentSelectedChangesKey, Set("email")))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(emailPendingVerificationKey, "abc@abc.com"))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.ApplySACodeChangesController.showPage.url}"
    }

    "redirect to /email-locked if email is locked" in {

      givenFullAuthorisedAsAgentWith(
        arn.value,
        "cred-id",
        isAdmin = true,
        email = "abc@abc.com"
      )
      givenAgentRecordFound(agentRecord)
      stubASAGetResponseError(arn, NOT_FOUND)
      givenCheckEmailSuccess(
        credId = "cred-id",
        verificationStatusResponse = VerificationStatusResponse(
          emails = List(CompletedEmail(
            "new@abc.com",
            verified = true,
            locked = true
          ))
        )
      )

      await(repo.putSession(currentSelectedChangesKey, Set("email")))
      await(repo.putSession(draftNewContactDetailsKey, designatoryDetails))
      await(repo.putSession(emailPendingVerificationKey, "new@abc.com"))

      val result = get(finishEmailVerificationPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${desiDetails.routes.UpdateEmailAddressController.showEmailLocked.url}"
    }
  }

}
