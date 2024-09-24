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

import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentservicesaccount.controllers.{ARN, DESCRIPTION, DRAFT_NEW_CONTACT_DETAILS, DRAFT_SUBMITTED_BY, EMAIL, EMAIL_PENDING_VERIFICATION, NAME, PHONE, sessionKeys}
import uk.gov.hmrc.agentservicesaccount.models.desiDetails.{DesignatoryDetails, YourDetails}
import uk.gov.hmrc.agentservicesaccount.repository.SessionCacheRepository
import uk.gov.hmrc.agentservicesaccount.utils.EncryptedStringUtil
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionCacheService @Inject()(sessionCacheRepository: SessionCacheRepository)
                                   (implicit @Named("aes") crypto: Encrypter with Decrypter) {


  def withSessionItem[T](dataKey: DataKey[T])
                        (body: Option[T] => Future[Result])
                        (implicit reads: Reads[T], request: Request[_], ec: ExecutionContext): Future[Result] = {
    sessionCacheRepository.getFromSession[T](dataKey).flatMap(data => body(data))
  }

  def get[T](dataKey: DataKey[T])
            (implicit reads: Reads[T], request: Request[_]): Future[Option[T]] = {
    dataKey match {
      case key: DataKey[String @unchecked] if Seq(NAME, EMAIL, PHONE, ARN, DESCRIPTION, EMAIL_PENDING_VERIFICATION).contains(key) =>
        sessionCacheRepository.getFromSession(key)(EncryptedStringUtil.fallbackStringFormat, request)
      case key: DataKey[DesignatoryDetails @unchecked] if key == DRAFT_NEW_CONTACT_DETAILS =>
        sessionCacheRepository.getFromSession(key)(DesignatoryDetails.databaseFormat, request)
      case key: DataKey[YourDetails @unchecked] if key == DRAFT_SUBMITTED_BY =>
        sessionCacheRepository.getFromSession(key)(YourDetails.databaseFormat, request)
      case _ =>
        sessionCacheRepository.getFromSession[T](dataKey)
    }
  }

  def put[T](dataKey: DataKey[T], value: T)
            (implicit writes: Writes[T], request: Request[_]): Future[(String, String)] = {
    dataKey match {
      case key: DataKey[String @unchecked] if Seq(NAME, EMAIL, PHONE, ARN, DESCRIPTION, EMAIL_PENDING_VERIFICATION).contains(key) =>
        sessionCacheRepository.putSession(key, value)(EncryptedStringUtil.fallbackStringFormat, request)
      case key: DataKey[DesignatoryDetails @unchecked] if key == DRAFT_NEW_CONTACT_DETAILS =>
        sessionCacheRepository.putSession(key, value)(DesignatoryDetails.databaseFormat, request)
      case key: DataKey[YourDetails @unchecked] if key == DRAFT_SUBMITTED_BY =>
        sessionCacheRepository.putSession(key, value)(YourDetails.databaseFormat, request)
      case _ =>
        sessionCacheRepository.putSession(dataKey, value)
    }
  }

  def delete[T](dataKey: DataKey[T])
               (implicit request: Request[_]): Future[Unit] = {
    sessionCacheRepository.deleteFromSession(dataKey)
  }

  def getSessionItems()(implicit request: Request[_], ec: ExecutionContext): Future[List[Option[String]]] = {
    Future.sequence(sessionKeys.map(get(_)))
  }

  def clearSession()(implicit request: Request[_], ec: ExecutionContext): Future[Unit] = {
    Future.sequence(sessionKeys.map(delete(_))).map(_ => ())
  }
}
