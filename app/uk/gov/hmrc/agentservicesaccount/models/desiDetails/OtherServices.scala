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
import uk.gov.hmrc.domain.CtUtr
import uk.gov.hmrc.domain.SaUtr

case class CtChanges(
  applyChanges: Boolean,
  ctAgentReference: Option[CtUtr]
)

object CtChanges {

  implicit val ctChangesFormat: OFormat[CtChanges] = Json.format[CtChanges]

  def databaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[CtChanges] = Format(
    Reads { json =>
      for {
        applyChanges <- (json \ "applyChanges").validate[Boolean]
        ctAgentReference <- (json \ "ctAgentReference").validateOpt[String](stringEncrypterDecrypter).map(_.map(CtUtr(_)))
      } yield CtChanges(applyChanges, ctAgentReference)
    },
    OWrites[CtChanges] { ctChanges =>
      Json.obj(
        "applyChanges" -> ctChanges.applyChanges,
        "ctAgentReference" -> ctChanges.ctAgentReference.map(utr => Json.toJson(utr.utr)(stringEncrypterDecrypter)).getOrElse(JsNull)
      )
    }
  )

}

case class SaChanges(
  applyChanges: Boolean,
  saAgentReference: Option[SaUtr]
)

object SaChanges {

  implicit val saCodeChangesFormat: OFormat[SaChanges] = Json.format[SaChanges]

  def databaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[SaChanges] = Format(
    Reads { json =>
      for {
        applyChanges <- (json \ "applyChanges").validate[Boolean]
        saAgentReference <- (json \ "saAgentReference").validateOpt[String](stringEncrypterDecrypter).map(_.map(SaUtr(_)))
      } yield SaChanges(applyChanges, saAgentReference)
    },
    OWrites[SaChanges] { saChanges =>
      Json.obj(
        "applyChanges" -> saChanges.applyChanges,
        "saAgentReference" -> saChanges.saAgentReference.map(utr => Json.toJson(utr.utr)(stringEncrypterDecrypter)).getOrElse(JsNull)
      )
    }
  )

}

case class OtherServices(
  saChanges: SaChanges,
  ctChanges: CtChanges
) {
  val ctOrSaApplied: Boolean = ctChanges.applyChanges || saChanges.applyChanges
}

object OtherServices {

  implicit val otherServicesFormat: OFormat[OtherServices] = Json.format[OtherServices]

  def databaseFormat(implicit
    crypto: Encrypter & Decrypter
  ): Format[OtherServices] = Format(
    Reads { json =>
      for {
        saChanges <- (json \ "saChanges").validate[SaChanges](SaChanges.databaseFormat)
        ctChanges <- (json \ "ctChanges").validate[CtChanges](CtChanges.databaseFormat)
      } yield OtherServices(saChanges, ctChanges)
    },
    OWrites[OtherServices] { otherServices =>
      Json.obj(
        "saChanges" -> Json.toJson(otherServices.saChanges)(SaChanges.databaseFormat),
        "ctChanges" -> Json.toJson(otherServices.ctChanges)(CtChanges.databaseFormat)
      )
    }
  )

}
