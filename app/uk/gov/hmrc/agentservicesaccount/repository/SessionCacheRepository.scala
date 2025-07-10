/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import uk.gov.hmrc.crypto.json.JsonEncryption.sensitiveDecrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.sensitiveEncrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.Sensitive
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.cache.{SessionCacheRepository => CacheRepository}
import SensitiveWrapper._
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SessionCacheRepository @Inject() (
  val mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport
)(implicit
  ec: ExecutionContext,
  @Named("aes") val crypto: Encrypter
    with Decrypter
)
extends CacheRepository(
  mongoComponent = mongoComponent,
  collectionName = "sessions",
  replaceIndexes = true,
  ttl = 4.hours, // sessions can last for a maximum of 4 hours
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
) {

  override def putSession[T: Writes](
    dataKey: DataKey[T],
    data: T
  )(implicit request: RequestHeader): Future[(String, String)] = Mdc.preservingMdc {
    super.putSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap), SensitiveWrapper(data))
  }

  override def getFromSession[T: Reads](
    dataKey: DataKey[T]
  )(implicit request: RequestHeader): Future[Option[T]] = Mdc.preservingMdc {
    super.getFromSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap)).map(_.map(_.decryptedValue))
  }

  override def deleteFromSession[T](
    dataKey: DataKey[T]
  )(implicit request: RequestHeader): Future[Unit] = Mdc.preservingMdc {
    super.deleteFromSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap))
  }

}

case class SensitiveWrapper[T](override val decryptedValue: T)
extends Sensitive[T]

object SensitiveWrapper {

  implicit def reads[T](implicit
    reads: Reads[T],
    crypto: Encrypter
      with Decrypter
  ): Reads[SensitiveWrapper[T]] = sensitiveDecrypter(SensitiveWrapper[T])

  implicit def writes[T](implicit
    writes: Writes[T],
    crypto: Encrypter
      with Decrypter
  ): Writes[SensitiveWrapper[T]] = sensitiveEncrypter

}
