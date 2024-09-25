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

package uk.gov.hmrc.agentservicesaccount.models.desiDetails

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, Json, OFormat, __}
import uk.gov.hmrc.crypto.json.JsonEncryption.stringEncrypterDecrypter
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.domain.{CtUtr, SaUtr}

case class CtChanges(
                      applyChanges: Boolean,
                      ctAgentReference: Option[CtUtr]
                    )

object CtChanges {
  implicit val ctChangesFormat: OFormat[CtChanges] = Json.format[CtChanges]

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[CtChanges] =
    (
      (__ \ "applyChanges").format[Boolean] and
        (__ \ "ctAgentReference").formatNullable[String](stringEncrypterDecrypter)
          .bimap[Option[CtUtr]](
            _.map(CtUtr(_)),
            _.map(_.utr)
          )
      )(CtChanges.apply, unlift(CtChanges.unapply))
}


case class SaChanges(
                      applyChanges: Boolean,
                      saAgentReference: Option[SaUtr]
                    )

object SaChanges {
  implicit val saCodeChangesFormat: OFormat[SaChanges] = Json.format[SaChanges]

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[SaChanges] =
    (
      (__ \ "applyChanges").format[Boolean] and
        (__ \ "saAgentReference").formatNullable[String](stringEncrypterDecrypter)
          .bimap[Option[SaUtr]](
            _.map(SaUtr(_)),
            _.map(_.utr)
          )
      )(SaChanges.apply, unlift(SaChanges.unapply))
}


case class OtherServices(
                          saChanges: SaChanges,
                          ctChanges: CtChanges
                        )

object OtherServices {
  implicit val otherServicesFormat: OFormat[OtherServices] = Json.format[OtherServices]

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[OtherServices] =
    (
      (__ \ "saChanges").format[SaChanges](SaChanges.databaseFormat) and
        (__ \ "ctChanges").format[CtChanges](CtChanges.databaseFormat)
      )(OtherServices.apply, unlift(OtherServices.unapply))
}
