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

package uk.gov.hmrc.agentservicesaccount

import uk.gov.hmrc.agentservicesaccount.models.UpdateAmlsJourney
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.YourDetails
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.Future

package object controllers {

  implicit class ToFuture[T](t: T) {
    def toFuture: Future[T] = Future successful t
  }

  val agentSizeKey: DataKey[String] = DataKey("numberOfClients")
  val nameKey: DataKey[String] = DataKey("name")
  val emailKey: DataKey[String] = DataKey("email")
  val phoneKey: DataKey[String] = DataKey("phone")
  val arnKey: DataKey[String] = DataKey("arn")
  val descriptionKey: DataKey[String] = DataKey("description")

  // when the user changes their own details, this is a 'draft' of the set of updated details before the user sends the update request
  val draftNewContactDetailsKey: DataKey[DesignatoryDetails] = DataKey("updatedContactDetails")
  val draftSubmittedByKey: DataKey[YourDetails] = DataKey("submittedBy")

  // when the user chooses which details to change, this is set of each detail they have selected
  val currentSelectedChangesKey: DataKey[Set[String]] = DataKey("currentSelectedChanges")

  // after an email verification request has been sent, this value is set to keep track of which address was being verified
  val emailPendingVerificationKey: DataKey[String] = DataKey("emailPendingVerification")

  val amlsJourneyKey: DataKey[UpdateAmlsJourney] = DataKey[UpdateAmlsJourney]("amlsJourney")

  val sessionKeys: List[DataKey[String]] = List(
    agentSizeKey,
    nameKey,
    emailKey,
    phoneKey,
    arnKey,
    descriptionKey
  )

}
