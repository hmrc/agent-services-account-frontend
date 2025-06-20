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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.testkit.NoMaterializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import play.api.test.Helpers._

/** Provides an implicit Materializer for use in tests. Note that if your test is starting an app (e.g. via OneAppPerSuite or OneAppPerTest) then you should
  * probably use the app's Materializer instead.
  */
trait PekkoMaterializerSpec
extends UnitSpec
with BeforeAndAfterAll { this: Suite =>

  implicit lazy val actorSystem: ActorSystem = ActorSystem()
  implicit lazy val materializer: NoMaterializer.type = NoMaterializer

  override protected def afterAll(): Unit = {
    super.afterAll()
    await(actorSystem.terminate())
    ()
  }

}
