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

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import support.UnitSpec
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

class YourDetailsSpec
extends UnitSpec {

  implicit val crypto: Encrypter
    with Decrypter = SymmetricCryptoFactory.aesCrypto("edkOOwt7uvzw1TXnFIN6aRVHkfWcgiOrbBvkEQvO65g=")

  val testYourDetails: YourDetails = YourDetails(
    "testName",
    "testPhone"
  )
  val testJson: JsObject = Json.obj(
    "fullName" -> "testName",
    "telephone" -> "testPhone"
  )
  val testEncryptedJson: JsObject = Json.obj(
    "fullName" -> "7g352kI4Rfh0Af6Jm7Bl1g==",
    "telephone" -> "2NFf8EgEOaFuVowl+Zotcw=="
  )
  "YourDetails" when {
    "using default format" should {
      "serialise to Json correctly" in {
        Json.toJson(testYourDetails) shouldBe testJson
      }
      "deserialise from Json correctly" in {
        testJson.as[YourDetails] shouldBe testYourDetails
      }
    }
    "using crypto format" should {
      "serialise to encrypted Json correctly" in {
        Json.toJson(testYourDetails)(YourDetails.databaseFormat) shouldBe testEncryptedJson
      }
      "deserialise from encrypted Json correctly" in {
        testEncryptedJson.as[YourDetails](YourDetails.databaseFormat) shouldBe testYourDetails
      }
    }
  }

}
