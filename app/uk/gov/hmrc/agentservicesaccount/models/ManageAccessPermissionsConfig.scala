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
import uk.gov.hmrc.agentmtdidentifiers.model.{OptedInNotReady, OptedInReady, OptedInSingleUser, OptedOutEligible, OptedOutSingleUser, OptedOutWrongClientCount, OptinStatus}

case class ManageAccessPermissionsConfig(status: String, statusClass: String, insetText: Option[String], explainAccessGroups: Boolean, accessGroups: List[Link], settings: List[Link])

case class Link(msgKey: String, href: String, renderAsButton: Boolean = false)

object ManageAccessPermissionsConfig {

  def apply(optinStatus: OptinStatus, hasAnyGroups: Boolean)(implicit appConfig: AppConfig): ManageAccessPermissionsConfig = optinStatus match {
    case OptedInReady => ManageAccessPermissionsConfig(
      status = "manage.account.manage-access-permissions.status-opted-in",
      statusClass = "govuk-tag",
      insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-In_READY"),
      explainAccessGroups = !hasAnyGroups,
      accessGroups =
        List(
          Link(msgKey = "manage.account.manage-access-permissions.access-groups.create-new",
            href = appConfig.agentPermissionsCreateAccessGroupUrl,
            renderAsButton = !hasAnyGroups
          ),
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
      explainAccessGroups = false,
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
      explainAccessGroups = false,
      accessGroups = List.empty,
      settings = List.empty
    )

    case OptedOutEligible => ManageAccessPermissionsConfig(
      status = "manage.account.manage-access-permissions.status-opted-out",
      statusClass = "govuk-tag govuk-tag--grey",
      insetText = None,
      explainAccessGroups = false,
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
      explainAccessGroups = false,
      accessGroups = List.empty,
      settings = List.empty
    )

    case OptedOutSingleUser => ManageAccessPermissionsConfig(
      status = "manage.account.manage-access-permissions.status-opted-out",
      statusClass = "govuk-tag govuk-tag--grey",
      insetText = Some("manage.account.manage-access-permissions.inset-text.Opted-Out_SINGLE_USER"),
      explainAccessGroups = false,
      accessGroups = List.empty,
      settings = List.empty
    )
  }
}
