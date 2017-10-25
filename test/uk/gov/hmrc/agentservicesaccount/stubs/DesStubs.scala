/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault

object DesStubs {

  def givenDESRespondsWithAgencyName(arn: String, agencyName: String) = {
    stubFor(get(urlEqualTo(s"/registration/personal-details/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(
            s"""
              |{
              |   "isAnOrganisation" : true,
              |   "contactDetails" : {
              |      "phoneNumber" : "07000000000"
              |   },
              |   "isAnAgent" : true,
              |   "safeId" : "XB0000100101711",
              |   "agencyDetails" : {
              |      "agencyAddress" : {
              |         "addressLine2" : "Grange Central",
              |         "addressLine3" : "Town Centre",
              |         "addressLine4" : "Telford",
              |         "postalCode" : "TF3 4ER",
              |         "countryCode" : "GB",
              |         "addressLine1" : "Matheson House"
              |      },
              |      "agencyName" : "${agencyName}",
              |      "agencyEmail" : "abc@xyz.com"
              |   },
              |   "organisation" : {
              |      "organisationName" : "CT AGENT 183",
              |      "isAGroup" : false,
              |      "organisationType" : "0000"
              |   },
              |   "addressDetails" : {
              |      "addressLine2" : "Grange Central 183",
              |      "addressLine3" : "Telford 183",
              |      "addressLine4" : "Shropshire 183",
              |      "postalCode" : "TF3 4ER",
              |      "countryCode" : "GB",
              |      "addressLine1" : "Matheson House 183"
              |   },
              |   "isAnASAgent" : true,
              |   "isAnIndividual" : false,
              |   "businessPartnerExists" : true,
              |   "agentReferenceNumber" : "${arn}"
              |}
            """.
              stripMargin)))
    }

    def givenDESRespondsWithoutAgencyDetails(arn: String) = {
      stubFor(get(urlEqualTo(s"/registration/personal-details/arn/$arn"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""
                 |{
                 |   "isAnOrganisation" : true,
                 |   "contactDetails" : {
                 |      "phoneNumber" : "07000000000"
                 |   },
                 |   "isAnAgent" : false,
                 |   "safeId" : "XB0000100101711",
                 |   "organisation" : {
                 |      "organisationName" : "CT AGENT 183",
                 |      "isAGroup" : false,
                 |      "organisationType" : "0000"
                 |   },
                 |   "addressDetails" : {
                 |      "addressLine2" : "Grange Central 183",
                 |      "addressLine3" : "Telford 183",
                 |      "addressLine4" : "Shropshire 183",
                 |      "postalCode" : "TF3 4ER",
                 |      "countryCode" : "GB",
                 |      "addressLine1" : "Matheson House 183"
                 |   },
                 |   "isAnASAgent" : false,
                 |   "isAnIndividual" : false,
                 |   "businessPartnerExists" : true
                 |}
            """.stripMargin)))
  }

  def givenDESReturnsError(arn: String, responseCode: Int) = {
    stubFor(get(urlEqualTo(s"/registration/personal-details/arn/$arn"))
      .willReturn(
        aResponse()
          .withStatus(responseCode)
          .withBody(
            """
              |{
              |   "code" : "SOME_FAILURE",
              |   "reason" : "Some reason"
              |}
            """.stripMargin)))
  }
}
