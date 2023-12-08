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

import com.google.inject.ImplementedBy
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.Logging
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.agentservicesaccount.models.PendingChangeOfDetails
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.DAYS
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PendingChangeOfDetailsRepositoryImpl])
trait PendingChangeOfDetailsRepository {
  def find(arn: Arn): Future[Option[PendingChangeOfDetails]]
  def insert(pcod: PendingChangeOfDetails): Future[Unit]
}

@Singleton
class PendingChangeOfDetailsRepositoryImpl @Inject()(
    val mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[PendingChangeOfDetails](
        collectionName = "pending-change-of-details",
        domainFormat = PendingChangeOfDetails.format,
        mongoComponent = mongoComponent,
        indexes = Seq(
          IndexModel(Indexes.ascending("arn"), new IndexOptions().unique(true)),
          IndexModel(Indexes.ascending("timeSubmitted"), new IndexOptions().expireAfter(28, DAYS))
        )
  ) with PendingChangeOfDetailsRepository with Logging {

  def find(arn: Arn): Future[Option[PendingChangeOfDetails]] = collection
    .find(equal("arn", arn.value))
    .headOption()

  def insert(pcod: PendingChangeOfDetails): Future[Unit] = collection
    .replaceOne(equal("arn", pcod.arn.value), pcod, new ReplaceOptions().upsert(true))
    .toFuture()
    .map(_ => ())
}
