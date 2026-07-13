/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.model

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.agentservicesaccount.models.*

class Es5GroupAllocatedEnrolmentSpec
extends AnyWordSpec
with Matchers {

  "Es5GroupAllocatedEnrolment JSON format" should {

    "parse enrolmentDate as Instant" in {

      val json = Json.parse("""
        {
          "service": "HMRC-MTD-VAT",
          "status": "Activated",
          "enrolmentDate": "2018-10-05 14:48:00.000"
        }
      """)

      val result = json.validate[Es5GroupAllocatedEnrolment]

      result.isSuccess shouldBe true
    }
  }
}
