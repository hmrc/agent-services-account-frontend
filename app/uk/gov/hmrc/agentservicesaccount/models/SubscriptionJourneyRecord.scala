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

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, OFormat}


/**
 * A Mongo record which represents the user's current journey in setting up a new
 * MTD Agent Services account, with their existing relationships.
 *
 */
final case class SubscriptionJourneyRecord(
                                            authProviderId: AuthProviderId,
                                            businessDetails: BusinessDetails
                                          )

object SubscriptionJourneyRecord {


  implicit val subscriptionJourneyFormat: OFormat[SubscriptionJourneyRecord] =
    ((JsPath \ "authProviderId").format[AuthProviderId] and
      (JsPath \ "businessDetails").format[BusinessDetails])(SubscriptionJourneyRecord.apply, unlift(SubscriptionJourneyRecord.unapply))

}


import play.api.libs.json.{Json}
import uk.gov.hmrc.agentmtdidentifiers.model.Utr


/**
 * Information about the agent's business.  They must always provide a business type, UTR and postcode.
 * But other data points are only required for some business types and if certain conditions are NOT met
 * e.g.
 *   if they provide a NINO, they must provide date of birth
 *   if they are registered for vat, they must provide vat details
 * The record is created once we have the minimum business details
 */
case class BusinessDetails(
                            utr: Utr)

object BusinessDetails {
  implicit val format: OFormat[BusinessDetails] = Json.format
}
