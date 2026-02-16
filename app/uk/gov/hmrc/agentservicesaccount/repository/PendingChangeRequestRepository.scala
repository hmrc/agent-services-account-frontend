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

import com.google.inject.ImplementedBy
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.ReplaceOptions
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.models.Arn
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.connectors.AgentServicesAccountConnector
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@ImplementedBy(classOf[PendingChangeRequestRepositoryImpl])
trait PendingChangeRequestRepository {

  def find(arn: Arn)(implicit rh: RequestHeader): Future[Option[PendingChangeRequest]]
  def insert(pcod: PendingChangeRequest)(implicit rh: RequestHeader): Future[Unit]
  def delete(arn: Arn)(implicit rh: RequestHeader): Future[Unit]

}

@Singleton
class PendingChangeRequestRepositoryImpl @Inject() (
  val mongoComponent: MongoComponent,
  appConfig: AppConfig,
  asaConnector: AgentServicesAccountConnector
)(implicit ec: ExecutionContext)
extends PlayMongoRepository[PendingChangeRequest](
  collectionName = "pending-change-request",
  domainFormat = PendingChangeRequest.format,
  mongoComponent = mongoComponent,
  indexes = Seq(
    IndexModel(Indexes.ascending("arn"), new IndexOptions().unique(true)),
    IndexModel(
      Indexes.ascending("timeSubmitted"),
      new IndexOptions().expireAfter(appConfig.pendingChangeTTL, TimeUnit.MINUTES)
    )
  ),
  replaceIndexes = true
)
with PendingChangeRequestRepository
with Logging {

  def find(arn: Arn)(implicit rh: RequestHeader): Future[Option[PendingChangeRequest]] = {

    lazy val frontendDatabaseResult = collection
      .find(equal("arn", arn.value))
      .headOption()

    if (appConfig.enableBackendPCRDatabase) {
      asaConnector.findChangeRequest(arn).flatMap {
        case Some(changeRequest) => Future.successful(Some(changeRequest))
        case _ => frontendDatabaseResult
      }
    }
    else {
      frontendDatabaseResult
    }
  }

  def insert(pcod: PendingChangeRequest)(implicit rh: RequestHeader): Future[Unit] =
    if (appConfig.enableBackendPCRDatabase) {
      asaConnector.insertChangeRequest(pcod)
    }
    else {
      collection
        .replaceOne(
          equal("arn", pcod.arn.value),
          pcod,
          new ReplaceOptions().upsert(true)
        )
        .toFuture()
        .map(_ => ())
    }

  def delete(arn: Arn)(implicit rh: RequestHeader): Future[Unit] = {

    lazy val frontendDatabaseResult = collection
      .deleteOne(equal("arn", arn.value))
      .toFuture()
      .map(_ => ())

    if (appConfig.enableBackendPCRDatabase) {
      asaConnector.deleteChangeRequest(arn).flatMap {
        case true => Future.successful(())
        case false => frontendDatabaseResult
      }
    }
    else {
      frontendDatabaseResult
    }
  }

}
