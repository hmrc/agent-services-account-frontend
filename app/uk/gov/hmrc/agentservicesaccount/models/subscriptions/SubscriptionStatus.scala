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

import play.api.libs.json.Format
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.Reads
import play.api.libs.json.Writes

// TODO when migrating to scala 3, replace this with the backend model from agent-services-account
// Keep the Subscribed case unique to the frontend as it's retrieved from auth and not from the backend
sealed trait SubscriptionStatus

object SubscriptionStatus {

  case object Subscribed
  extends SubscriptionStatus
  case object SubscriptionInProgress
  extends SubscriptionStatus
  case object SubscriptionFailed
  extends SubscriptionStatus
  case object SubscriptionMapped
  extends SubscriptionStatus
  case object SubscriptionOnAgency
  extends SubscriptionStatus
  case object NotSubscribed
  extends SubscriptionStatus
  case object InvalidStatus
  extends SubscriptionStatus

  implicit val format: Format[SubscriptionStatus] = Format(
    Reads { json =>
      json.as[String] match {
        case "Subscribed" => JsSuccess(Subscribed)
        case "SubscriptionInProgress" => JsSuccess(SubscriptionInProgress)
        case "SubscriptionFailed" => JsSuccess(SubscriptionFailed)
        case "SubscriptionMapped" => JsSuccess(SubscriptionMapped)
        case "SubscriptionOnAgency" => JsSuccess(SubscriptionOnAgency)
        case "NotSubscribed" => JsSuccess(NotSubscribed)
        case "InvalidStatus" => JsSuccess(InvalidStatus)
        case _ => throw new RuntimeException(s"Unknown subscription status: ${json.as[String]}")
      }
    },
    Writes { subscriptionStatus =>
      JsString(subscriptionStatus.toString)
    }
  )

}
