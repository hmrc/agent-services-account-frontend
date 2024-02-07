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

package uk.gov.hmrc.agentservicesaccount.stubs

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.{Reads, Writes}
import play.api.mvc.Request
import uk.gov.hmrc.agentservicesaccount.services.SessionCacheService
import uk.gov.hmrc.mongo.cache.DataKey

import scala.concurrent.{ExecutionContext, Future}

trait SessionServiceMocks extends MockFactory {

  def expectGetSessionItem[T](key: DataKey[T], mockedResponse: T, times: Int = 1)
                             (implicit service: SessionCacheService): Unit =
    (service.get(_: DataKey[T])(_: Reads[T], _: Request[_]))
      .expects(key, *, *)
      .returning(Future.successful(Some(mockedResponse))).repeat(times)

  def expectGetSessionItemNone[T](key: DataKey[T])
                                 (implicit service: SessionCacheService): Unit =
    (service.get(_: DataKey[T])(_: Reads[T], _: Request[_]))
      .expects(key, *, *)
      .returning(Future.successful(None))

  def expectPutSessionItem[T](key: DataKey[T], value: T)
                             (implicit service: SessionCacheService): Unit =
    (service.put(_: DataKey[T], _: T)(_: Writes[T], _: Request[_]))
      .expects(key, value, *, *)
      .returning(Future.successful(("", "")))

  def expectDeleteSessionItem[T](key: DataKey[T])
                                (implicit service: SessionCacheService): Unit =
    (service.delete(_: DataKey[T])(_: Request[_]))
      .expects(key, *)
      .returning(Future.successful(None))

  def getAllSessionItems[T](mockedResponse: List[Option[String]], times: Int = 1)
                             (implicit service: SessionCacheService): Unit =
    (service.getSessionItems()(_: Request[_], _: ExecutionContext))
      .expects(*, *)
      .returning(Future.successful(mockedResponse)).repeat(times)

}

