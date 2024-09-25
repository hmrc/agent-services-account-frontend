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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, DesignatoryDetails, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress}
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.domain.{CtUtr, SaUtr}

class DesignatoryDetailsSpec extends UnitSpec {

  implicit val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesCrypto("edkOOwt7uvzw1TXnFIN6aRVHkfWcgiOrbBvkEQvO65g=")

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
  val testJson: JsObject = Json.obj(
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
  val testEncryptedJson: JsObject = Json.obj(
    "agencyDetails" -> Json.obj(
      "agencyName" -> "7g352kI4Rfh0Af6Jm7Bl1g==",
      "agencyEmail" -> "BVfLzY//sJgIMS+Frv7dHQ==",
      "agencyTelephone" -> "2NFf8EgEOaFuVowl+Zotcw==",
      "agencyAddress" -> Json.obj(
        "addressLine1" -> "u3kG4I/2HMwvbg6DgKW2NA==",
        "addressLine2" -> "wv13MNc1x64H4C2I7pK/9g==",
        "addressLine3" -> "6hEc2+5vL10UZ5wxTijhdA==",
        "addressLine4" -> "vIlbrtn9c/iM4Xg6+QxdXw==",
        "postalCode" -> "14YJ/1yyIF7SptIAKKJJ6Q==",
        "countryCode" -> "Q80Jvd8jhZzman3gH6Gh0A=="
      )
    ),
    "otherServices" -> Json.obj(
      "saChanges" -> Json.obj(
        "applyChanges" -> true,
        "saAgentReference" -> "r6af9+6cwYaHzCIsbO1VDw=="
      ),
      "ctChanges" -> Json.obj(
        "applyChanges" -> true,
        "ctAgentReference" -> "jHBh7aHi6yaOzzXrUP0+6g=="
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
    "using crypto format" should {
      "serialise to encrypted Json correctly" in {
        Json.toJson(testDesignatoryDetails)(DesignatoryDetails.databaseFormat) shouldBe testEncryptedJson
      }
      "deserialise from encrypted Json correctly" in {
        testEncryptedJson.as[DesignatoryDetails](DesignatoryDetails.databaseFormat) shouldBe testDesignatoryDetails
      }
    }
  }
}
