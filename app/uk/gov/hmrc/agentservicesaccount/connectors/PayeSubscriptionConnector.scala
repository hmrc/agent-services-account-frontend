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

package uk.gov.hmrc.agentservicesaccount.connectors

import com.google.inject.ImplementedBy
import uk.gov.hmrc.agentservicesaccount.models.paye._

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[PayeSubscriptionMockConnector])
trait PayeSubscriptionConnector {

  def getStatus()(implicit ec: ExecutionContext): Future[PayeStatus]
  def getCyaData()(implicit ec: ExecutionContext): Future[PayeCyaData]
  def submitRequest()(implicit ec: ExecutionContext): Future[Unit]

}

@Singleton
final class PayeSubscriptionMockConnector @Inject() ()
extends PayeSubscriptionConnector {

  override def getStatus()(implicit ec: ExecutionContext): Future[PayeStatus] = Future.successful(PayeStatus(
    hasSubscription = false,
    hasRequestInProgress = false
  ))

  override def getCyaData()(implicit ec: ExecutionContext): Future[PayeCyaData] = Future.successful(
    PayeCyaData(
      agentName = "Example Agent Ltd",
      contactName = "Jane Agent",
      telephoneNumber = Some("01632 960 001"),
      emailAddress = Some("jane.agent@example.com"),
      address = PayeAddress(
        line1 = "1 High Street",
        line2 = "Village",
        line3 = Some("County"),
        line4 = None,
        postCode = "AA1 1AA"
      )
    )
  )

  override def submitRequest()(implicit ec: ExecutionContext): Future[Unit] = Future.successful(())

}
