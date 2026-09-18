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
import play.api.mvc.PathBindable

// TODO when migrating to scala 3, replace this with the backend model from agent-services-account
sealed trait AgentRegime {

  def msgPrefix: String

  def enrolmentKey: String =
    this match {
      case AgentRegime.PAYE => "IR-PAYE-AGENT"
      case AgentRegime.SA => "IR-SA-AGENT"
      case AgentRegime.CT => "IR-CT-AGENT"
    }

  def agentReferenceKey: String =
    this match {
      case AgentRegime.PAYE => "IRAgentReference"
      case AgentRegime.SA => "IRAgentReference"
      case AgentRegime.CT => "IRAgentReference"
    }

}

object AgentRegime {

  case object PAYE
  extends AgentRegime {
    override def msgPrefix: String = s"asa.agent-regime.${PAYE.toString.toLowerCase}"
  }
  case object SA
  extends AgentRegime {
    override def msgPrefix: String = s"asa.agent-regime.${SA.toString.toLowerCase}"
  }
  case object CT
  extends AgentRegime {
    override def msgPrefix: String = s"asa.agent-regime.${CT.toString.toLowerCase}"
  }

  implicit val format: Format[AgentRegime] = Format(
    Reads { json =>
      json.as[String] match {
        case "PAYE" => JsSuccess(PAYE)
        case "SA" => JsSuccess(SA)
        case "CT" => JsSuccess(CT)
        case _ => throw new RuntimeException(s"Unknown regime: ${json.as[String]}")
      }
    },
    Writes { regime =>
      JsString(regime.toString)
    }
  )

  implicit val agentRegimeBinder: PathBindable[AgentRegime] =
    new PathBindable[AgentRegime] {

      override def bind(
        key: String,
        value: String
      ): Either[String, AgentRegime] = fromString(value).toRight(s"Unknown regime: $value")

      override def unbind(
        key: String,
        value: AgentRegime
      ): String = value.toString
    }

  def fromString(value: String): Option[AgentRegime] =
    value match {
      case "PAYE" => Some(PAYE)
      case "SA" => Some(SA)
      case "CT" => Some(CT)
      case _ => None
    }

}
