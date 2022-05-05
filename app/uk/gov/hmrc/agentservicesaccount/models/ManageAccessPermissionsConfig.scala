/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.agentservicesaccount.models

import uk.gov.hmrc.agentservicesaccount.config.AppConfig

case class ManageAccessPermissionsConfig(status: String, statusClass: String, insetText: Option[String], accessGroups: List[Link], settings: List[Link])

case class Link(msgKey: String, href: String)

object ManageAccessPermissionsConfig {

  def apply(optinStatus: Option[OptinStatus])(implicit appConfig: AppConfig): Option[ManageAccessPermissionsConfig] ={
    optinStatus.map {
      case OptedInReady => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-in",
        statusClass = "govuk-tag",
        insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-In_READY"),
        accessGroups =
          List(
            Link(msgKey = "manage.account.manage-access-permissions.access-groups.create-new",
              href = appConfig.agentPermissionsOptInUrl),
            Link(msgKey = "manage.account.manage-access-permissions.access-groups.manage",
              href = appConfig.agentPermissionsManageAccessGroupsUrl)),
        settings =
          List(
            Link(msgKey = "manage.account.manage-access-permissions.settings.optout",
              href = appConfig.agentPermissionsOptOutUrl)))

      case OptedInNotReady => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-in",
        statusClass = "govuk-tag",
        insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-In_NOT_READY"),
        accessGroups = List.empty,
        settings =
          List(
            Link(msgKey = "manage.account.manage-access-permissions.settings.optout",
              href = appConfig.agentPermissionsOptOutUrl)
          )
      )

      case OptedInSingleUser => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-in",
        statusClass = "govuk-tag",
        insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-In_SINGLE_USER"),
        accessGroups = List.empty,
        settings = List.empty
      )

      case OptedOutEligible => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-out",
        statusClass = "govuk-tag govuk-tag--grey",
        insetText = None,
        accessGroups =
          List(
            Link(msgKey = "manage.account.manage-access-permissions.access-groups.optin",
              href = appConfig.agentPermissionsOptInUrl)
          ),
        settings = List.empty
      )

      case OptedOutWrongClientCount => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-out",
        statusClass = "govuk-tag govuk-tag--grey",
        insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-Out_WRONG_CLIENT_COUNT"),
        accessGroups = List.empty,
        settings = List.empty
      )

      case OptedOutSingleUser => ManageAccessPermissionsConfig(
        status = "manage.account.manage-access-permissions.status-opted-out",
        statusClass = "govuk-tag govuk-tag--grey",
        insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-Out_SINGLE_USER"),
        accessGroups = List.empty,
        settings = List.empty
      )
    }
  }
}
