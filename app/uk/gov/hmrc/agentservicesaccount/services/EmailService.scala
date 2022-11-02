/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.{LoggerLike, Logging}
import play.api.i18n.{Lang, Langs}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.connectors.EmailConnector
import uk.gov.hmrc.agentservicesaccount.models.{BetaInviteDetailsForEmail, SendEmailData}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailConnector: EmailConnector)(implicit langs: Langs)
    extends Logging {

  implicit val lang: Lang = langs.availables.head

  protected def getLogger: LoggerLike = logger

  def sendInviteAcceptedEmail(arn: Arn, details: BetaInviteDetailsForEmail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    sendEmail("mtdgpvolunteers@hmrc.gov.uk", arn, details, "agent_interested_in_private_beta")

  def sendEmail(sendTo: String,
                arn: Arn,
                details: BetaInviteDetailsForEmail,
                templateId: String
               )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    arn match {
      case arn =>
        val emailInfo: SendEmailData = emailInformation(templateId, sendTo, arn.value, details)
        emailConnector.sendEmail(emailInfo)
      case _ =>
        logger.warn(s"email not sent as there were no details for email found in invitation")
        Future.successful((): Unit)
    }

  private def emailInformation(templateId: String, email: String, arn: String, details: BetaInviteDetailsForEmail) =
    SendEmailData(
      Seq(email),
      templateId,
      Map(
        "arn" -> arn,
        "numberOfClients" -> details.numberOfClients.toDescription,
        "name" -> details.name,
        "email" -> details.email,
        "phone" -> details.phone.getOrElse("Not provided")
      )
    )

}
