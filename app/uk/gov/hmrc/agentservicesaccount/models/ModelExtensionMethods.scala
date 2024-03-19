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

object ModelExtensionMethods {
  implicit class JourneyCardFeesExt(val amlsStatus: AmlsStatus) {

    def isUkAgent(): Boolean = amlsStatus match {
      case AmlsStatus.NoAmlsDetailsUK => true
      case AmlsStatus.ValidAmlsDetailsUK => true
      case AmlsStatus.ExpiredAmlsDetailsUK => true
      case AmlsStatus.PendingAmlsDetails => true
      case AmlsStatus.PendingAmlsDetailsRejected => true
      case AmlsStatus.NoAmlsDetailsNonUK => false
      case AmlsStatus.ValidAmlsNonUK => false
    }

    def isHmrc():Boolean = amlsStatus match {
      case AmlsStatus.PendingAmlsDetails => true
      case AmlsStatus.PendingAmlsDetailsRejected => true
      case AmlsStatus.NoAmlsDetailsNonUK => false
      case AmlsStatus.ValidAmlsNonUK => false
      case AmlsStatus.NoAmlsDetailsUK => false
      case AmlsStatus.ValidAmlsDetailsUK => false
      case AmlsStatus.ExpiredAmlsDetailsUK => false
    }


    def isValid():Boolean = amlsStatus match {
      case AmlsStatus.ValidAmlsNonUK => true
      case AmlsStatus.ValidAmlsDetailsUK => true
      case AmlsStatus.NoAmlsDetailsNonUK => false
      case AmlsStatus.NoAmlsDetailsUK => false
      case AmlsStatus.ExpiredAmlsDetailsUK => false
      case AmlsStatus.PendingAmlsDetails => false
      case AmlsStatus.PendingAmlsDetailsRejected => false
    }

    def isExpired():Boolean = amlsStatus match {
      case AmlsStatus.ExpiredAmlsDetailsUK => true
      case AmlsStatus.ValidAmlsNonUK => false
      case AmlsStatus.ValidAmlsDetailsUK => false
      case AmlsStatus.NoAmlsDetailsNonUK => false
      case AmlsStatus.NoAmlsDetailsUK => false
      case AmlsStatus.PendingAmlsDetails => false
      case AmlsStatus.PendingAmlsDetailsRejected => false
    }

    def isNoDetails():Boolean = amlsStatus match {
      case AmlsStatus.NoAmlsDetailsNonUK => true
      case AmlsStatus.NoAmlsDetailsUK => true
      case AmlsStatus.ValidAmlsNonUK => false
      case AmlsStatus.ValidAmlsDetailsUK => false
      case AmlsStatus.ExpiredAmlsDetailsUK => false
      case AmlsStatus.PendingAmlsDetails => false
      case AmlsStatus.PendingAmlsDetailsRejected => false
    }


  }

}



