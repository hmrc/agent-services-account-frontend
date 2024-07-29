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

import org.apache.commons.text.StringEscapeUtils
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}

object XmlUtils {

  private def escapeXml(input: String): String = StringEscapeUtils.escapeXml10(input)

  private def escapeXml(input: Option[String]): Option[String] = input.map(escapeXml)

  private def escapeBusinessAddress(businessAddress: Option[BusinessAddress]): Option[BusinessAddress] = {
    businessAddress.map { address =>
      address.copy(
        addressLine1 = escapeXml(address.addressLine1),
        addressLine2 = escapeXml(address.addressLine2),
        addressLine3 = escapeXml(address.addressLine3),
        addressLine4 = escapeXml(address.addressLine4)
      )
    }
  }

  // the agency name is already being escaped
  private def escapeAgencyDetails(agencyDetails: AgencyDetails): AgencyDetails =
    agencyDetails.copy( agencyAddress = escapeBusinessAddress(agencyDetails.agencyAddress) )

  // XML characters need to be escaped for PDF generation and this needs to be done before it is turned into HTML
  def escapeXmlFor(pendingChangeOfDetails: PendingChangeOfDetails): PendingChangeOfDetails = {
    pendingChangeOfDetails.copy(
      oldDetails = escapeAgencyDetails(pendingChangeOfDetails.oldDetails),
      newDetails = escapeAgencyDetails(pendingChangeOfDetails.newDetails)
    )
  }

}
