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

package uk.gov.hmrc.agentservicesaccount.model.accessgroups

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.JsResultException
import play.api.libs.json.JsString
import play.api.libs.json.Json
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptinEvent
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptinEventType
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptinRecord
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.AgentUser
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedIn
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedInNotReady
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedInReady
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedInSingleUser
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedOut
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedOutEligible
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedOutSingleUser
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptedOutWrongClientCount
import uk.gov.hmrc.agentservicesaccount.models.accessgroups.OptinStatus
import uk.gov.hmrc.agentservicesaccount.models.Arn

import java.time.LocalDateTime

class OptinRecordSpec
extends AnyWordSpecLike
with Matchers {

  val arn: Arn = Arn("KARN1234567")
  val user: AgentUser = AgentUser("userId", "userName")

  def withOptinRecord(mapStatusToEpoch: List[(OptinEventType, LocalDateTime)]): OptinRecord = OptinRecord(
    arn,
    mapStatusToEpoch.map { case (optedStatus, epoch) =>
      OptinEvent(
        optedStatus,
        user,
        epoch
      )
    }
  )

  s"OptinRecord status" when {

    "no events exist" should {

      s"be $OptedOut" in {
        withOptinRecord(List.empty).status shouldBe OptedOut
      }
    }

    "only a single opted event exists" when {

      s"has status $OptedIn" should {
        s"be $OptedIn" in {
          withOptinRecord(List(OptedIn -> LocalDateTime.now())).status shouldBe OptedIn
        }
      }

      s"has status $OptedOut" should {
        s"be $OptedOut" in {
          withOptinRecord(List(OptedOut -> LocalDateTime.now())).status shouldBe OptedOut
        }
      }
    }

    "multiple opted events exist" when {

      s"has latest $OptedIn event" should {
        s"be $OptedIn" in {
          val now = LocalDateTime.now()

          withOptinRecord(
            List(
              OptedIn -> now.minusDays(1),
              OptedOut -> now.minusSeconds(1),
              OptedIn -> now
            )
          ).status shouldBe OptedIn
        }
      }

      s"has latest $OptedOut event" should {
        s"be $OptedOut" in {
          val now = LocalDateTime.now()

          withOptinRecord(
            List(
              OptedOut -> now.minusDays(1),
              OptedIn -> now.minusNanos(1000),
              OptedOut -> now
            )
          ).status shouldBe OptedOut
        }
      }
    }

  }

  "OptinRecord" should {
    "return the correct ARN" in {
      val arn = Arn("KARN1234567")
      val optinRecord = OptinRecord(arn, List.empty)
      optinRecord.arn shouldBe arn
    }

    "return the correct history" in {
      val user1 = AgentUser("user1", "User One")
      val user2 = AgentUser("user2", "User Two")
      val history = List(
        OptinEvent(
          OptedIn,
          user1,
          LocalDateTime.now()
        ),
        OptinEvent(
          OptedOut,
          user2,
          LocalDateTime.now()
        )
      )
      val optinRecord = OptinRecord(Arn("KARN1234567"), history)
      optinRecord.history shouldBe history
    }
    "return an empty history when no events exist" in {
      val optinRecord = OptinRecord(Arn("KARN1234567"), List.empty)
      optinRecord.history shouldBe empty
    }
    "serialise and deserialise correctly" in {
      val optinRecord: OptinRecord = withOptinRecord(List(OptedIn -> LocalDateTime.now()))
      val json = Json.toJson(optinRecord)
      val deserialised = Json.fromJson[OptinRecord](json).get
      deserialised shouldBe optinRecord
    }
  }
  "OptinStatus" should {
    "return OptedInSingleUser when value is Opted-In_SINGLE_USER" in {
      val optinStatus = JsString("Opted-In_SINGLE_USER")
      optinStatus.as[OptinStatus] shouldBe OptedInSingleUser
    }
    "return OptedOutSingleUser when value is Opted-Out_SINGLE_USER" in {
      val optinStatus = JsString("Opted-Out_SINGLE_USER")
      optinStatus.as[OptinStatus] shouldBe OptedOutSingleUser
    }
    "return OptedOutWrongClientCount when value is Opted-Out_WRONG_CLIENT_COUNT" in {
      val optinStatus = JsString("Opted-Out_WRONG_CLIENT_COUNT")
      optinStatus.as[OptinStatus] shouldBe OptedOutWrongClientCount
    }
    "return OptedOutEligible when value is Opted-Out_ELIGIBLE" in {
      val optinStatus = JsString("Opted-Out_ELIGIBLE")
      optinStatus.as[OptinStatus] shouldBe OptedOutEligible
    }
    "return OptedInReady when value is Opted-In_READY" in {
      val optinStatus = JsString("Opted-In_READY")
      optinStatus.as[OptinStatus] shouldBe OptedInReady
    }
    "return OptedInNotReady when value is Opted-In_NOT_READY" in {
      val optinStatus = JsString("Opted-In_NOT_READY")
      optinStatus.as[OptinStatus] shouldBe OptedInNotReady
    }
  }
  "OptinEventType" should {
    "return OptedIn when value is OptedIn" in {
      val optinEventType = JsString("OptedIn")
      optinEventType.as[OptinEventType] shouldBe OptedIn
    }
    "return OptedOut when value is OptedOut" in {
      val optinEventType = JsString("OptedOut")
      optinEventType.as[OptinEventType] shouldBe OptedOut
    }
    "return an error when value is invalid" in {
      val optinEventType = JsString("invalid")
      an[JsResultException] should be thrownBy optinEventType.as[OptinEventType]
    }
  }

}
