/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.actions.AgentInfo
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionInfo
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionStatus
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject() (
  agentServicesAccountConnector: AgentServicesAccountConnector,
  enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector
)(implicit ec: ExecutionContext)
extends Logging {

  def getSubscriptionInfo(
    agentInfo: AgentInfo
  )(using
    HeaderCarrier,
    RequestHeader
  ): Future[Seq[SubscriptionInfo]] = {
    val subscriptions = agentInfo.subscriptions
    val subscribed = subscriptions.filter(_.subscriptionStatus == SubscriptionStatus.Subscribed)
    val inactive = subscriptions.filter(_.subscriptionStatus == SubscriptionStatus.InactiveEnrolment)
    val notSubscribed = subscriptions.filter(_.subscriptionStatus == SubscriptionStatus.NotSubscribed)
    val inactiveWithAgentReference = inactive.flatMap { sub => agentInfo.getAgentReferenceFor(sub.regime).map(sub -> _) }
    for {
      enrichedInactive <-
        Future.traverse(inactiveWithAgentReference) { case (sub, agentReference) =>
          enrichInactiveSubscriptionWithEnrolmentDate(
            sub,
            agentInfo.groupId,
            agentReference
          )
        }
      backendSubscriptions <-
        if (notSubscribed.nonEmpty)
          agentServicesAccountConnector.getSubscriptionInfo(
            notSubscribed.map(_.regime)
          )
        else
          Future.successful(Seq.empty)

    } yield subscribed ++ enrichedInactive ++ backendSubscriptions
  }

  private val es5DateFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  private def convertEs5DateToInstant(date: String): Instant =
    LocalDateTime.parse(date, es5DateFormatter).atZone(ZoneId.of("Europe/London")).toInstant

  private def enrichInactiveSubscriptionWithEnrolmentDate(
    subInfo: SubscriptionInfo,
    groupId: String,
    agentReference: String
  )(using HeaderCarrier): Future[SubscriptionInfo] = {
    if (subInfo.subscriptionStatus != SubscriptionStatus.InactiveEnrolment) {
      Future.successful(subInfo)
    }
    else {
      enrolmentStoreProxyConnector
        .getGroupAllocatedEnrolment(
          groupId,
          subInfo.regime,
          agentReference
        )
        .map {
          case Some(es5Response) =>
            subInfo.copy(
              creationDate = es5Response.enrolmentDate.map(convertEs5DateToInstant)
            )
          case None => subInfo
        }
        .recover {
          case ex =>
            logger.warn(
              s"[SubscriptionService] ES5 enrichment failed for ${subInfo.regime}: ${ex.getMessage}"
            )
            subInfo
        }
    }
  }

}
