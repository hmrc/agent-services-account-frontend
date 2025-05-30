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

import play.api.libs.json._

case class BetaInviteContactDetails(
  name: String,
  email: String,
  phone: Option[String]
)

case class BetaInviteDetailsForEmail(
  numberOfClients: AgentSize,
  name: String,
  email: String,
  phone: Option[String]
)

sealed trait AgentSize {

  // for email
  def toDescription: String =
    this match {
      case Small => "Less than 1000"
      case Medium => "Between 1,001 and 5,000"
      case Large => "Between 5,001 and 9,999"
      case XLarge => "Over 10,000"
      case _ => "Unknown"
    }

  override def toString: String =
    this match {
      case Small => "s"
      case Medium => "m"
      case Large => "l"
      case XLarge => "x"
      case _ => "Unknown"
    }

}

case object Small
extends AgentSize
case object Medium
extends AgentSize
case object Large
extends AgentSize
case object XLarge
extends AgentSize
case class Unknown(attempted: String)
extends AgentSize

object AgentSize {

  def unapply(status: AgentSize): Option[String] =
    status match {
      case Small => Some("small")
      case Medium => Some("medium")
      case Large => Some("large")
      case XLarge => Some("xlarge")
      case _ => None
    }

  def apply(status: String): AgentSize =
    status.toLowerCase match {
      case "small" => Small
      case "medium" => Medium
      case "large" => Large
      case "xlarge" => XLarge
      case _ => Unknown(status)
    }

  implicit val agentSizeFormat: Format[AgentSize] =
    new Format[AgentSize] {
      override def reads(json: JsValue): JsResult[AgentSize] =
        apply(json.as[String]) match {
          case Unknown(value) => JsError(s"Status of [$value] is not a valid AgentSize")
          case value => JsSuccess(value)
        }

      override def writes(o: AgentSize): JsValue = unapply(o).map(JsString).getOrElse(throw new IllegalArgumentException)
    }

}
