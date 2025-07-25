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

package utils

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.agentservicesaccount.utils.ViewUtils

import java.time.LocalDate

class ViewUtilsSpec
extends PlaySpec {

  "ViewUtils.convertLocalDateToDisplayDate" should {
    "convert '2023-12-25' to '25/12/2023'" in {
      val date = LocalDate.of(2023, 12, 25)
      ViewUtils.convertLocalDateToDisplayDate(date) mustBe "25/12/2023"
    }
    "convert '2023-2-1' to '01/02/2023'" in {
      val date = LocalDate.of(2023, 2, 1)
      ViewUtils.convertLocalDateToDisplayDate(date) mustBe "01/02/2023"
    }
  }

}
