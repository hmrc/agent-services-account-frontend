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

import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import support.TestConstants
import uk.gov.hmrc.agentservicesaccount.actions.AgentInfo
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.agentservicesaccount.models.Es5GroupAllocatedEnrolment
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.*
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionStatus.InactiveEnrolment
import uk.gov.hmrc.agentservicesaccount.models.subscriptions.SubscriptionStatus.Subscribed
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class SubscriptionServiceSpec
extends PlaySpec
with IdiomaticMockito
with ArgumentMatchersSugar
with TestConstants
with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait Setup {

    val mockASAConnector = mock[AgentServicesAccountConnector]
    val mockES5Connector = mock[EnrolmentStoreProxyConnector]
    val service = new SubscriptionService(mockASAConnector, mockES5Connector)

  }

  private val regime = LegacyRegime.SA
  private val inactiveSubInfo = SubscriptionInfo(
    regime = regime,
    subscriptionStatus = SubscriptionStatus.InactiveEnrolment,
    creationDate = None
  )
  private val activeSubInfo = inactiveSubInfo.copy(
    subscriptionStatus = SubscriptionStatus.Subscribed
  )

  "getSubscriptionInfo" should {

    "not call ES5 when subscription is not inactive" in new Setup {

      val agentInfo = mock[AgentInfo]
      agentInfo.missingSubscriptions returns Seq(SubscriptionInfo(regime, Subscribed))
      agentInfo.existingSubscriptionInfo returns Seq()
      mockASAConnector.getSubscriptionInfo(*).returns(Future.successful(Seq(activeSubInfo)))

      val result = service.getSubscriptionInfo(agentInfo).futureValue

      mockES5Connector.getGroupAllocatedEnrolment(*, *) wasNever called
      result.head.subscriptionStatus mustBe SubscriptionStatus.Subscribed
    }

    "enrich inactive enrolment with ES5 date when available" in new Setup {

      val agentInfo = mock[AgentInfo]
      agentInfo.missingSubscriptions returns Seq(SubscriptionInfo(regime, InactiveEnrolment))
      agentInfo.existingSubscriptionInfo returns Seq()
      mockASAConnector.getSubscriptionInfo(*).returns(Future.successful(Seq(inactiveSubInfo)))
      val enrolmentDate = Instant.now
      mockES5Connector.getLegacyAgentEnrolment(*, *)
        .returns(Future.successful(Some(
          Es5GroupAllocatedEnrolment(
            service = "testService",
            status = Some("testStatus"),
            enrolmentDate = Some(enrolmentDate)
          )
        )))

      val result = service.getSubscriptionInfo(agentInfo).futureValue

      result.head.creationDate mustBe Some(enrolmentDate)
      result.head.subscriptionStatus mustBe SubscriptionStatus.InactiveEnrolment
    }

    "leave inactive enrolment unchanged when ES5 returns None" in new Setup {

      val agentInfo = mock[AgentInfo]
      agentInfo.missingSubscriptions returns Seq(SubscriptionInfo(regime, InactiveEnrolment))
      agentInfo.existingSubscriptionInfo returns Seq()
      mockASAConnector.getSubscriptionInfo(*).returns(Future.successful(Seq(inactiveSubInfo)))
      mockES5Connector.getLegacyAgentEnrolment(*, *).returns(Future.successful(None))

      val result = service.getSubscriptionInfo(agentInfo).futureValue

      result.head.creationDate mustBe None
    }

    "recover from ES5 failure without failing request" in new Setup {

      val agentInfo = mock[AgentInfo]
      agentInfo.missingSubscriptions returns Seq(SubscriptionInfo(regime, InactiveEnrolment))
      agentInfo.existingSubscriptionInfo returns Seq()
      mockASAConnector.getSubscriptionInfo(*).returns(Future.successful(Seq(inactiveSubInfo)))
      mockES5Connector.getLegacyAgentEnrolment(*, *).returns(Future.failed(new RuntimeException("ES5 down")))

      val result = service.getSubscriptionInfo(agentInfo).futureValue

      result.head.subscriptionStatus mustBe SubscriptionStatus.InactiveEnrolment
    }
  }

}
