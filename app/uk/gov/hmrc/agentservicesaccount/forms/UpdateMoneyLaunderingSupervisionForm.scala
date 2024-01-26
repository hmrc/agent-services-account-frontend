package uk.gov.hmrc.agentservicesaccount.forms

import play.api.data.Form
import play.api.data.Forms.{single, text}


object UpdateMoneyLaunderingSupervisionForm{
  private val supervisoryBodyRegex = """^[A-Za-z0-9\,\.\'\-\/\ ]{2,200}$""".r
  private val supervisoryNumberRegex = """^(\+44|0)\d{9,12}$""".r // remove all spaces from input before matching to ensure correct digit count
  private val supervisoryEndDateRegex = """^.{1,252}@.{1,256}\..{1,256}$""".r // This error handling needs to change for dates

  private val trimmedText = text.transform[String](x => x.trim, x => x)

  val supervisoryBodyForm: Form[String] = Form(
    single("name" -> trimmedText
      .verifying("update-contact-details.name.error.empty", _.nonEmpty) // message keys needs to change
      .verifying("update-contact-details.name.error.invalid", x => x.isEmpty || supervisoryBodyRegex.matches(x)) // message keys needs to change
    )
  )
  val supervisoryNumberForm: Form[String] = Form(
    single("telephoneNumber" -> trimmedText
      .verifying("update-contact-details.phone.error.empty", _.nonEmpty) // message keys needs to change
      .verifying("update-contact-details.phone.error.invalid", x => x.isEmpty || supervisoryNumberRegex.matches(x.replace(" ",""))) // message keys needs to change
    )
  )
  val supervisoryEndDateForm: Form[String] = Form( // This error handling needs to change for dates
    single("emailAddress" -> trimmedText
      .verifying("update-contact-details.email.error.empty", _.nonEmpty)  // message keys needs to change
      .verifying("update-contact-details.email.error.invalid", x => x.isEmpty || supervisoryEndDateRegex.matches(x)) // message keys needs to change
    )
  )
}