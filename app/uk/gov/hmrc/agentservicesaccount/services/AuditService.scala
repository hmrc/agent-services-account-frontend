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

package uk.gov.hmrc.agentservicesaccount.services

import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.models._
import uk.gov.hmrc.agentservicesaccount.models.audit._
import uk.gov.hmrc.agentservicesaccount.models.AmlsDetails
import uk.gov.hmrc.agentservicesaccount.models.AmlsRequest
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeOfDetails
import uk.gov.hmrc.agentservicesaccount.utils.RequestSupport._
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject() (
  val auditConnector: AuditConnector,
  config: Configuration
)(implicit ec: ExecutionContext) {

  private def audit[A <: AuditDetail: Writes](a: A)(implicit rh: RequestHeader): Unit = {
    val _ = auditConnector.sendExtendedEvent(
      ExtendedDataEvent(
        auditSource = auditSource,
        auditType = a.auditType,
        eventId = UUID.randomUUID().toString,
        detail = Json.toJson(a),
        tags = hc.toAuditTags()
      )
    )
  }

  def auditUpdateContactDetailsRequest(
    optUtr: Option[Utr],
    pendingChangeOfDetails: PendingChangeOfDetails
  )(implicit rh: RequestHeader): Unit = audit(toUpdateContactDetailsRequestAudit(optUtr, pendingChangeOfDetails))

  def auditUpdateAmlSupervisionDetails(
    amlsRequest: AmlsRequest,
    almsDetailsOpt: Option[AmlsDetails],
    arn: Arn,
    optUtr: Option[Utr]
  )(implicit rh: RequestHeader): Unit = audit(toUpdateAmlSupervisionDetailsAudit(
    arn: Arn,
    optUtr: Option[Utr],
    amlsRequest,
    almsDetailsOpt
  ))

  private def toUpdateContactDetailsRequestAudit(
    optUtr: Option[Utr],
    pendingChangeOfDetails: PendingChangeOfDetails
  ): UpdateContactDetailsAuditRequest = {

    val firstName = pendingChangeOfDetails.submittedBy.fullName.split(" ").headOption.getOrElse("")
    val lastName = pendingChangeOfDetails.submittedBy.fullName.split(" ").lastOption.getOrElse("")

    UpdateContactDetailsAuditRequest(
      agentReferenceNumber = pendingChangeOfDetails.arn,
      utr = optUtr,
      existingContactDetails = pendingChangeOfDetails.oldDetails,
      newContactDetails = pendingChangeOfDetails.newDetails,
      changedInSelfAssessment = pendingChangeOfDetails.otherServices.saChanges.applyChanges,
      selfAssessmentAgentCode = pendingChangeOfDetails.otherServices.saChanges.saAgentReference,
      changedInCorporationTax = pendingChangeOfDetails.otherServices.ctChanges.applyChanges,
      corporationTaxAgentCode = pendingChangeOfDetails.otherServices.ctChanges.ctAgentReference,
      userDetails = UserDetails(
        firstName,
        lastName = lastName,
        telephone = pendingChangeOfDetails.submittedBy.telephone
      ),
      queueDetails = config.getOptional[String](
        "microservice.services.dms-submission.contact-details-submission.classificationType"
      ).getOrElse(throw new RuntimeException(s"Config not found: microservice.services.dms-submission.contact-details-submission.classificationType")) // appConfig.dmsSubmissionClassificationType
    )
  }

  private def toUpdateAmlSupervisionDetailsAudit(
    arn: Arn,
    optUtr: Option[Utr],
    amlsRequest: AmlsRequest,
    almsDetailsOpt: Option[AmlsDetails]
  ): UpdateAmlsAuditDetails = {
    UpdateAmlsAuditDetails(
      agentReferenceNumber = arn,
      utr = optUtr,
      existingAmlsDetails = almsDetailsOpt.map(x =>
        AmlsAuditDetails(
          x.membershipExpiresOn,
          x.membershipNumber,
          x.supervisoryBody
        )
      ),
      newAmlsDetails = AmlsAuditDetails(
        amlsRequest.membershipExpiresOn,
        Some(amlsRequest.membershipNumber),
        amlsRequest.supervisoryBody
      )
    )
  }

  private val auditSource: String = "agent-services-account-frontend"

}
