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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class BusinessAddress(
                            addressLine1: String,
                            addressLine2: Option[String],
                            addressLine3: Option[String] = None,
                            addressLine4: Option[String] = None,
                            postalCode: Option[String],
                            countryCode: String)

object BusinessAddress {
  implicit val format: OFormat[BusinessAddress] = Json.format

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[BusinessAddress] =
    (
      (__ \ "addressLine1").format[String](stringEncrypterDecrypter) and
        (__ \ "addressLine2").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "addressLine3").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "addressLine4").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "postalCode").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "countryCode").format[String](stringEncrypterDecrypter)
      )(BusinessAddress.apply, unlift(BusinessAddress.unapply))
}

case class AgencyDetails(
                          agencyName: Option[String],
                          agencyEmail: Option[String],
                          agencyTelephone: Option[String],
                          agencyAddress: Option[BusinessAddress]
                        )

object AgencyDetails {
  implicit val format: OFormat[AgencyDetails] = Json.format

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[AgencyDetails] =
    (
      (__ \ "agencyName").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "agencyEmail").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "agencyTelephone").formatNullable[String](stringEncrypterDecrypter) and
        (__ \ "agencyAddress").formatNullable[BusinessAddress](BusinessAddress.databaseFormat)
      )(AgencyDetails.apply, unlift(AgencyDetails.unapply))
}

