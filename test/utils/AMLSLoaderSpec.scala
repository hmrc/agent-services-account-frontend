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
import uk.gov.hmrc.agentservicesaccount.utils.AMLSLoader

class AMLSLoaderSpec extends PlaySpec {

  val amlsLoader = new AMLSLoader

  "AMLSLoader.load" should {
    "load the data" in {
      val result = amlsLoader.load("/amls.csv")
      result.isInstanceOf[Map[String, String]] mustBe true
      result.size mustBe 27
    }

    "throw AMLSLoaderException when no path specified" in {
      intercept[amlsLoader.AMLSLoaderException]{
        amlsLoader.load("")
      }.getMessage mustBe "Unexpected error while loading AMLS Bodies: requirement failed: AMLS file path cannot be empty"
    }

    "throw AMLSLoaderException when not a .csv file type specified" in {
      intercept[amlsLoader.AMLSLoaderException]{
        amlsLoader.load("/amls.xls")
      }.getMessage mustBe "Unexpected error while loading AMLS Bodies: requirement failed: AMLS file should be a csv file"
    }
  }

}
