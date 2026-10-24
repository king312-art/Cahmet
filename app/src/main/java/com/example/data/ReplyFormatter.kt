package com.example.data

object ReplyFormatter {
  const val DEFAULT_GREETING =
    "Assalam-o-Alaikum 😊\nWelcome! Chamet Diamonds available hain 💎"

  const val DEFAULT_PAYMENT_NUMBERS =
    "JazzCash / EasyPaisa: 0300-0000000 (Update in App)\nWhatsApp: 0300-0000000"

  const val DEFAULT_CLOSING = "Shukriya ❤️"

  const val DEFAULT_CHAMET_PACKAGE = "com.hkfuliao.chamet"

  fun buildCustomerReply(
    greeting: String,
    packages: List<PricePackageEntity>,
    paymentNumbers: String,
    closing: String,
  ): String {
    val cleanGreeting = greeting.trim().ifEmpty { DEFAULT_GREETING }
    val priceListBlock =
      if (packages.isEmpty()) {
        PricePackageEntity.defaultPackages().joinToString("\n") { it.toFormattedBullet() }
      } else {
        packages.joinToString("\n") { it.toFormattedBullet() }
      }
    val cleanPayment = paymentNumbers.trim().ifEmpty { DEFAULT_PAYMENT_NUMBERS }
    val cleanClosing = closing.trim().ifEmpty { DEFAULT_CLOSING }

    return buildString {
      append(cleanGreeting)
      append("\n\nMy diamond price list:\n")
      append(priceListBlock)
      append("\n\nPayment and contact details:\n")
      append(cleanPayment)
      append("\n\n")
      append(cleanClosing)
    }
  }
}
