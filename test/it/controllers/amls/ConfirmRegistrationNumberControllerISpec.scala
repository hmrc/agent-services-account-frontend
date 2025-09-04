/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.test.Helpers._
import support.ComponentBaseISpec
import uk.gov.hmrc.agentservicesaccount.controllers.amlsJourneyKey
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses.ValidAmlsDetailsUK
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentAssuranceStubs.givenAMLSDetailsForArn
import stubs.AgentAssuranceStubs.givenAgentRecordFound

import java.time.LocalDate

class ConfirmRegistrationNumberControllerISpec
extends ComponentBaseISpec {

  private def amlsDetailsResponse(membershipNumber: Option[String]) = AmlsDetailsResponse(
    status = ValidAmlsDetailsUK,
    details = Some(AmlsDetails(supervisoryBody = "ABC", membershipNumber = membershipNumber))
  )

  private val repo = inject[SessionCacheRepository]

  private val confirmRegistrationNumberPath = s"$amlsStartPath/confirm-registration-number"

  private def amlsJourney(status: AmlsStatus) = UpdateAmlsJourney(
    status = status,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = Some("1234567890"),
    newExpirationDate = Some(LocalDate.now())
  )

  s"GET $confirmRegistrationNumberPath" should {
    "display the page if the user has an AMLS reg number" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(Some("123")), arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

      val result = get(confirmRegistrationNumberPath)

      result.status shouldBe OK
      assertPageHasTitle("Is your registration number still 123?")(result)
    }

    "display the page if the user has an AMLS reg number and previously answered the question" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(Some("ref123")), arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK).copy(isRegistrationNumberStillTheSame = Some(true))))

      val result = get(confirmRegistrationNumberPath)

      result.status shouldBe OK

      result.body should include("ref123")

    }

    "redirect the user to /new-registration-number if they do not have an AMLS reg number" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(None), arn.value)

      val result = get(confirmRegistrationNumberPath)

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"
    }
  }

  s"POST $confirmRegistrationNumberPath" should {
    "redirect the user to /new-registration-number if they do not have an AMLS reg number" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(None), arn.value)

      val result = post(confirmRegistrationNumberPath)(body = Map("accept" -> List("true")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"

    }

    "redirect to /renewal-date when YES is selected" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(Some("ref123")), arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

      val result = post(confirmRegistrationNumberPath)(body = Map("accept" -> List("true")))

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/renewal-date"

      val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

      updatedSession.isRegistrationNumberStillTheSame shouldBe Some(true)
      updatedSession.newRegistrationNumber shouldBe Some("ref123")

    }

    "redirect to /new-registration-number when NO is selected" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(Some("ref123")), arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

      val result = post(confirmRegistrationNumberPath)(body = Map("accept" -> List("false")))

      result.status shouldBe SEE_OTHER
      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/new-registration-number"

      val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

      updatedSession.isRegistrationNumberStillTheSame shouldBe Some(false)
      updatedSession.newRegistrationNumber shouldBe Some("1234567890")
    }

    "return BadRequest when invalid form submission" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)
      givenAMLSDetailsForArn(amlsDetailsResponse(Some("ref123")), arn.value)

      await(repo.putSession(amlsJourneyKey, amlsJourney(ValidAmlsDetailsUK)))

      val result = post(confirmRegistrationNumberPath)(body = Map("invalid" -> List("???")))

      result.status shouldBe BAD_REQUEST
      assertPageHasTitle("Error: Is your registration number still ref123?")(result)
    }
  }

}
