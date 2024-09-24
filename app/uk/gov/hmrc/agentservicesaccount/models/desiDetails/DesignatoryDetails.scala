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
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

case class DesignatoryDetails(
                               agencyDetails: AgencyDetails,
                               otherServices: OtherServices
                             )

object DesignatoryDetails {
  implicit val desiDetailsFormat: OFormat[DesignatoryDetails] = Json.format[DesignatoryDetails]

  def databaseFormat(implicit crypto: Encrypter with Decrypter): Format[DesignatoryDetails] =
    (
      (__ \ "agencyDetails").format[AgencyDetails](AgencyDetails.databaseFormat) and
        (__ \ "otherServices").format[OtherServices](OtherServices.databaseFormat)
      )(DesignatoryDetails.apply, unlift(DesignatoryDetails.unapply))
}
