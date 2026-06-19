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
import play.api.libs.json.Writes

sealed trait SubscriptionRequest {

  val agentName: String
  val contactName: String
  val phoneNumber: Option[String]
  val emailAddress: Option[String]
  val address: SubscriptionAddress
  val isWelsh: Boolean
  def isAbroad: Boolean

}

object SubscriptionRequest {

  implicit val writes: Writes[SubscriptionRequest] = Json.writes[SubscriptionRequest]

}

case class PayeSubscriptionRequest(
  agentName: String,
  contactName: String,
  phoneNumber: Option[String],
  emailAddress: Option[String],
  address: SubscriptionAddress,
  isWelsh: Boolean
)
extends SubscriptionRequest {
  val isAbroad: Boolean = false // Unused value for PAYE as postcode is always required
}

object PayeSubscriptionRequest {
  implicit val writes: Writes[PayeSubscriptionRequest] = Writes { request =>
    Json.obj(
      "agentName" -> request.agentName,
      "contactName" -> request.contactName,
      "phoneNumber" -> request.phoneNumber,
      "emailAddress" -> request.emailAddress,
      "address" -> request.address,
      "isWelsh" -> request.isWelsh
    )
  }
}

case class CtSubscriptionRequest(
  agentName: String,
  contactName: String,
  phoneNumber: Option[String],
  emailAddress: Option[String],
  address: SubscriptionAddress,
  countryCode: String,
  isWelsh: Boolean
)
extends SubscriptionRequest {
  override def isAbroad: Boolean = !countryCode.equalsIgnoreCase("GB")
}

object CtSubscriptionRequest {
  implicit val writes: Writes[CtSubscriptionRequest] = Writes { request =>
    Json.obj(
      "agentName" -> request.agentName,
      "contactName" -> request.contactName,
      "phoneNumber" -> request.phoneNumber,
      "emailAddress" -> request.emailAddress,
      "address" -> request.address,
      "isAbroad" -> request.isAbroad,
      "isWelsh" -> request.isWelsh
    )
  }
}

case class SaSubscriptionRequest(
  agentName: String,
  contactName: String,
  phoneNumber: Option[String],
  emailAddress: Option[String],
  address: SubscriptionAddress,
  countryCode: String,
  isWelsh: Boolean
)
extends SubscriptionRequest {
  override def isAbroad: Boolean = !countryCode.equalsIgnoreCase("GB")
}

object SaSubscriptionRequest {
  implicit val writes: Writes[SaSubscriptionRequest] = Writes { request =>
    Json.obj(
      "agentName" -> request.agentName,
      "contactName" -> request.contactName,
      "phoneNumber" -> request.phoneNumber,
      "emailAddress" -> request.emailAddress,
      "address" -> request.address,
      "isAbroad" -> request.isAbroad,
      "isWelsh" -> request.isWelsh
    )
  }
}
