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

package uk.gov.hmrc.agentservicesaccount.utils

import enumeratum.Enum
import enumeratum.EnumEntry
import play.api.libs.json._

object EnumFormat {

  @SuppressWarnings(Array(
    "org.wartremover.warts.Product",
    "org.wartremover.warts.Serializable"
  )) // TODO: delete that and use enumeratum-play-json instead of it
  def apply[T <: EnumEntry](e: Enum[T]): Format[T] = Format(
    Reads {
      case JsString(value) => e.withNameOption(value).map[JsResult[T]](JsSuccess(_)).getOrElse(JsError(s"Unknown ${e.getClass.getSimpleName} value: $value"))
      case _ => JsError("Can only parse String")
    },
    Writes(v => JsString(v.entryName))
  )
}
