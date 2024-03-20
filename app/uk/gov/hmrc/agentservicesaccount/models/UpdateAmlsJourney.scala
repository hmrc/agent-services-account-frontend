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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate


case class UpdateAmlsJourney(status: AmlsStatus,
                             isAmlsBodyStillTheSame: Option[Boolean] = None,
                             newAmlsBody: Option[String] = None,
                             isRegistrationNumberStillTheSame: Option[Boolean] = None,
                             newRegistrationNumber: Option[String] = None,
                             newExpirationDate: Option[LocalDate] = None
                            ){

  val isUkAgent: Boolean = status match {
    case AmlsStatus.NoAmlsDetailsNonUK => false
    case AmlsStatus.ValidAmlsNonUK => false
    case AmlsStatus.NoAmlsDetailsUK => true
    case AmlsStatus.ValidAmlsDetailsUK => true
    case AmlsStatus.ExpiredAmlsDetailsUK => true
    case AmlsStatus.PendingAmlsDetails => true
    case AmlsStatus.PendingAmlsDetailsRejected => true
  }

  val isHmrc:Boolean = status match {
    case AmlsStatus.NoAmlsDetailsNonUK => false
    case AmlsStatus.ValidAmlsNonUK => false
    case AmlsStatus.NoAmlsDetailsUK => false
    case AmlsStatus.ValidAmlsDetailsUK => false
    case AmlsStatus.ExpiredAmlsDetailsUK => false
    case AmlsStatus.PendingAmlsDetails => true
    case AmlsStatus.PendingAmlsDetailsRejected => true
  }

  val hasExistingAmls: Boolean = status match {
    case AmlsStatus.NoAmlsDetailsNonUK => false
    case AmlsStatus.ValidAmlsNonUK => true
    case AmlsStatus.NoAmlsDetailsUK => false
    case AmlsStatus.ValidAmlsDetailsUK => true
    case AmlsStatus.ExpiredAmlsDetailsUK => true
    case AmlsStatus.PendingAmlsDetails => true
    case AmlsStatus.PendingAmlsDetailsRejected => true
  }
}

object UpdateAmlsJourney{
  implicit val format: OFormat[UpdateAmlsJourney] = Json.format[UpdateAmlsJourney]
}
