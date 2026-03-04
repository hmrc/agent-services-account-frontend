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

import play.api.libs.json.{JsError, JsSuccess, Json, Reads, Writes}
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime.PAYE

sealed trait SubscriptionRequest {
  val agentName: String
  val contactName: String
  val phoneNumber: Option[String]
  val emailAddress: Option[String]
  val address: SubscriptionAddress
  val isAbroad: Boolean
}

object SubscriptionRequest {
  def reads(regime: LegacyRegime): Reads[SubscriptionRequest] = Reads { json =>
    regime match {
      case PAYE => Json.fromJson(json)(Json.reads[PayeSubscriptionRequest])
    }
  }

  implicit val writes: Writes[SubscriptionRequest] = Writes {
    case payeRequest: PayeSubscriptionRequest => Json.writes[PayeSubscriptionRequest].writes(payeRequest)
  }
}

case class PayeSubscriptionRequest(
                                    agentName: String,
                                    contactName: String,
                                    phoneNumber: Option[String],
                                    emailAddress: Option[String],
                                    address: SubscriptionAddress
                                  )
  extends SubscriptionRequest {
  val isAbroad: Boolean = false // Unused value for PAYE as postcode is always required
}

object PayeSubscriptionRequest {
//  given Writes[SubscriptionAddress] = SubscriptionAddress.payeRegistrationWrites
  implicit val registerWrites: Writes[PayeSubscriptionRequest] = Writes { request =>
    Json.obj(
      "agentName" -> request.agentName,
      "contactName" -> request.contactName,
      "telephoneNumber" -> request.phoneNumber,
      "emailAddress" -> request.emailAddress,
      "address" -> request.address
    )
  }
}
