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

package uk.gov.hmrc.agentservicesaccount.models

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.JsNull
import play.api.libs.json.OFormat
import play.api.libs.json.OWrites
import play.api.libs.json.Reads
import play.api.libs.json.__
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter

case class BusinessAddress(
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postalCode: Option[String],
  countryCode: String
) {

  def isUk: Boolean = countryCode == "GB"

}

object BusinessAddress {

  implicit val format: OFormat[BusinessAddress] = Json.format

  def databaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[BusinessAddress] = Format(
    Reads { json =>
      for {
        addressLine1 <- (json \ "addressLine1").validate[String](stringEncrypterDecrypter)
        addressLine2 <- (json \ "addressLine2").validateOpt[String](stringEncrypterDecrypter)
        addressLine3 <- (json \ "addressLine3").validateOpt[String](stringEncrypterDecrypter)
        addressLine4 <- (json \ "addressLine4").validateOpt[String](stringEncrypterDecrypter)
        postalCode <- (json \ "postalCode").validateOpt[String](stringEncrypterDecrypter)
        countryCode <- (json \ "countryCode").validate[String](stringEncrypterDecrypter)
      } yield BusinessAddress(
        addressLine1,
        addressLine2,
        addressLine3,
        addressLine4,
        postalCode,
        countryCode
      )
    },
    OWrites[BusinessAddress] { businessAddress =>
      Json.obj(
        "addressLine1" -> Json.toJson(businessAddress.addressLine1)(stringEncrypterDecrypter),
        "addressLine2" -> businessAddress.addressLine2.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "addressLine3" -> businessAddress.addressLine3.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "addressLine4" -> businessAddress.addressLine4.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "postalCode" -> businessAddress.postalCode.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "countryCode" -> Json.toJson(businessAddress.countryCode)(stringEncrypterDecrypter)
      )
    }
  )

}

case class AgencyDetails(
  agencyName: Option[String],
  agencyEmail: Option[String],
  agencyTelephone: Option[String],
  agencyAddress: Option[BusinessAddress]
) {
  def isAbroad: Boolean = !agencyAddress.exists(_.countryCode.equalsIgnoreCase("GB"))
}

object AgencyDetails {

  implicit val format: OFormat[AgencyDetails] = Json.format

  def databaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[AgencyDetails] = Format(
    Reads { json =>
      for {
        agencyName <- (json \ "agencyName").validateOpt[String](stringEncrypterDecrypter)
        agencyEmail <- (json \ "agencyEmail").validateOpt[String](stringEncrypterDecrypter)
        agencyTelephone <- (json \ "agencyTelephone").validateOpt[String](stringEncrypterDecrypter)
        agencyAddress <- (json \ "agencyAddress").validateOpt[BusinessAddress](BusinessAddress.databaseFormat)
      } yield AgencyDetails(
        agencyName,
        agencyEmail,
        agencyTelephone,
        agencyAddress
      )
    },
    OWrites[AgencyDetails] { agencyDetails =>
      Json.obj(
        "agencyName" -> agencyDetails.agencyName.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "agencyEmail" -> agencyDetails.agencyEmail.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "agencyTelephone" -> agencyDetails.agencyTelephone.map(v => Json.toJson(v)(stringEncrypterDecrypter)).getOrElse(JsNull),
        "agencyAddress" -> agencyDetails.agencyAddress.map(addr => Json.toJson(addr)(BusinessAddress.databaseFormat)).getOrElse(JsNull)
      )
    }
  )

}
