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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.{CtUtr, SaUtr}

case class CtChanges(
                      applyChanges: Boolean,
                      ctAgentReference:Option[CtUtr]
                    )
object CtChanges {
  implicit val ctChangesFormat: OFormat[CtChanges] = Json.format[CtChanges]
}


case class SaChanges(
                      applyChanges: Boolean,
                      saAgentReference:Option[SaUtr]
                    )
object SaChanges {
  implicit val saCodeChangesFormat: OFormat[SaChanges] = Json.format[SaChanges]
}


case class OtherServices (
                      saChanges: SaChanges,
                      ctChanges: CtChanges
                    )

object OtherServices {
  implicit val otherServicesFormat: OFormat[OtherServices] = Json.format[OtherServices]
}
