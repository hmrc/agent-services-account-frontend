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

package utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, OtherServices, SaChanges, YourDetails}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.utils.XmlUtils
import uk.gov.hmrc.domain.{CtUtr, SaUtr}

import java.time.Instant

class XmlUtilsSpec extends PlaySpec {

  val time: Instant = Instant.now()

  val input: PendingChangeOfDetails = PendingChangeOfDetails(
    arn = Arn("XARN0077777"),
    oldDetails = AgencyDetails(
      agencyName = Some("GREENDALE ACCOUNTING &<>\"' TAILORING LTD"),
      agencyEmail = Some("agency@greendale.net"),
      agencyTelephone = None,
      agencyAddress = Some(BusinessAddress(
        addressLine1 = "SPACE &<>\"' TIME",
        addressLine2 = Some("1 GREENDALE &<>\"' ROAD"),
        addressLine3 = Some("GREEN &<>\"' DALE"),
        addressLine4 = Some("PLANET &<>\"' EARTH"),
        postalCode = Some("QL4 5EE"),
        countryCode = "GB"
      ))
    ),
    newDetails = AgencyDetails(
      agencyName = Some("Greendale Accounting &<>\"' Tailoring Ltd"),
      agencyEmail = None,
      agencyTelephone = None,
      agencyAddress = Some(BusinessAddress(
        addressLine1 = "2 &<>\"' Lane",
        addressLine2 = Some("Green &<>\"' dale"),
        addressLine3 = Some("APPLES &<>\"'"),
        addressLine4 = Some("&<>\"' LEMONS"),
        postalCode = Some("QL1 5RR"),
        countryCode = "GB"
      ))
    ),
    otherServices = OtherServices(
      saChanges = SaChanges(
        applyChanges = true,
        saAgentReference = Some(SaUtr("1234HB"))
      ),
      ctChanges = CtChanges(
        applyChanges = true,
        ctAgentReference = Some(CtUtr("F1234L"))
      )
    ),
    timeSubmitted = time,
    submittedBy = YourDetails(
      fullName = "Troy Barnes",
      telephone = "01726354656"
    )
  )

  val expectedOutput: PendingChangeOfDetails = PendingChangeOfDetails(
    arn = Arn("XARN0077777"),
    oldDetails = AgencyDetails(
      agencyName = Some("GREENDALE ACCOUNTING &<>\"' TAILORING LTD"),
      agencyEmail = Some("agency@greendale.net"),
      agencyTelephone = None,
      agencyAddress = Some(BusinessAddress(
        addressLine1 = "SPACE &amp;&lt;&gt;&quot;&apos; TIME",
        addressLine2 = Some("1 GREENDALE &amp;&lt;&gt;&quot;&apos; ROAD"),
        addressLine3 = Some("GREEN &amp;&lt;&gt;&quot;&apos; DALE"),
        addressLine4 = Some("PLANET &amp;&lt;&gt;&quot;&apos; EARTH"),
        postalCode = Some("QL4 5EE"),
        countryCode = "GB"
      ))
    ),
    newDetails = AgencyDetails(
      agencyName = Some("Greendale Accounting &<>\"' Tailoring Ltd"),
      agencyEmail = None,
      agencyTelephone = None,
      agencyAddress = Some(BusinessAddress(
        addressLine1 = "2 &amp;&lt;&gt;&quot;&apos; Lane",
        addressLine2 = Some("Green &amp;&lt;&gt;&quot;&apos; dale"),
        addressLine3 = Some("APPLES &amp;&lt;&gt;&quot;&apos;"),
        addressLine4 = Some("&amp;&lt;&gt;&quot;&apos; LEMONS"),
        postalCode = Some("QL1 5RR"),
        countryCode = "GB"
      ))
    ),
    otherServices = OtherServices(
      saChanges = SaChanges(
        applyChanges = true,
        saAgentReference = Some(SaUtr("1234HB"))
      ),
      ctChanges = CtChanges(
        applyChanges = true,
        ctAgentReference = Some(CtUtr("F1234L"))
      )
    ),
    timeSubmitted = time,
    submittedBy = YourDetails(
      fullName = "Troy Barnes",
      telephone = "01726354656"
    )
  )

  "XmlUtils.escapeXmlFor" should {
    "escape all XML characters in all address lines" in {
      XmlUtils.escapeXmlFor(input).toString mustBe expectedOutput.toString
    }
  }

}
