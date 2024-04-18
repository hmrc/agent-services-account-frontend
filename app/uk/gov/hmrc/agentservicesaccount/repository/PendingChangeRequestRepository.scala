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
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeRequest
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PendingChangeRequestRepositoryImpl])
trait PendingChangeRequestRepository {
  def find(arn: Arn): Future[Option[PendingChangeRequest]]
  def insert(pcod: PendingChangeRequest): Future[Unit]
}

@Singleton
class PendingChangeRequestRepositoryImpl @Inject()(
    val mongoComponent: MongoComponent, configuration: Configuration)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[PendingChangeRequest](
        collectionName = "pending-change-request",
        domainFormat = PendingChangeRequest.format,
        mongoComponent = mongoComponent,
        indexes = Seq(
          IndexModel(Indexes.ascending("arn"), new IndexOptions().unique(true)),
          IndexModel(
            Indexes.ascending("timeSubmitted"),
            new IndexOptions()
              .expireAfter(configuration.get[Long]("mongodb.desi-details.lockout-period"), TimeUnit.MINUTES)
        )),
    replaceIndexes = true
  ) with PendingChangeRequestRepository with Logging {

  def find(arn: Arn): Future[Option[PendingChangeRequest]] = collection
    .find(equal("arn", arn.value))
    .headOption()

  def insert(pcod: PendingChangeRequest): Future[Unit] = collection
    .replaceOne(equal("arn", pcod.arn.value), pcod, new ReplaceOptions().upsert(true))
    .toFuture()
    .map(_ => ())
}
