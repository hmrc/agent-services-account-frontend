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

package uk.gov.hmrc.agentservicesaccount.repository

import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{CtChanges, OtherServices, SaChanges}
import uk.gov.hmrc.agentservicesaccount.models.{AgencyDetails, BusinessAddress, PendingChangeOfDetails}
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class PendingChangeOfDetailsRepositorySpec extends UnitSpec with Matchers  with ScalaFutures with IntegrationPatience with Eventually with CleanMongoCollectionSupport {

  private val testArn = Arn("XXARN0123456789")
  private val anotherArn = Arn("XZARN1111111111")

  private val agencyDetails = AgencyDetails(
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

  private val emptyOtherServices = OtherServices(
    saChanges = SaChanges(
      applyChanges = false,
      saAgentReference = None
    ),
    ctChanges = CtChanges(
      applyChanges = false,
      ctAgentReference = None
    )
  )

  private def aPendingChangeOfDetails(timeSubmitted: Instant = Instant.now()) = PendingChangeOfDetails(
    arn = testArn,
    oldDetails = agencyDetails,
    newDetails = agencyDetails.copy(agencyName = Some("New and Improved Agency")),
    otherServices = emptyOtherServices,
    timeSubmitted = timeSubmitted.truncatedTo(ChronoUnit.SECONDS) // truncating allows us to compare timestamps more easily as mongo round-trip loses time precision
  )

  "PendingChangeOfDetailsRepositoryImpl" should {
    "store a pending change of details" in {
      val pcodRepository: PendingChangeOfDetailsRepositoryImpl = new PendingChangeOfDetailsRepositoryImpl(mongoComponent)
      val pcod = aPendingChangeOfDetails()
      pcodRepository.insert(pcod).futureValue
      // verify document is stored
      pcodRepository.collection.find().toFuture().futureValue shouldBe Seq(pcod)
    }
    "retrieve a pending change of details with the correct arn" in {
      val pcodRepository: PendingChangeOfDetailsRepositoryImpl = new PendingChangeOfDetailsRepositoryImpl(mongoComponent)
      val pcod = aPendingChangeOfDetails()
      val anotherPcod = aPendingChangeOfDetails().copy(arn = anotherArn)
      pcodRepository.collection.insertMany(Seq(pcod, anotherPcod)).toFuture().futureValue
      // verify correct document is found
      pcodRepository.find(testArn).futureValue shouldBe Some(pcod)
    }
    "expire documents with timestamps older than the expiry period (28 days)" in {
      // Note: Slow test but may be worth having due to how easy it is to end up with a silently-failing TTL index on mongo
      val pcodRepository: PendingChangeOfDetailsRepositoryImpl = new PendingChangeOfDetailsRepositoryImpl(mongoComponent)
      val oldPcod = aPendingChangeOfDetails().copy(timeSubmitted = Instant.now().minus(30, ChronoUnit.DAYS))
      pcodRepository.collection.insertOne(oldPcod).toFuture().futureValue

      // verify that soon the document will no longer exist (because it is expired)
      eventually(Timeout(Span(65, Seconds)), Interval(Span(2, Seconds))) {
        pcodRepository.find(testArn).futureValue shouldBe None
      }
    }
  }

}

