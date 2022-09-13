/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.connectors

import play.api.test.Injecting
import uk.gov.hmrc.agentservicesaccount.models.UserDetails
import uk.gov.hmrc.agentservicesaccount.stubs.UserDetailsStubs._
import uk.gov.hmrc.agentservicesaccount.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier

class UserDetailsConnectorSpec extends BaseISpec with Injecting {

  val connector: UserDetailsConnector = inject[UserDetailsConnector]

  "Fetching user details" when {

    "connector returns 200" should {
      "return user details" in {
        val userId = "abcd"
        val stubbedUserDetails = UserDetails(userId = Option(userId), name = "First Last",
          email = Option("id@domain.com"), credentialRole = Option("User"))

        givenUserDetailsFetchedOk(userId, stubbedUserDetails)

        connector.getUserDetails(userId)(HeaderCarrier()).futureValue shouldBe Some(stubbedUserDetails)
      }
    }

    "connector returns 4xx" should {
      "not return user details" in {
        val userId = "abcd"

        givenUserDetailsNotFound(userId)

        connector.getUserDetails(userId)(HeaderCarrier()).futureValue shouldBe None
      }
    }

    "connector returns 5xx" should {
      "not return user details" in {
        val userId = "abcd"

        givenServerErrorWhenFetchingUserDetails(userId)

        connector.getUserDetails(userId)(HeaderCarrier()).futureValue shouldBe None
      }
    }

  }
}
