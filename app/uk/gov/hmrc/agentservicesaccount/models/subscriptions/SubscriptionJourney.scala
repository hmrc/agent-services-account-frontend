/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models.subscriptions

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress

case class SubscriptionJourney(
  asaDetails: AgencyDetails,
  payeContactName: Option[String] = None,
  useCustomBusinessName: Option[Boolean] = None,
  businessNameAnswer: Option[String] = None,
  useCustomPhoneNumber: Option[Boolean] = None,
  phoneNumberAnswer: Option[String] = None,
  useCustomEmail: Option[Boolean] = None,
  emailAnswer: Option[String] = None,
  useCustomAddress: Option[Boolean] = None,
  addressAnswer: Option[BusinessAddress] = None
) {

  private def answerComplete(
    booleanField: Option[Boolean],
    newAnswerField: Option[_]
  ): Boolean = {
    (booleanField, newAnswerField) match {
      case (Some(false), _) => true
      case (Some(true), Some(_)) => true
      case _ => false
    }
  }

  //      TODO: 11186 Implement for PAYE
  def isComplete(legacyRegime: LegacyRegime): Boolean = {
    val bnComplete = answerComplete(useCustomBusinessName, businessNameAnswer)
    val pnComplete = answerComplete(useCustomPhoneNumber, phoneNumberAnswer)
    val eaComplete = answerComplete(useCustomEmail, emailAnswer)
    val addressComplete = answerComplete(useCustomAddress, addressAnswer)
    bnComplete && pnComplete && eaComplete && addressComplete
  }

}

object SubscriptionJourney {
  implicit val format: OFormat[SubscriptionJourney] = Json.format[SubscriptionJourney]
}
