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

package uk.gov.hmrc.agentservicesaccount.utils

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object ValueClassBinder {

  def valueClassBinder[A: Reads](fromAtoString: A => String)(implicit stringBinder: PathBindable[String]): PathBindable[A] = {

      def parseString(str: String) =
        JsString(str).validate[A] match {
          case JsSuccess(a, _) => Right(a)
          case JsError(error)  => Left(s"No valid value in path: $str. Error: ${error.toString()}")
        }

    new PathBindable[A] {
      override def bind(key: String, value: String): Either[String, A] =
        stringBinder.bind(key, value).flatMap(parseString)

      override def unbind(key: String, a: A): String =
        stringBinder.unbind(key, fromAtoString(a))
    }
  }

  def queryStringValueBinder[A: TypeTag: Reads](fromAtoString: A => String): QueryStringBindable[A] = {
    new QueryStringBindable.Parsing[A](
      parse = JsString(_).as[A],
      fromAtoString,
      {
        case (key: String, e: JsResultException) => s"Cannot parse param $key as ${typeOf[A].typeSymbol.name.toString}. ${e.errors.headOption.flatMap(_._2.headOption.map(_.message)).getOrElse("")}"
        case (key: String, e)                    => s"Cannot parse param $key as ${typeOf[A].typeSymbol.name.toString}. ${e.toString}"
      }
    )
  }

}
