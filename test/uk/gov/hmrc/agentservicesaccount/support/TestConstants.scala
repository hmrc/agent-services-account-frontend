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

package uk.gov.hmrc.agentservicesaccount.support

import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentmtdidentifiers.model.SuspensionDetails
import uk.gov.hmrc.agentmtdidentifiers.model.Utr
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.CtChanges
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.DesignatoryDetails
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.OtherServices
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.SaChanges
import uk.gov.hmrc.agentservicesaccount.models.AgencyDetails
import uk.gov.hmrc.agentservicesaccount.models.AgentDetailsDesResponse
import uk.gov.hmrc.agentservicesaccount.models.BusinessAddress
import uk.gov.hmrc.agentservicesaccount.models.userDetails.UserDetails
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.retrieve.Email
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.EnrolmentIdentifier
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.auth.core.User

import scala.concurrent.Future

trait TestConstants {

  val arn: Arn = Arn("TARN0000001")
  val credentialRole: User.type = User
  val agentEnrolment: Set[Enrolment] = Set(
    Enrolment(
      "HMRC-AS-AGENT",
      Seq(EnrolmentIdentifier("AgentReferenceNumber", arn.value)),
      state = "Active",
      delegatedAuthRule = None
    )
  )
  val ggCredentials: Credentials = Credentials("ggId", "GovernmentGateway")
  val userDetails: UserDetails = UserDetails("Troy", Some("Barnes"))

  val authResponse: Future[Enrolments ~ Some[Credentials] ~ Some[Email] ~ Some[User.type]] = Future.successful(
    new ~(
      new ~(
        new ~(
          Enrolments(agentEnrolment),
          Some(ggCredentials)
        ),
        Some(Email("test@email.com"))
      ),
      Some(credentialRole)
    )
  )

  val suspensionDetailsResponse: Future[SuspensionDetails] = Future.successful(SuspensionDetails(suspensionStatus = false, None))

  val agencyDetails: AgencyDetails = AgencyDetails(
    agencyName = Some("My Agency"),
    agencyEmail = Some("abc@abc.com"),
    agencyTelephone = Some("07345678901"),
    agencyAddress = Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"
    ))
  )

  val emptyAgencyDetails: AgencyDetails = AgencyDetails(
    None,
    None,
    None,
    None
  )

  val emptyOtherServices: OtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = false,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  val desiDetailsWithEmptyOtherServices: DesignatoryDetails = DesignatoryDetails(agencyDetails, emptyOtherServices)

  val agentDetails: AgencyDetails = AgencyDetails(
    Some("My Agency"),
    Some("abc@abc.com"),
    Some("07345678901"),
    Some(BusinessAddress(
      "25 Any Street",
      Some("Central Grange"),
      Some("Telford"),
      None,
      Some("TF4 3TR"),
      "GB"
    ))
  )

  val suspensionDetails: SuspensionDetails = SuspensionDetails(suspensionStatus = false, None)

  val agentRecord: AgentDetailsDesResponse = AgentDetailsDesResponse(
    uniqueTaxReference = Some(Utr("0123456789")),
    agencyDetails = Some(agentDetails),
    suspensionDetails = Some(suspensionDetails)
  )

  val ctChangesOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = false,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = true,
      ctAgentReference = None
    )
  )

  val saChangesOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = true,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  val desiDetailsCtChangesOtherServices = DesignatoryDetails(agencyDetails, ctChangesOtherServices)
  val desiDetailsSaChangesOtherServices = DesignatoryDetails(agencyDetails, saChangesOtherServices)

}
