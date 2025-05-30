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

package uk.gov.hmrc.agentservicesaccount.support

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger => LogbackLogger}
import ch.qos.logback.core.read.ListAppender
import play.Logger.ALogger
import play.api.LoggerLike

import scala.jdk.CollectionConverters._

// This trait is a copy and paste from the hmrctest library, which is now deprecated.
trait LogCapturing {

  def withCaptureOfLoggingFrom(logger: LogbackLogger)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
    val appender = new ListAppender[ILoggingEvent]()
    appender.setContext(logger.getLoggerContext)
    appender.start()
    logger.addAppender(appender)
    logger.setLevel(Level.TRACE)
    logger.setAdditive(true)
    body(appender.list.asScala.toList)
  }

  def withCaptureOfLoggingFrom(logger: LoggerLike)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
    withCaptureOfLoggingFrom(logger.logger.asInstanceOf[LogbackLogger])(body)
  }

  def withCaptureOfLoggingFrom(logger: ALogger)(body: (=> List[ILoggingEvent]) => Unit): Unit = {
    withCaptureOfLoggingFrom(logger.underlying.asInstanceOf[LogbackLogger])(body)
  }

}
