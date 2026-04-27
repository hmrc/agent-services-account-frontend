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

package uk.gov.hmrc.agentservicesaccount.repository

import org.apache.pekko.Done
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.ReplaceOptions
import uk.gov.hmrc.agentservicesaccount.config.AppConfig
import uk.gov.hmrc.agentservicesaccount.models.upscan.FileUploadReference
import uk.gov.hmrc.agentservicesaccount.models.upscan.UpscanDetails
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class UpscanRepository @Inject() (
  val mongoComponent: MongoComponent,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
extends PlayMongoRepository[UpscanDetails](
  collectionName = "upscan-details",
  domainFormat = UpscanDetails.format,
  mongoComponent = mongoComponent,
  indexes = Seq(
    IndexModel(Indexes.ascending("reference"), IndexOptions().unique(true)),
    IndexModel(
      Indexes.ascending("timestamp"),
      IndexOptions().expireAfter(appConfig.upscanExpiryDuration, TimeUnit.MINUTES)
    )
  ),
  replaceIndexes = true
) {

  def findByReference(reference: FileUploadReference): Future[Option[UpscanDetails]] = collection
    .find(equal("reference", reference.value))
    .toFuture()
    .map(_.headOption)

  def saveUpscanDetails(details: UpscanDetails): Future[Done] = collection
    .replaceOne(
      filter = equal("reference", details.reference.value),
      replacement = details,
      options = ReplaceOptions().upsert(true)
    ).toFuture().map(_ => Done)

}
