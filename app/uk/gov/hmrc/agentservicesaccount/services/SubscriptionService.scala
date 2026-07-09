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

    val originalMissingSubscriptions = agentInfo.missingSubscriptions

    agentServicesAccountConnector
      .getSubscriptionInfo(originalMissingSubscriptions.map(_.regime))
      .flatMap { connectorSubscriptions =>
        val mergedSubscriptions =
          connectorSubscriptions.map { connectorSubscription =>
            originalMissingSubscriptions
              .find(_.regime == connectorSubscription.regime)
              .filter(_.subscriptionStatus == SubscriptionStatus.InactiveEnrolment)
              .map { _ =>
                connectorSubscription.copy(
                  subscriptionStatus = SubscriptionStatus.InactiveEnrolment
                )
              }
              .getOrElse(connectorSubscription)
          }
        Future.traverse(mergedSubscriptions) { subInfo =>
          if (subInfo.subscriptionStatus != SubscriptionStatus.InactiveEnrolment) {
            Future.successful(subInfo)
          }
          else {
            enrolmentStoreProxyConnector
              .getGroupAllocatedEnrolment(
                agentInfo.groupId,
                s"HMRC-${subInfo.regime}-AGENT"
              )
              .map {
                case Some(es5Response) =>
                  subInfo.copy(
                    creationDate = es5Response.enrolmentDate
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
  }
}
