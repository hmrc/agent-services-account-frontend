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

import play.api.i18n.Messages
import uk.gov.hmrc.agents.accessgroups.optin.OptedInNotReady
import uk.gov.hmrc.agents.accessgroups.optin.OptedInReady
import uk.gov.hmrc.agents.accessgroups.optin.OptedInSingleUser
import uk.gov.hmrc.agents.accessgroups.optin.OptinStatus

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ViewUtils {

  def withErrorPrefix(
    hasFormErrors: Boolean,
    str: String
  )(implicit mgs: Messages): String = {
    val errorPrefix =
      if (hasFormErrors) { mgs("error-prefix") + " " }
      else { "" }
    errorPrefix.concat(mgs(str))
  }

  def isOptedIn(optinStatus: OptinStatus): Boolean = {
    optinStatus match {
      case OptedInReady | OptedInNotReady | OptedInSingleUser => true
      case _ => false
    }
  }

  // converts 2024-1-25 to 25/01/2024
  def convertLocalDateToDisplayDate(localDate: LocalDate): String = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(localDate)

}
