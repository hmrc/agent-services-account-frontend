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
import uk.gov.hmrc.agentservicesaccount.models.AmlsStatuses.ValidAmlsNonUK
import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import stubs.AgentServicesAccountStubs.givenGetAgentRecord

import java.time.LocalDate

class EnterRegistrationNumberControllerISpec
extends ComponentBaseISpec {

  private def amlsJourney(newRegistrationNumber: Option[String]) = UpdateAmlsJourney(
    status = ValidAmlsDetailsUK,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = newRegistrationNumber,
    isAmlsBodyStillTheSame = Some(true),
    newExpirationDate = Some(LocalDate.now()),
    isRegistrationNumberStillTheSame = Some(true)
  )

  private val repo = inject[SessionCacheRepository]

  private val newRegistrationNumberPath = s"$amlsStartPath/new-registration-number"

  s"GET $newRegistrationNumberPath" should {
    "display the page with journey data" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newRegistrationNumber = Some("ABCD"))))

      val result = get(newRegistrationNumberPath)

      result.status shouldBe OK
      assertPageHasTitle("What is the registration number?")(result)
      result.body should include("ABCD")
    }

    "display the page without journey data" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newRegistrationNumber = None)))

      val result = get(newRegistrationNumberPath)

      result.status shouldBe OK
      assertPageHasTitle("What is the registration number?")(result)
    }
  }

  s"POST $newRegistrationNumberPath" should {

    "redirect to /upload-evidence and store data for UK agent " in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newRegistrationNumber = Some("ABC123"))))

      val result = post(newRegistrationNumberPath)(body = Map("number" -> List("ABC123")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/upload-evidence"

      val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

      updatedSession.newRegistrationNumber shouldBe Some("ABC123")
      updatedSession.isRegistrationNumberStillTheSame shouldBe Some(true)

    }

    "redirect to upload evidence and store data for overseas agent " in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newRegistrationNumber = Some("ABC123")).copy(status = ValidAmlsNonUK)))

      val result = post(newRegistrationNumberPath)(body = Map("number" -> List("ABC123")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/upload-evidence"

      val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

      updatedSession.newRegistrationNumber shouldBe Some("ABC123")
      updatedSession.isRegistrationNumberStillTheSame shouldBe Some(true)
    }

    "redirect to CYA and store data for agent with HMRC AMLS" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(None).copy(newAmlsBody = Some("HM Revenue and Customs (HMRC)"))))

      val result = post(newRegistrationNumberPath)(body = Map("number" -> List("XAML00000123456")))

      result.status shouldBe SEE_OTHER

      result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/check-your-answers"

      val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

      updatedSession.newRegistrationNumber shouldBe Some("XAML00000123456")
      updatedSession.isRegistrationNumberStillTheSame shouldBe Some(false)
    }

    "return BadRequest when invalid form submission" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenGetAgentRecord(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newRegistrationNumber = Some("ABC123")).copy(status = ValidAmlsNonUK)))

      val result = post(newRegistrationNumberPath)(body = Map("invalid" -> List("???")))

      result.status shouldBe BAD_REQUEST
      assertPageHasTitle("Error: What is the registration number?")(result)
    }
  }

}
