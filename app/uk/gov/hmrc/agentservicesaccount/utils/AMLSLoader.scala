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

package uk.gov.hmrc.agentservicesaccount.utils

import javax.inject.Inject
import javax.inject.Singleton
import scala.util.Failure
import scala.util.Success
import scala.util.Try

@Singleton
class AMLSLoader @Inject() () {

  def load(path: String): Map[String, String] =
    Try {
      require(path.nonEmpty, "AMLS file path cannot be empty")
      require(path.endsWith(".csv"), "AMLS file should be a csv file")

      val header = 1
      val items = scala.io.Source.fromInputStream(this.getClass.getResourceAsStream(path), "utf-8")
      items
        .getLines()
        .drop(header)
        .toSeq
        .map { line =>
          line.split(",").map(_.trim) match {
            case Array(code, bodyName) => (code, bodyName)
            case _ => throw new AMLSLoaderException(s"Strange line in AMLS csv file")
          }
        }
        .toMap
    } match {
      case Success(amlsCodesToNames) => amlsCodesToNames
      case Failure(ex) => throw new AMLSLoaderException(ex.getMessage)
    }

  final class AMLSLoaderException(message: String)
  extends Exception(s"Unexpected error while loading AMLS Bodies: $message")

}
