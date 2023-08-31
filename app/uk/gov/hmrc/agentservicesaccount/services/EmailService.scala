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

import play.api.i18n.{Lang, Langs}
import play.api.{LoggerLike, Logging}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.EmailConnector
import uk.gov.hmrc.agentservicesaccount.models.{AccountRecoverySummary, BetaInviteDetailsForEmail, SendEmailData}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailConnector: EmailConnector)(implicit langs: Langs)
    extends Logging {

  implicit val lang: Lang = langs.availables.head

  protected def getLogger: LoggerLike = logger

  def sendInviteAcceptedEmail(arn: Arn, details: BetaInviteDetailsForEmail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sendEmail(Seq("mtdgpvolunteers@hmrc.gov.uk"), arn, details, "agent_permissions_beta_participant_details")

  def sendEmail(sendTo: Seq[String],
                arn: Arn,
                details: BetaInviteDetailsForEmail,
                templateId: String
               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val emailInfo: SendEmailData = emailInformation(templateId, sendTo, arn.value, details)
    emailConnector.sendEmail(emailInfo)
  }

  private def emailInformation(templateId: String, sendTo: Seq[String], arn: String, details: BetaInviteDetailsForEmail) =
    SendEmailData(
      sendTo,
      templateId,
      Map(
        "arn" -> arn,
        "numClients" -> details.numberOfClients.toDescription,
        "contactName" -> details.name,
        "emailAddress" -> details.email,
        "telephoneNumber" -> details.phone.getOrElse("Not provided")
      )
    )

  def sendSuspendedSummaryEmail(arn: Arn, details: AccountRecoverySummary)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sendSuspendedEmail(Seq("mtdgpvolunteers@hmrc.gov.uk"), arn, details, "agent_permissions_beta_participant_details")

  def sendSuspendedEmail(sendTo: Seq[String],
                         arn: Arn,
                         details: AccountRecoverySummary,
                         templateId: String
                        )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val emailInfo: SendEmailData = SuspendedEmailInformation(templateId, sendTo, arn.value, details)
    emailConnector.sendEmail(emailInfo)
  }

  private def SuspendedEmailInformation(templateId: String, sendTo: Seq[String], arn: String, details: AccountRecoverySummary) =
    SendEmailData(
      sendTo,
      templateId,
      Map(
        "arn" -> arn,
        "contactName" -> details.name,
        "emailAddress" -> details.email,
        "telephoneNumber" -> details.phone
      )
    )

}
