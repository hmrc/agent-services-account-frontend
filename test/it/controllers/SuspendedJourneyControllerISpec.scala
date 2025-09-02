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

package it.controllers

import play.api.http.Status.OK
import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.arnKey
import uk.gov.hmrc.agentservicesaccount.controllers.descriptionKey
import uk.gov.hmrc.agentservicesaccount.controllers.emailKey
import uk.gov.hmrc.agentservicesaccount.controllers.nameKey
import uk.gov.hmrc.agentservicesaccount.controllers.phoneKey
import uk.gov.hmrc.agentservicesaccount.controllers.routes
import uk.gov.hmrc.agentservicesaccount.models.SuspensionDetails
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAgentRecordFound
import stubs.EmailStubs._

class SuspendedJourneyControllerISpec
extends ComponentBaseISpec {

  private val repo = inject[SessionCacheRepository]

  private val accountLimitedPath = "/agent-services-account/account-limited"
  private val recoveryContactDetailsPath = "/agent-services-account/recovery-contact-details"
  private val recoveryDescriptionPath = "/agent-services-account/recovery-description"
  private val recoveryCheckYourAnswersPath = "/agent-services-account/recovery-check-your-answers"
  private val recoveryRequestConfirmationPath = "/agent-services-account/recovery-request-confirmation"

  private val suspendedAgentRecord = agentRecord.copy(suspensionDetails = Some(SuspensionDetails(suspensionStatus = true, Some(Set("AGSV")))))

  s"GET $accountLimitedPath" should {
    "return Ok and show the suspension warning page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result = get(accountLimitedPath)

      result.status shouldBe OK
    }
  }

  s"GET $recoveryContactDetailsPath" should {
    "return Ok and show the contact details page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      await(repo.putSession(nameKey, "Bob"))
      await(repo.putSession(emailKey, "abc@abc.com"))
      await(repo.putSession(phoneKey, "0101"))
      await(repo.putSession(arnKey, "123"))

      val result = get(recoveryContactDetailsPath)

      result.status shouldBe OK

      result.body should include("Bob")
      result.body should include("abc@abc.com")
      result.body should include("0101")
      result.body should include("123")
    }
  }

  s"POST $recoveryContactDetailsPath" should {
    s"redirect to $recoveryDescriptionPath" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result =
        post(recoveryContactDetailsPath)(body =
          Map(
            "name" -> List("Bob"),
            "email" -> List("abc@abc.com"),
            "phone" -> List("0101")
          )
        )

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${routes.SuspendedJourneyController.showSuspendedDescription().url}"

      await(repo.getFromSession(nameKey)).get shouldBe "Bob"
      await(repo.getFromSession(emailKey)).get shouldBe "abc@abc.com"
      await(repo.getFromSession(phoneKey)).get shouldBe "0101"
    }

    "return Bad Request if the data is invalid" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result =
        post(recoveryContactDetailsPath)(body =
          Map(
            "name" -> List("<>"),
            "email" -> List("<>"),
            "phone" -> List("<>")
          )
        )

      result.status shouldBe BAD_REQUEST
    }
  }

  s"GET $recoveryDescriptionPath" should {
    "return Ok and show the description recovery page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result = get(recoveryDescriptionPath)

      result.status shouldBe OK
    }

    "return Ok and bind previous answers" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      await(repo.putSession(descriptionKey, "Help"))

      val result = get(recoveryDescriptionPath)

      result.status shouldBe OK

      result.body should include("Help")
    }
  }

  s"POST $recoveryDescriptionPath" should {
    "return 400 if form is submitted with errors" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result =
        post(recoveryContactDetailsPath)(body =
          Map(
            "description" -> List("")
          )
        )

      result.status shouldBe BAD_REQUEST
    }
    s"redirect to ${routes.SuspendedJourneyController.showSuspendedSummary().url} when form is submitted with correct information" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result =
        post(recoveryDescriptionPath)(body =
          Map(
            "description" -> List("Help")
          )
        )

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${routes.SuspendedJourneyController.showSuspendedSummary().url}"
      await(repo.getFromSession(descriptionKey)).get shouldBe "Help"
    }
  }

  s"GET $recoveryCheckYourAnswersPath" should {
    s"redirect to ${routes.SuspendedJourneyController.showContactDetails().url} when no details are present" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result = get(recoveryCheckYourAnswersPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe s"${routes.SuspendedJourneyController.showContactDetails().url}"

    }
    s"return Ok when session details are found" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      await(repo.putSession(nameKey, "Bob"))
      await(repo.putSession(emailKey, "abc@abc.com"))
      await(repo.putSession(phoneKey, "0101"))
      await(repo.putSession(descriptionKey, "Help"))
      await(repo.putSession(arnKey, "123"))

      val result = get(recoveryCheckYourAnswersPath)

      result.status shouldBe OK
    }
  }

  s"POST $recoveryCheckYourAnswersPath" should {
    s"redirect to ${routes.SuspendedJourneyController.showContactDetails().url} when no summary details are present" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result = post(recoveryCheckYourAnswersPath)(body = Map("" -> List("")))

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe s"${routes.SuspendedJourneyController.showContactDetails().url}"

    }

    s"redirect to ${routes.SuspendedJourneyController.showSuspendedConfirmation().url} when session details are found and send email" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)
      givenEmailSent()

      await(repo.putSession(nameKey, "Bob"))
      await(repo.putSession(emailKey, "abc@abc.com"))
      await(repo.putSession(phoneKey, "0101"))
      await(repo.putSession(descriptionKey, "Help"))
      await(repo.putSession(arnKey, "123"))

      val result = post(recoveryCheckYourAnswersPath)(body = Map("" -> List("")))

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe s"${routes.SuspendedJourneyController.showSuspendedConfirmation().url}"
    }
  }

  "showSuspendedConfirmation" should {
    "return Ok and show the confirmation page" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(suspendedAgentRecord)

      val result = get(recoveryRequestConfirmationPath)

      result.status shouldBe OK
    }
  }

}
