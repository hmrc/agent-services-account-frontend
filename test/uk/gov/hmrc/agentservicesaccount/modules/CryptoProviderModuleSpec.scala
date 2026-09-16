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

package uk.gov.hmrc.agentservicesaccount.modules

import com.typesafe.config.ConfigFactory
import play.api.Configuration
import support.UnitSpec
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

class CryptoProviderModuleSpec
extends UnitSpec {

  private val module = new CryptoProviderModule
  private val fieldLevelEncryptionKey = "edkOOwt7uvzw1TXnFIN6aRVHkfWcgiOrbBvkEQvO65g="

  "aesCryptoInstance" should {
    "encrypt new values with AES-GCM from field level encryption config" in {
      val crypto = module.aesCryptoInstance(configuration(fieldLevelEncryptionEnabled = true))

      val firstEncrypted = crypto.encrypt(PlainText("user-answer"))
      val secondEncrypted = crypto.encrypt(PlainText("user-answer"))

      firstEncrypted.value should not be "user-answer"
      secondEncrypted.value should not be "user-answer"
      firstEncrypted should not be secondEncrypted
      crypto.decrypt(firstEncrypted).value shouldBe "user-answer"
      crypto.decrypt(secondEncrypted).value shouldBe "user-answer"
    }

    "decrypt old AES values using the same field level encryption key as fallback" in {
      val oldAesCrypto = SymmetricCryptoFactory.aesCrypto(fieldLevelEncryptionKey)
      val oldAesEncrypted = oldAesCrypto.encrypt(PlainText("pre-migration-answer"))

      val crypto = module.aesCryptoInstance(configuration(fieldLevelEncryptionEnabled = true))

      crypto.decrypt(oldAesEncrypted).value shouldBe "pre-migration-answer"
    }

    "leave values unencrypted when field level encryption is disabled" in {
      val crypto = module.aesCryptoInstance(configuration(fieldLevelEncryptionEnabled = false))

      val encrypted = crypto.encrypt(PlainText("user-answer"))

      encrypted.value shouldBe "user-answer"
      crypto.decrypt(encrypted).value shouldBe "user-answer"
    }
  }

  private def configuration(fieldLevelEncryptionEnabled: Boolean): Configuration =
    Configuration(
      ConfigFactory.parseString(s"""
                                   |fieldLevelEncryption {
                                   |  enable = $fieldLevelEncryptionEnabled
                                   |  key = "$fieldLevelEncryptionKey"
                                   |}
                                   |""".stripMargin)
    )

}
