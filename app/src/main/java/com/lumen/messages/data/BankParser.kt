package com.lumen.messages.data

/**
 * Reads bank alert SMS such as
 *  "Rs.2,500.00 debited from A/c XX1234 on 12-10-26 to VPA shop@upi. Avl Bal Rs 10,234.50"
 *  "KWD 120.000 has been credited to your account ending 5678. Balance: KWD 1,540.250"
 * and turns them into structured transactions. Everything runs on the phone.
 */
enum class TxKind { Credit, Debit }

data class Transaction(
    val messageId: Long,
    val threadId: Long,
    val sender: String,
    val bank: String,
    val account: String,       // last digits, e.g. "1234", or "" when the SMS names no account
    val kind: TxKind,
    val amount: Double,
    val currency: String,
    val balance: Double?,
    val party: String?,
    val date: Long,
    val body: String,
) {
    val accountKey: String get() = "$bank|$account"
    val accountLabel: String get() = if (account.isEmpty()) bank else "$bank ••$account"
}

object BankParser {
    private val otpGuard = Regex("(?i)\\b(otp|one[- ]time password|verification code|do not share|is your code)\\b")
    private val promoGuard = Regex(
        "(?i)(pre-?approved|apply now|offer ends|limited period|upto|up to rs|win |lucky|eligible for|instant loan|loan of|click here|t&c)"
    )
    private val cardPhrases = Regex("(?i)\\b(credit|debit)\\s+card\\b")
    private val debitWords = Regex(
        "(?i)\\b(debited|debit(?:ed)? by|withdrawn|withdrawal|withdrew|spent|paid|purchase[ds]?|deducted|" +
            "sent|transferred to|transfer to|used for|dr\\.?|debit)\\b"
    )
    private val creditWords = Regex(
        "(?i)\\b(credited|deposited|deposit of|received|refund(?:ed)?|reversed|cr\\.?|credit(?:ed)? by|added to|transferred from)\\b"
    )
    private val accountRef = Regex(
        "(?i)\\b(?:a/c|a/c\\.|ac|acct|account|card|acc)\\b(?:\\s*(?:no\\.?|number|num|ending(?:\\s+(?:with|in))?|#|:))*\\s*[xX*•.#\\-]{0,14}\\s?(\\d{3,6})\\b"
    )
    private val endingRef = Regex("(?i)\\bending(?:\\s+(?:with|in))?\\s*[:#]?\\s*[xX*]*(\\d{3,6})\\b")
    private val maskedRef = Regex("\\b[xX*•]{2,}(\\d{3,6})\\b")

    private const val CUR = "(rs\\.?|inr|kwd|kd|k\\.d\\.?|usd|aed|sar|qar|bhd|omr|eur|gbp|₹|\\$|€|£)"
    private const val NUM = "([0-9]{1,3}(?:,[0-9]{2,3})+(?:\\.[0-9]{1,3})?|[0-9]+(?:\\.[0-9]{1,3})?)"
    private val amountPre = Regex("(?i)(?<![a-z])$CUR\\s*$NUM")
    private val amountPost = Regex("(?i)$NUM\\s*$CUR(?![a-z])")
    private val balanceContext = Regex("(?i)(bal(?:ance)?|avl|avail|available|limit|outstanding|due)[^0-9]{0,12}$")
    private val partyRef = Regex(
        "(?i)\\b(?:to|at|from|by|towards|info:?|merchant:?)\\s+([A-Za-z0-9&@._'\\- ]{2,32}?)" +
            "(?=\\s+(?:on|ref|upi|avl|bal|via|for|is|dt|date|thru|through|using|with|has|was)\\b|[.,;:]|\\s+\\d|$)"
    )

    private val knownBanks = listOf(
        "HDFC" to "HDFC Bank", "ICICI" to "ICICI Bank", "SBI" to "SBI", "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak", "IDFC" to "IDFC First", "YESB" to "Yes Bank", "PNB" to "PNB",
        "BOB" to "Bank of Baroda", "CANBNK" to "Canara Bank", "CANARA" to "Canara Bank", "UNION" to "Union Bank",
        "INDUS" to "IndusInd", "FEDBNK" to "Federal Bank", "FEDERAL" to "Federal Bank", "IOB" to "IOB",
        "INDBNK" to "Indian Bank", "CENTBK" to "Central Bank", "AUBANK" to "AU Bank", "PAYTM" to "Paytm",
        "NBK" to "NBK", "KFH" to "KFH", "BOUBYAN" to "Boubyan", "GULF" to "Gulf Bank", "GBK" to "Gulf Bank",
        "BURGAN" to "Burgan Bank", "CBK" to "CBK", "ABK" to "ABK", "WARBA" to "Warba Bank", "KIB" to "KIB",
        "ALRAJHI" to "Al Rajhi", "EMIRATES" to "Emirates NBD", "ENBD" to "Emirates NBD", "ADCB" to "ADCB",
    )

    fun bankName(sender: String, body: String): String {
        val id = sender.uppercase().substringAfterLast('-').filter { it.isLetterOrDigit() }
        knownBanks.firstOrNull { id.contains(it.first) }?.let { return it.second }
        knownBanks.firstOrNull { body.uppercase().contains(it.first + " ") }?.let { return it.second }
        Regex("([A-Z][A-Za-z]+ Bank)").find(body)?.let { return it.groupValues[1] }
        return if (id.any { it.isLetter() }) id.lowercase().replaceFirstChar { it.uppercase() } else sender
    }

    private fun parseNum(s: String): Double? = s.replace(",", "").toDoubleOrNull()

    private fun normCurrency(c: String): String = when (c.lowercase().replace(".", "")) {
        "rs", "inr", "₹" -> "INR"
        "kwd", "kd" -> "KWD"
        "$", "usd" -> "USD"
        "€", "eur" -> "EUR"
        "£", "gbp" -> "GBP"
        else -> c.uppercase().replace(".", "")
    }

    /** Fast pre-check so we only run the regexes on likely bank messages. */
    fun looksFinancial(body: String): Boolean {
        val b = body.lowercase()
        return ("debit" in b || "credit" in b || "withdr" in b || "deposit" in b || "spent" in b ||
            "purchase" in b || "paid" in b || "received" in b || "transfer" in b || "refund" in b) &&
            b.any { it.isDigit() }
    }

    fun parse(messageId: Long, threadId: Long, sender: String, body: String, date: Long): Transaction? {
        if (!looksFinancial(body)) return null
        if (otpGuard.containsMatchIn(body) || promoGuard.containsMatchIn(body)) return null

        val text = cardPhrases.replace(body) { "card" }
        val d = debitWords.find(text)?.range?.first ?: Int.MAX_VALUE
        val c = creditWords.find(text)?.range?.first ?: Int.MAX_VALUE
        if (d == Int.MAX_VALUE && c == Int.MAX_VALUE) return null
        val kind = if (d <= c) TxKind.Debit else TxKind.Credit

        data class Amt(val pos: Int, val value: Double, val cur: String)
        val amounts = ArrayList<Amt>()
        amountPre.findAll(body).forEach { m ->
            parseNum(m.groupValues[2])?.let { amounts += Amt(m.range.first, it, normCurrency(m.groupValues[1])) }
        }
        amountPost.findAll(body).forEach { m ->
            parseNum(m.groupValues[1])?.let { amounts += Amt(m.range.first, it, normCurrency(m.groupValues[2])) }
        }
        if (amounts.isEmpty()) return null
        amounts.sortBy { it.pos }
        val isBalance = { a: Amt -> balanceContext.containsMatchIn(body.substring(maxOf(0, a.pos - 22), a.pos)) }
        val main = amounts.firstOrNull { !isBalance(it) && it.value > 0 } ?: return null
        val balance = amounts.firstOrNull { isBalance(it) }?.value

        val account = accountRef.find(body)?.groupValues?.get(1)
            ?: endingRef.find(body)?.groupValues?.get(1)
            ?: maskedRef.find(body)?.groupValues?.get(1)
            ?: ""
        val businessSender = sender.any { it.isLetter() }
        if (account.isEmpty() && !(businessSender && balance != null)) return null

        val party = partyRef.findAll(body)
            .map { it.groupValues[1].trim() }
            .firstOrNull { p ->
                val l = p.lowercase()
                p.length >= 2 && !l.startsWith("your") && !l.startsWith("a/c") && !l.startsWith("ac ") &&
                    !l.startsWith("account") && !l.startsWith("card") && !l.all { it.isDigit() || it == ' ' }
            }
            ?.take(28)

        return Transaction(
            messageId = messageId,
            threadId = threadId,
            sender = sender,
            bank = bankName(sender, body),
            account = account.takeLast(4),
            kind = kind,
            amount = main.value,
            currency = main.cur,
            balance = balance,
            party = party,
            date = date,
            body = body,
        )
    }
}
