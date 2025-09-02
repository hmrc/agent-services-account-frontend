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
import stubs.AgentAssuranceStubs.givenAgentRecordFound

import java.time.LocalDate

class EnterRenewalDateControllerISpec
extends ComponentBaseISpec {

  private def amlsJourney(newExpirationDate: Option[LocalDate]) = UpdateAmlsJourney(
    status = ValidAmlsDetailsUK,
    newAmlsBody = Some("ABC"),
    newRegistrationNumber = Some("ABC123"),
    isAmlsBodyStillTheSame = Some(true),
    newExpirationDate = newExpirationDate,
    isRegistrationNumberStillTheSame = Some(true)
  )

  private val validEndDate = LocalDate.now().plusMonths(1)

  private val repo = inject[SessionCacheRepository]

  private val enterRenewalDatePath = s"$amlsStartPath/renewal-date"

  s"GET $enterRenewalDatePath" should {
    "display the page for the first time" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newExpirationDate = None)))

      val result = get(enterRenewalDatePath)

      result.status shouldBe OK
    }

    "display the page when answer already provided" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newExpirationDate = Some(validEndDate))))

      val result = get(enterRenewalDatePath)

      result.status shouldBe OK

      result.body should include(s"${validEndDate.getYear}")
      result.body should include(s"${validEndDate.getMonthValue}")
      result.body should include(s"${validEndDate.getDayOfMonth}")

    }

    "Forbidden for overseas agents" in {

      givenAuthorisedAsAgentWith(arn.value)
      givenAgentRecordFound(agentRecord)

      await(repo.putSession(amlsJourneyKey, amlsJourney(newExpirationDate = None).copy(status = ValidAmlsNonUK)))

      val result = get(enterRenewalDatePath)

      result.status shouldBe FORBIDDEN
    }

    s"POST $enterRenewalDatePath" should {

      "return 303 SEE_OTHER and redirect to /check-your-answers" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(newExpirationDate = None)))

        val result =
          post(enterRenewalDatePath)(body =
            Map(
              "endDate.year" -> List(validEndDate.getYear.toString),
              "endDate.month" -> List(validEndDate.getMonthValue.toString),
              "endDate.day" -> List(validEndDate.getDayOfMonth.toString)
            )
          )

        result.status shouldBe SEE_OTHER

        result.header("Location").get shouldBe "/agent-services-account/manage-account/money-laundering-supervision/check-your-answers"

        val updatedSession = await(repo.getFromSession(amlsJourneyKey)).get

        updatedSession.newExpirationDate shouldBe Some(validEndDate)
      }

      "return BadRequest when invalid form submission" in {

        givenAuthorisedAsAgentWith(arn.value)
        givenAgentRecordFound(agentRecord)

        await(repo.putSession(amlsJourneyKey, amlsJourney(newExpirationDate = None)))

        val result = post(enterRenewalDatePath)(body = Map("invalid" -> List("???")))

        result.status shouldBe BAD_REQUEST
      }
    }
  }

}
