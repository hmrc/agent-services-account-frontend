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

package uk.gov.hmrc.agentservicesaccount.models.addresslookup

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads.min
import play.api.libs.json._

case class JourneyConfigV2(
  version: Int,
  options: JourneyOptions,
  labels: Option[JourneyLabels] = None, // messages
  requestedVersion: Option[Int] = None
)

case class JourneyOptions(
  continueUrl: String,
  homeNavHref: Option[String] = None,
  signOutHref: Option[String] = None,
  accessibilityFooterUrl: Option[String] = None,
  phaseFeedbackLink: Option[String] = None,
  deskProServiceName: Option[String] = None,
  showPhaseBanner: Option[Boolean] = None,
  alphaPhase: Option[Boolean] = None,
  showBackButtons: Option[Boolean] = None,
  disableTranslations: Option[Boolean] = None,
  includeHMRCBranding: Option[Boolean] = None,
  ukMode: Option[Boolean] = None,
  allowedCountryCodes: Option[Set[String]] = None,
  selectPageConfig: Option[SelectPageConfig] = None,
  confirmPageConfig: Option[ConfirmPageConfig] = None,
  timeoutConfig: Option[TimeoutConfig] = None,
  serviceHref: Option[String] = None,
  pageHeadingStyle: Option[String] = None
) {

  val isUkMode: Boolean = ukMode contains true

}

case class SelectPageConfig(
  proposalListLimit: Option[Int] = Some(100),
  showSearchAgainLink: Option[Boolean] = None
)

case class ConfirmPageConfig(
  showSearchAgainLink: Option[Boolean] = None,
  showSubHeadingAndInfo: Option[Boolean] = None,
  showChangeLink: Option[Boolean] = None,
  showConfirmChangeText: Option[Boolean] = None
)

case class TimeoutConfig(
  timeoutAmount: Int,
  timeoutUrl: String,
  timeoutKeepAliveUrl: Option[String] = None
)

case class JourneyLabels(
  en: Option[JsObject] = None,
  cy: Option[JsObject] = None
)

object JourneyConfigV2 {

  implicit val labelsFormat: Format[JourneyLabels] = Json.format[JourneyLabels]
  implicit val selectConfigFormat: Format[SelectPageConfig] = Json.format[SelectPageConfig]
  implicit val confirmConfigFormat: Format[ConfirmPageConfig] = Json.format[ConfirmPageConfig]
  implicit val timeoutFormat: Format[TimeoutConfig] =
    (
      (JsPath \ "timeoutAmount").format[Int](min(120)) and
        (JsPath \ "timeoutUrl").format[String] and
        (JsPath \ "timeoutKeepAliveUrl").formatNullable[String]
    )(TimeoutConfig.apply, unlift(TimeoutConfig.unapply))
  implicit val optionsFormat: Format[JourneyOptions] = Json.format[JourneyOptions]
  implicit val format: Format[JourneyConfigV2] = Json.format[JourneyConfigV2]

}
