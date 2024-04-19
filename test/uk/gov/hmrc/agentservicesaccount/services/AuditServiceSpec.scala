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

package uk.gov.hmrc.agentservicesaccount.services

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, OtherServices, SaChanges, YourDetails}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.stubs.MockAuditConnector
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext

class AuditServiceSpec extends PlaySpec with MockAuditConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  val service  = new AuditService(mockAuditConnector)

  val testArn = Arn("XXARN0123456789")
  val testUtr = Utr("XXUTR12345667")

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
      "GB"))
  )

  val testAgencyDetailsFull = AgencyDetails(
    agencyName = Some("Test Name"),
    agencyEmail = Some("test@email.com"),
    agencyTelephone = Some("01234 567890"),
    agencyAddress = Some(BusinessAddress("Test Street", Some("Test Town"), None, None, Some("TE5 7ED"), "GB"))
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

  val submittedByDetails = YourDetails(
    fullName = "John Tester",
    telephone = "01903 209919"
  )

  val pendingChangeOfDetails1 = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyName = testAgencyDetailsFull.agencyName),
    otherServices = emptyOtherServices,
    timeSubmitted = Instant.now,
    submittedBy = submittedByDetails
  )


  "UpdateContactDetailsRequest" should {
    "end a successful update contact details event to the audit connector and get an audit result back" in {
      mockSendExtendedEvent()

      val result =service.auditUpdateContactDetailsRequest(Some(testUtr), pendingChangeOfDetails1)
      result mustBe ()
    }
  }
}

