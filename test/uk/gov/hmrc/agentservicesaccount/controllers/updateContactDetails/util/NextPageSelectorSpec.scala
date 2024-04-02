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

package uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util

import play.api.test.FakeRequest
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.agentservicesaccount.controllers.updateContactDetails.util.NextPageSelector.getNextPage
import uk.gov.hmrc.agentservicesaccount.controllers.{CURRENT_SELECTED_CHANGES, PREVIOUS_SELECTED_CHANGES, updateContactDetails}
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.agentservicesaccount.stubs.SessionServiceMocks

import scala.concurrent.{ExecutionContextExecutor, Future}

class NextPageSelectorSpec extends BaseISpec with SessionServiceMocks {
  implicit val mockSessionCacheService: SessionCacheService = mock[SessionCacheService]
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "getNextPage with no previous selections" should {
    "redirect to first page in list" when {
      "all pages selected" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set.empty, 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "address", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService)

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeBusinessName.url)
      }

      "some pages selected" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set.empty, 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService)

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeEmailAddress.url)
      }
    }

    "redirect to next page in list" when {
      "given a page part way through the list" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set.empty, 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "address", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService, "email")

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeTelephoneNumber.url)
      }
    }
  }

  "getNextPage with previous selections" should {
    "redirect to first non-previously selected page in list" when {
      "all pages selected" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set("businessName", "address"), 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "address", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService)

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeEmailAddress.url)
      }
    }

    "redirect to next non-previously selected page in list" when {
      "given a page part way through the list" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set("businessName", "email"), 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "address", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService, "address")

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeTelephoneNumber.url)
      }

      "given a page that was part of the previous journey" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set("businessName", "email"), 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService, "email")

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeTelephoneNumber.url)
      }

      "given a page that shouldn't have been part of journey" in {
        expectGetSessionItem[Set[String]](PREVIOUS_SELECTED_CHANGES, Set("businessName", "email"), 1)
        expectGetSessionItem[Set[String]](CURRENT_SELECTED_CHANGES, Set("businessName", "email", "telephone"), 1)

        val response: Future[Result] = getNextPage(mockSessionCacheService, "address")

        redirectLocation(await(response)) shouldBe Some(updateContactDetails.routes.ContactDetailsController.showChangeTelephoneNumber.url)
      }
    }
  }
}
