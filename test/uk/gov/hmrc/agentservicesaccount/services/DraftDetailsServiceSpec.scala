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

package uk.gov.hmrc.agentservicesaccount.services

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.test.Helpers.await
import play.api.test.DefaultAwaitTimeout
import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.controllers.draftNewContactDetailsKey
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.support.TestConstants
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class DraftDetailsServiceSpec
extends PlaySpec
with DefaultAwaitTimeout
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants {

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait Setup {

    protected val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]

    object TestService
    extends DraftDetailsService(mockSessionCacheService)

  }

  "updateDraftDetails" should {
    "store updated draft details in session" when {
      "there are no details already in session" in new Setup {
        mockSessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[Request[Any]]
        ) returns Future.successful(None)

        mockSessionCacheService.put[DesignatoryDetails](
          dataKey = draftNewContactDetailsKey,
          value = desiDetailsWithEmptyOtherServices.copy(
            agencyDetails = emptyAgencyDetails,
            otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None))
          )
        )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        val result = TestService.updateDraftDetails(desiDetailsWithEmptyOtherServices =>
          desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None)))
        )

        await(result) mustBe ()

      }

      "there are details already in session" in new Setup {
        mockSessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[Request[Any]]
        ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

        mockSessionCacheService.put[DesignatoryDetails](
          dataKey = draftNewContactDetailsKey,
          value = desiDetailsWithEmptyOtherServices.copy(otherServices =
            desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None))
          )
        )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.successful((SessionKeys.sessionId -> "session-123"))

        val result = TestService.updateDraftDetails(desiDetailsWithEmptyOtherServices =>
          desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None)))
        )

        await(result) mustBe ()

      }
    }
    "throw an exception" when {
      "session retrieval fails" in new Setup {
        mockSessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[Request[Any]]
        ) returns Future.failed(new Exception("Something went wrong"))

        intercept[Exception] {
          await(TestService.updateDraftDetails(desiDetailsWithEmptyOtherServices =>
            desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None)))
          ))
        }
      }

      "session storage fails" in new Setup {
        mockSessionCacheService.get[DesignatoryDetails](draftNewContactDetailsKey)(
          *[Reads[DesignatoryDetails]],
          *[Request[Any]]
        ) returns Future.successful(Some(desiDetailsWithEmptyOtherServices))

        mockSessionCacheService.put[DesignatoryDetails](
          dataKey = draftNewContactDetailsKey,
          value = desiDetailsWithEmptyOtherServices.copy(otherServices =
            desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None))
          )
        )(*[Writes[DesignatoryDetails]], *[Request[Any]]) returns Future.failed(new Exception("Something went wrong"))

        intercept[Exception] {
          await(TestService.updateDraftDetails(desiDetailsWithEmptyOtherServices =>
            desiDetailsWithEmptyOtherServices.copy(otherServices = desiDetailsWithEmptyOtherServices.otherServices.copy(ctChanges = CtChanges(true, None)))
          ))
        }
      }
    }
  }

}
