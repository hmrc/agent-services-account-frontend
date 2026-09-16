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

package uk.gov.hmrc.agentservicesaccount.model

import play.api.libs.json.Json
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr

class DesignatoryDetailsSpec
extends UnitSpec {

  val testDesignatoryDetails: DesignatoryDetails = DesignatoryDetails(
    AgencyDetails(
      Some("testName"),
      Some("testEmail"),
      Some("testPhone"),
      Some(BusinessAddress(
        "line1",
        Some("line2"),
        Some("line3"),
        Some("line4"),
        Some("postCode"),
        "countryCode"
      ))
    ),
    OtherServices(
      SaChanges(
        applyChanges = true,
        Some(SaUtr("sautr"))
      ),
      CtChanges(
        applyChanges = true,
        Some(CtUtr("ctutr"))
      )
    )
  )
  val testJson = Json.obj(
    "agencyDetails" -> Json.obj(
      "agencyName" -> "testName",
      "agencyEmail" -> "testEmail",
      "agencyTelephone" -> "testPhone",
      "agencyAddress" -> Json.obj(
        "addressLine1" -> "line1",
        "addressLine2" -> "line2",
        "addressLine3" -> "line3",
        "addressLine4" -> "line4",
        "postalCode" -> "postCode",
        "countryCode" -> "countryCode"
      )
    ),
    "otherServices" -> Json.obj(
      "saChanges" -> Json.obj(
        "applyChanges" -> true,
        "saAgentReference" -> "sautr"
      ),
      "ctChanges" -> Json.obj(
        "applyChanges" -> true,
        "ctAgentReference" -> "ctutr"
      )
    )
  )

  "DesignatoryDetails" when {
    "using default format" should {
      "serialise to Json correctly" in {
        Json.toJson(testDesignatoryDetails) shouldBe testJson
      }
      "deserialise from Json correctly" in {
        testJson.as[DesignatoryDetails] shouldBe testDesignatoryDetails
      }
    }
  }

}
