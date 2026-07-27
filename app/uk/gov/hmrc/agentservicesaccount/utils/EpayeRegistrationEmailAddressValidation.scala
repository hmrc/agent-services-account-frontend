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

package uk.gov.hmrc.agentservicesaccount.utils

import javax.naming.Context.INITIAL_CONTEXT_FACTORY as ICF
import javax.naming.directory.Attribute
import javax.naming.directory.InitialDirContext
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex
import scala.util.Success
import scala.util.Try

class EpayeRegistrationEmailAddressValidation {

  def isValid(email: String): Boolean =
    email match {
      case EpayeRegistrationEmailAddressValidation.validEmail(_, domain) => isHostMailServer(domain)
      case _ => false
    }

  private def isHostMailServer(domain: String): Boolean = {
    val attributeMX = getAttributeValue(domain, "MX")
    val attributeA = getAttributeValue(domain, "A")

    (attributeMX, attributeA) match {
      case (Success(value), _) if value.nonEmpty => true
      case (_, Success(value)) => value.nonEmpty
      case _ => false
    }
  }

  private def getAttributeValue(
    domain: String,
    attribute: String
  ): Try[List[Attribute]] =
    Try {
      EpayeRegistrationEmailAddressValidation.ictx.getAttributes(domain, Array(attribute)).getAll.asScala.toList
    }

}

object EpayeRegistrationEmailAddressValidation {

  private val validEmail: Regex = """^([a-zA-Z0-9.!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9-]+)*)$""".r

  private lazy val ictx = {
    val DNS_CONTEXT_FACTORY = "com.sun.jndi.dns.DnsContextFactory"
    val env = new java.util.Hashtable[String, String]()
    env.put(ICF, DNS_CONTEXT_FACTORY)

    new InitialDirContext(env)
  }

}
