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

package uk.gov.hmrc.agentservicesaccount.services

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentservicesaccount.controllers.sessionKeys
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class SessionCacheService @Inject() (sessionCacheRepository: SessionCacheRepository) {

  def get[T](dataKey: DataKey[T])(implicit
    reads: Reads[T],
    request: RequestHeader
  ): Future[Option[T]] = {
    sessionCacheRepository.getFromSession[T](dataKey)
  }

  def put[T](
    dataKey: DataKey[T],
    value: T
  )(implicit
    writes: Writes[T],
    request: RequestHeader
  ): Future[(String, String)] = {
    sessionCacheRepository.putSession(dataKey, value)
  }

  def delete[T](dataKey: DataKey[T])(implicit request: RequestHeader): Future[Unit] = {
    sessionCacheRepository.deleteFromSession(dataKey)
  }

  def getBetaInviteSessionItems()(implicit
    request: RequestHeader,
    ec: ExecutionContext
  ): Future[List[Option[String]]] = {
    Future.sequence(sessionKeys.map(get(_)))
  }

}
