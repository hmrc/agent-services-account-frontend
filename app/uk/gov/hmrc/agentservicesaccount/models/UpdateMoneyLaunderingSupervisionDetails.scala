package uk.gov.hmrc.agentservicesaccount.models

import play.api.libs.json.{Json, OFormat}

case class UpdateMoneyLaunderingSupervisionDetails(body: String,
                                                   number: String,
                                                   EndDate: String // date-input
                                                  )

object UpdateMoneyLaunderingSupervisionDetails{
  implicit val formats: OFormat[UpdateMoneyLaunderingSupervisionDetails] = Json.format[UpdateMoneyLaunderingSupervisionDetails]
}

