/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.mongo.cache.DataKey

import java.time.LocalDate
import scala.concurrent.Future

package object controllers {
  implicit class ToFuture[T](t: T) {
    def toFuture: Future[T] = Future successful t
  }

  val AGENT_SIZE: DataKey[String] = DataKey("numberOfClients")
  val NAME: DataKey[String] = DataKey("name")
  val EMAIL: DataKey[String] = DataKey("email")
  val PHONE: DataKey[String] = DataKey("phone")
  val ARN: DataKey[String] = DataKey("arn")
  val DESCRIPTION: DataKey[String] = DataKey("description")
  val BODY: DataKey[String] = DataKey("body")
  val REG_NUMBER: DataKey[String] = DataKey("number")
  val END_DATE: DataKey[LocalDate] = DataKey("endDate")

  // when the user changes their own details, this is a 'draft' of the set of updated details before the user sends the update request
  val DRAFT_NEW_CONTACT_DETAILS: DataKey[DesignatoryDetails] = DataKey("updatedContactDetails")

  // after an email verification request has been sent, this value is set to keep track of which address was being verified
  val EMAIL_PENDING_VERIFICATION: DataKey[String] = DataKey("emailPendingVerification")

  val sessionKeys =
    List(
      AGENT_SIZE,
      NAME,
      EMAIL,
      PHONE,
      ARN,
      DESCRIPTION
    )

}
