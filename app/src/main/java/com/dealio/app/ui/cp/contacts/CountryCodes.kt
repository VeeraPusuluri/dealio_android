package com.dealio.app.ui.cp.contacts

/**
 * Dial codes a CP can pick from — India plus the markets NRI buyers call from.
 *
 * A contact's number is held as two pieces: the dial code and the national
 * number. They stay apart rather than being stored as one E.164 string because
 * lead matching and dedupe key on the bare national digits; folding "+971" into
 * the number would quietly break both.
 *
 * The backend keeps the same list (utils/phone.ts).
 */
data class DialCode(val code: String, val flag: String, val label: String)

val DIAL_CODES = listOf(
    DialCode("+91", "🇮🇳", "India"),
    DialCode("+971", "🇦🇪", "UAE"),
    DialCode("+966", "🇸🇦", "Saudi Arabia"),
    DialCode("+974", "🇶🇦", "Qatar"),
    DialCode("+965", "🇰🇼", "Kuwait"),
    DialCode("+968", "🇴🇲", "Oman"),
    DialCode("+973", "🇧🇭", "Bahrain"),
    DialCode("+1", "🇺🇸", "US / Canada"),
    DialCode("+44", "🇬🇧", "UK"),
    DialCode("+65", "🇸🇬", "Singapore"),
    DialCode("+61", "🇦🇺", "Australia"),
    DialCode("+60", "🇲🇾", "Malaysia"),
    DialCode("+64", "🇳🇿", "New Zealand"),
    DialCode("+852", "🇭🇰", "Hong Kong"),
    DialCode("+49", "🇩🇪", "Germany"),
    DialCode("+353", "🇮🇪", "Ireland"),
    DialCode("+27", "🇿🇦", "South Africa"),
)

const val DEFAULT_DIAL_CODE = "+91"

/** Longest first, so "+971" is never mistaken for "+97" and "+91" never for "+9". */
private val BY_LENGTH = DIAL_CODES.map { it.code }.sortedByDescending { it.length }

fun flagFor(code: String?): String =
    DIAL_CODES.firstOrNull { it.code == normalizeDialCode(code) }?.flag ?: "🌐"

/** "91" / " +91 " -> "+91"; anything unusable falls back to the default. */
fun normalizeDialCode(raw: String?): String {
    val digits = raw?.filter(Char::isDigit).orEmpty()
    return if (digits.isEmpty() || digits.length > 4) DEFAULT_DIAL_CODE else "+$digits"
}

/**
 * Pull a country code off a number that carries one.
 *
 * This is what makes importing work. A phone book stores "+971 50 123 4567" and
 * a CRM export stores "00971501234567"; both have to survive as UAE contacts.
 * The old normalizer assumed India and truncated every number to its last ten
 * digits, which silently corrupted any foreign one it touched.
 */
fun splitDialCode(raw: String, fallback: String = DEFAULT_DIAL_CODE): Pair<String, String> {
    val trimmed = raw.trim()
    val cc = normalizeDialCode(fallback)
    val international = trimmed.startsWith("+") || Regex("^00\\d").containsMatchIn(trimmed)
    var digits = trimmed.filter(Char::isDigit)
    if (Regex("^00\\d").containsMatchIn(trimmed)) digits = digits.drop(2)

    if (international) {
        val match = BY_LENGTH.firstOrNull { digits.startsWith(it.drop(1)) }
        if (match != null) {
            val national = digits.drop(match.length - 1)
            // Guard against a bare "+91" with no number behind it.
            if (national.length >= 6) return match to national
        }
        return cc to digits
    }

    // A local number that still carries its code without the plus ("919876543210").
    val ccDigits = cc.drop(1)
    if (digits.length > 10 && digits.startsWith(ccDigits)) {
        return cc to digits.drop(ccDigits.length)
    }
    return cc to digits
}

/**
 * Split only once the code is unambiguous and a real number follows it.
 *
 * Typing happens one character at a time, so a field sees "+", "+9", "+97",
 * "+971"… in turn. Splitting eagerly would eat the "+" on the first keystroke
 * and leave every later digit looking like a local number — which is exactly
 * how "+971 50 123 4567" ended up saved as an Indian number. Returning null
 * until the number is plausible lets the field hold the raw text meanwhile.
 */
fun trySplitDialCode(raw: String, fallback: String = DEFAULT_DIAL_CODE): Pair<String, String>? {
    val trimmed = raw.trim()
    if (!trimmed.startsWith("+") && !Regex("^00\\d").containsMatchIn(trimmed)) return null
    val (code, national) = splitDialCode(trimmed, fallback)
    val digits = trimmed.filter(Char::isDigit).let { if (trimmed.startsWith("00")) it.drop(2) else it }
    val matched = digits.startsWith(code.drop(1))
    return if (matched && national.length >= 6) code to national else null
}

/** How the number reads on a card: "+91 98765 43210". */
fun formatPhone(countryCode: String?, phone: String): String {
    val cc = normalizeDialCode(countryCode)
    val grouped = if (cc == DEFAULT_DIAL_CODE && phone.length == 10) {
        "${phone.take(5)} ${phone.drop(5)}"
    } else {
        phone
    }
    return "$cc $grouped"
}

/** For wa.me and tel: links — "919876543210", no plus, no spaces. */
fun dialable(countryCode: String?, phone: String): String =
    normalizeDialCode(countryCode).drop(1) + phone.filter(Char::isDigit)
