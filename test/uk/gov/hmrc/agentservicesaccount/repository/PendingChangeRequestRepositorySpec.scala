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

package uk.gov.hmrc.agentservicesaccount.repository

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.PatienceConfiguration.{Interval, Timeout}
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import play.api.Configuration
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.support.UnitSpec
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class PendingChangeRequestRepositorySpec extends UnitSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with Eventually
  with CleanMongoCollectionSupport with MockFactory {

  private val testArn = Arn("XXARN0123456789")
  private val anotherArn = Arn("XZARN1111111111")

  val configuration: Configuration = Configuration.from(Map("mongodb.desi-details.lockout-period" -> 5))

  private def aPendingChangeOfDetails(timeSubmitted: Instant = Instant.now()) = PendingChangeRequest(
    arn = testArn,
    timeSubmitted = timeSubmitted.truncatedTo(ChronoUnit.SECONDS) // truncating allows us to compare timestamps more easily as mongo round-trip loses time precision
  )

  "PendingChangeRequestRepositoryImpl" should {
    "store a pending change of details" in {
      val pcodRepository: PendingChangeRequestRepositoryImpl = new PendingChangeRequestRepositoryImpl(mongoComponent, configuration)
      val pcod = aPendingChangeOfDetails()
      pcodRepository.insert(pcod).futureValue
      // verify document is stored
      pcodRepository.collection.find().toFuture().futureValue shouldBe Seq(pcod)
    }
    "retrieve a pending change of details with the correct arn" in {
      val pcodRepository: PendingChangeRequestRepositoryImpl = new PendingChangeRequestRepositoryImpl(mongoComponent,configuration)
      val pcod = aPendingChangeOfDetails()
      val anotherPcod = aPendingChangeOfDetails().copy(arn = anotherArn)
      pcodRepository.collection.insertMany(Seq(pcod, anotherPcod)).toFuture().futureValue
      // verify correct document is found
      pcodRepository.find(testArn).futureValue shouldBe Some(pcod)
    }
    "expire documents with timestamps older than the expiry period (28 days)" in {
      // Note: Slow test but may be worth having due to how easy it is to end up with a silently-failing TTL index on mongo
      val pcodRepository: PendingChangeRequestRepositoryImpl = new PendingChangeRequestRepositoryImpl(mongoComponent, configuration)
      val oldPcod = aPendingChangeOfDetails().copy(timeSubmitted = Instant.now().minus(30, ChronoUnit.DAYS))
      pcodRepository.collection.insertOne(oldPcod).toFuture().futureValue

      // verify that soon the document will no longer exist (because it is expired)
      eventually(Timeout(Span(65, Seconds)), Interval(Span(2, Seconds))) {
        pcodRepository.find(testArn).futureValue shouldBe None
      }
    }
  }

}

