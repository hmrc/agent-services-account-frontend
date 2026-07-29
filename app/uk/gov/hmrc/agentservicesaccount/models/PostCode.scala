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

package uk.gov.hmrc.agentservicesaccount.models

import play.api.data.Mapping
import uk.gov.hmrc.agentservicesaccount.forms.CommonValidators.trimmedText
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime

object PostCode {

  private val ValidPostcodeRegex = """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}$|BFPO\s?[0-9]{1,5}$""".r

  /** Validation for forms using a [[PostCode]]
    *
    * @param legacyRegime
    *   the legacy tax regime (PAYE, SA, or CT), used to scope the error message keys (e.g. `asa.legacy.paye.error.postcode`)
    * @return
    *   a valid postcode
    */
  def mapping(legacyRegime: LegacyRegime): Mapping[String] =
    val postCodeKey = s"${legacyRegime.msgPrefix}.error.postcode"
    trimmedText
      .verifying(s"$postCodeKey.required", _.nonEmpty)
      .verifying(s"$postCodeKey.invalid", ValidPostcodeRegex.matches)

  /** Format a postcode to the standard format (e.g. `SW1A 1AA`).
    *
    * @param value
    *   the un-normalised postcode
    * @return
    *   the normalised postcode
    */
  def normalise(value: String): String =
    val withoutSpaces = value.replaceAll("\\s+", "").toUpperCase
    val midpoint = withoutSpaces.sizeIs match {
      case 5 => 2
      case 6 => 3
      case _ => 4
    }
    val (incode, outcode) = withoutSpaces.splitAt(midpoint)

    s"$incode $outcode"

}
