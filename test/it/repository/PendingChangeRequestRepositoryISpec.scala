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

package it.repository

import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Interval
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import play.api.Application
import play.api.http.Status.FORBIDDEN
import play.api.http.Status.NOT_FOUND
import play.api.http.Status.NO_CONTENT
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import support.BaseISpec
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.agentservicesaccount.repository.PendingChangeRequestRepositoryImpl
import stubs.AgentServicesAccountStubs.stubASADeleteResponse
import stubs.AgentServicesAccountStubs.stubASAGetResponse
import stubs.AgentServicesAccountStubs.stubASAGetResponseError
import stubs.AgentServicesAccountStubs.stubASAPostResponse
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.test.MongoSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class PendingChangeRequestRepositorySpec
extends BaseISpec
with MongoSupport {

  override def beforeEach(): Unit = {
    super.beforeEach()
    prepareDatabase()
  }

  private val testArn = Arn("XXARN0123456789")

  val pendingChangeRequest: PendingChangeRequest = PendingChangeRequest(
    arn = testArn,
    timeSubmitted = Instant.now().truncatedTo(ChronoUnit.SECONDS) // truncating allows us to compare timestamps more easily as mongo round-trip loses time precision
  )

  def appWithFeature(featureFlag: Boolean): Application = appBuilder(Map("features.enable-backend-pcr-database" -> featureFlag)).build()

  def createPCRRepository(appWithFeature: Application): PendingChangeRequestRepositoryImpl = {
    val appConfig: AppConfig = appWithFeature.injector.instanceOf[AppConfig]
    val asaConnector: AgentServicesAccountConnector = appWithFeature.injector.instanceOf[AgentServicesAccountConnector]
    new PendingChangeRequestRepositoryImpl(
      mongoComponent,
      appConfig,
      asaConnector
    )
  }

  ".find" when {

    "the enableBackendPCRDatabase feature switch is enabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(true))

      "retrieve a pending change request from ASA" in {
        stubASAGetResponse(pendingChangeRequest)
        await(pcrRepository.find(testArn)) shouldBe Some(pendingChangeRequest)
      }

      "retrieve a pending change from the database when ASA does not return one" in {
        stubASAGetResponseError(testArn, NOT_FOUND)
        await(pcrRepository.collection.insertOne(pendingChangeRequest).toFuture())
        await(pcrRepository.find(testArn)) shouldBe Some(pendingChangeRequest)
      }

      "return None if a pending change is not returned by either ASA or the database" in {
        stubASAGetResponseError(testArn, NOT_FOUND)
        await(pcrRepository.find(testArn)) shouldBe None
      }
    }

    "the enableBackendPCRDatabase feature switch is disabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(false))

      "retrieve a pending change from the database" in {
        await(pcrRepository.collection.insertOne(pendingChangeRequest).toFuture())
        await(pcrRepository.find(testArn)) shouldBe Some(pendingChangeRequest)
      }

      "return None if a pending change is not found in the database" in {
        await(pcrRepository.find(testArn)) shouldBe None
      }
    }
  }

  ".insert" when {

    "the enableBackendPCRDatabase feature switch is enabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(true))

      "send a pending change request to ASA" in {
        stubASAPostResponse(NO_CONTENT)
        await(pcrRepository.insert(pendingChangeRequest)) shouldBe ()
      }

      "return the exception thrown by the connector if the insert fails" in {
        stubASAPostResponse(FORBIDDEN)
        intercept[UpstreamErrorResponse](await(pcrRepository.insert(pendingChangeRequest)))
      }
    }

    "the enableBackendPCRDatabase feature switch is disabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(false))

      "insert a pending change request to the database" in {
        await(pcrRepository.insert(pendingChangeRequest)) shouldBe ()
        await(pcrRepository.collection.countDocuments().toFuture()) shouldBe 1
      }
    }
  }

  ".delete" when {

    "the enableBackendPCRDatabase feature switch is enabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(true))

      "delete a pending change request from ASA" in {
        stubASADeleteResponse(testArn, NO_CONTENT)
        await(pcrRepository.delete(testArn)) shouldBe ()
      }

      "delete a pending change from the database when ASA was not able to" in {
        stubASADeleteResponse(testArn, NOT_FOUND)
        await(pcrRepository.collection.insertOne(pendingChangeRequest).toFuture())
        await(pcrRepository.collection.countDocuments().toFuture()) shouldBe 1
        await(pcrRepository.delete(testArn)) shouldBe ()
        await(pcrRepository.collection.countDocuments().toFuture()) shouldBe 0
      }
    }

    "the enableBackendPCRDatabase feature switch is disabled" should {

      val pcrRepository = createPCRRepository(appWithFeature(false))

      "delete a pending change from the database" in {
        await(pcrRepository.collection.insertOne(pendingChangeRequest).toFuture())
        await(pcrRepository.collection.countDocuments().toFuture()) shouldBe 1
        await(pcrRepository.delete(testArn)) shouldBe ()
        await(pcrRepository.collection.countDocuments().toFuture()) shouldBe 0
      }
    }
  }

  "The DB should expire documents with timestamps older than the expiry period (28 days)" in {
    // Note: Slow test but may be worth having due to how easy it is to end up with a silently-failing TTL index on mongo
    val pcrRepository = createPCRRepository(appWithFeature(true))
    val oldPCR = pendingChangeRequest.copy(timeSubmitted = Instant.now().minus(30, ChronoUnit.DAYS))
    stubASAGetResponseError(testArn, NOT_FOUND)
    await(pcrRepository.collection.insertOne(oldPCR).toFuture())

    // verify that soon the document will no longer exist (because it is expired)
    eventually(Timeout(Span(65, Seconds)), Interval(Span(2, Seconds))) {
      await(pcrRepository.find(testArn)) shouldBe None
    }
  }

}
