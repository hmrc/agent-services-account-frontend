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
  implicit class AmlsStatusExt(amlsStatus: => AmlsStatus) {

    def isUkAgent(): Boolean =
      amlsStatus match {
        case AmlsStatuses.NoAmlsDetailsUK => true
        case AmlsStatuses.ValidAmlsDetailsUK => true
        case AmlsStatuses.ExpiredAmlsDetailsUK => true
        case AmlsStatuses.PendingAmlsDetails => true
        case AmlsStatuses.PendingAmlsDetailsRejected => true
        case AmlsStatuses.NoAmlsDetailsNonUK => false
        case AmlsStatuses.ValidAmlsNonUK => false
      }

    def isValid(): Boolean =
      amlsStatus match {
        case AmlsStatuses.ValidAmlsNonUK => true
        case AmlsStatuses.ValidAmlsDetailsUK => true
        case AmlsStatuses.NoAmlsDetailsNonUK => false
        case AmlsStatuses.NoAmlsDetailsUK => false
        case AmlsStatuses.ExpiredAmlsDetailsUK => false
        case AmlsStatuses.PendingAmlsDetails => false
        case AmlsStatuses.PendingAmlsDetailsRejected => false
      }

    def isExpired(): Boolean =
      amlsStatus match {
        case AmlsStatuses.ExpiredAmlsDetailsUK => true
        case AmlsStatuses.ValidAmlsNonUK => false
        case AmlsStatuses.ValidAmlsDetailsUK => false
        case AmlsStatuses.NoAmlsDetailsNonUK => false
        case AmlsStatuses.NoAmlsDetailsUK => false
        case AmlsStatuses.PendingAmlsDetails => false
        case AmlsStatuses.PendingAmlsDetailsRejected => false
      }

    def isNoDetails(): Boolean =
      amlsStatus match {
        case AmlsStatuses.NoAmlsDetailsNonUK => true
        case AmlsStatuses.NoAmlsDetailsUK => true
        case AmlsStatuses.ValidAmlsNonUK => false
        case AmlsStatuses.ValidAmlsDetailsUK => false
        case AmlsStatuses.ExpiredAmlsDetailsUK => false
        case AmlsStatuses.PendingAmlsDetails => false
        case AmlsStatuses.PendingAmlsDetailsRejected => false
      }

    def hasExistingAmls(): Boolean =
      amlsStatus match {
        case AmlsStatuses.NoAmlsDetailsNonUK => false
        case AmlsStatuses.ValidAmlsNonUK => true
        case AmlsStatuses.NoAmlsDetailsUK => false
        case AmlsStatuses.ValidAmlsDetailsUK => true
        case AmlsStatuses.ExpiredAmlsDetailsUK => true
        case AmlsStatuses.PendingAmlsDetails => true
        case AmlsStatuses.PendingAmlsDetailsRejected => true
      }

  }

}
