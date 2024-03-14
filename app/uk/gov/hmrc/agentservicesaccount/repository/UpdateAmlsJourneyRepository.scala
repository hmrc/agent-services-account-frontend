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

import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.cache.{SessionCacheRepository => HmrcSessionCacheRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

@Singleton
class UpdateAmlsJourneyRepository @Inject()(mongoComponent: MongoComponent,
                                            timestampSupport: TimestampSupport)(implicit ec: ExecutionContext)
extends HmrcSessionCacheRepository(
  mongoComponent = mongoComponent,
  collectionName = "update-amls-journey",
  ttl = 4.hours, //sessions can last for a maximum of 4 hours
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
)
