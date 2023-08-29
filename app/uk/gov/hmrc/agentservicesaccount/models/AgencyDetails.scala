/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

case class BusinessAddress(
                            addressLine1: String,
                            addressLine2: Option[String],
                            addressLine3: Option[String] = None,
                            addressLine4: Option[String]= None,
                            postalCode: Option[String],
                            countryCode: String)

object BusinessAddress {
  implicit val format: OFormat[BusinessAddress] = Json.format
}

case class AgencyDetails(
                          agencyName: Option[String],
                          agencyEmail: Option[String],
                          agencyAddress: Option[BusinessAddress]
                        )

object AgencyDetails {
  implicit val format: OFormat[AgencyDetails] = Json.format
}

case class ContactDetails(phoneNumber: Option[String])

object ContactDetails {
  implicit val detailsFormat: OFormat[ContactDetails] = Json.format[ContactDetails]
}

case class AgencyDetailsResponse(
                                    agencyDetails: Option[AgencyDetails],
                                    contactDetails: Option[ContactDetails]
                                  )

object AgencyDetailsResponse {
  implicit val format: OFormat[AgencyDetailsResponse] = Json.format
}

//a bit easier to handle in the view
case class AccountDetails(
                           phoneNumber: Option[String],
                           agencyName: Option[String],
                           agencyEmail: Option[String],
                           agencyAddress: Option[BusinessAddress]
                        )

object AccountDetails {
  implicit val format: OFormat[AccountDetails] = Json.format

  def maybeFromResponse(maybeResponse: Option[AgencyDetailsResponse]): Option[AccountDetails] = {
    maybeResponse.fold(Option.empty[AccountDetails])(response =>
    Option(AccountDetails(
      response.contactDetails.flatMap(_.phoneNumber),
      response.agencyDetails.flatMap(_.agencyName),
      response.agencyDetails.flatMap(_.agencyEmail),
      response.agencyDetails.flatMap(_.agencyAddress),
    ))
    )
  }
}
