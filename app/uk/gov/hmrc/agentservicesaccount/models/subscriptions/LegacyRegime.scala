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
sealed trait LegacyRegime

object LegacyRegime {

  case object PAYE
  extends LegacyRegime
  case object SA
  extends LegacyRegime
  case object CT
  extends LegacyRegime

  implicit val format: Format[LegacyRegime] = Format(
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

}
