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
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.LegacyRegime
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionInfo

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SubscriptionService @Inject() (
  agentServicesAccountConnector: AgentServicesAccountConnector
)(implicit ec: ExecutionContext)
extends Logging {

  def getSubscriptionInfo(
    missingSubscriptions: Seq[LegacyRegime],
    existingSubscriptionInfo: Seq[SubscriptionInfo]
  )(implicit rh: RequestHeader): Future[Seq[SubscriptionInfo]] = {
    for {
      missingSubscriptionInfo <-
        if (missingSubscriptions.nonEmpty)
          agentServicesAccountConnector.getSubscriptionInfo(missingSubscriptions)
        else
          Future.successful(Nil)
    } yield existingSubscriptionInfo ++ missingSubscriptionInfo
  }
}
