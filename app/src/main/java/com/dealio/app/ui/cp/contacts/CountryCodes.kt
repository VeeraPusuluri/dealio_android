package com.dealio.app.ui.cp.contacts

/**
 * Dial codes a CP can pick from.
 *
 * A contact's number is held as two pieces: the dial code and the national
 * number. They stay apart rather than being stored as one E.164 string because
 * lead matching and dedupe key on the bare national digits; folding "+971" into
 * the number would quietly break both.
 *
 * The backend keeps the same list (utils/phone.ts) — it has to, because that is
 * what splits an imported "+33 6 12 34 56 78" into France rather than assuming
 * India and truncating it. Adding a country here without adding it there leaves
 * the number storable but not parseable on import.
 */
data class DialCode(val code: String, val flag: String, val label: String)

/**
 * The markets that actually come up — India, the Gulf, and where NRI buyers
 * live. These lead the picker; [DIAL_CODES] carries the rest of the world
 * behind a search box.
 */
val COMMON_DIAL_CODES = listOf(
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

/**
 * Everywhere else, alphabetically. One entry per distinct dial code: the +1
 * Caribbean territories are deliberately absent so that a North American number
 * always reads as US/Canada rather than being split into whichever island
 * happens to share its first three digits.
 */
private val OTHER_DIAL_CODES = listOf(
    DialCode("+93", "🇦🇫", "Afghanistan"),
    DialCode("+355", "🇦🇱", "Albania"),
    DialCode("+213", "🇩🇿", "Algeria"),
    DialCode("+376", "🇦🇩", "Andorra"),
    DialCode("+244", "🇦🇴", "Angola"),
    DialCode("+54", "🇦🇷", "Argentina"),
    DialCode("+374", "🇦🇲", "Armenia"),
    DialCode("+297", "🇦🇼", "Aruba"),
    DialCode("+43", "🇦🇹", "Austria"),
    DialCode("+994", "🇦🇿", "Azerbaijan"),
    DialCode("+880", "🇧🇩", "Bangladesh"),
    DialCode("+375", "🇧🇾", "Belarus"),
    DialCode("+32", "🇧🇪", "Belgium"),
    DialCode("+501", "🇧🇿", "Belize"),
    DialCode("+229", "🇧🇯", "Benin"),
    DialCode("+975", "🇧🇹", "Bhutan"),
    DialCode("+591", "🇧🇴", "Bolivia"),
    DialCode("+387", "🇧🇦", "Bosnia & Herzegovina"),
    DialCode("+267", "🇧🇼", "Botswana"),
    DialCode("+55", "🇧🇷", "Brazil"),
    DialCode("+673", "🇧🇳", "Brunei"),
    DialCode("+359", "🇧🇬", "Bulgaria"),
    DialCode("+226", "🇧🇫", "Burkina Faso"),
    DialCode("+257", "🇧🇮", "Burundi"),
    DialCode("+855", "🇰🇭", "Cambodia"),
    DialCode("+237", "🇨🇲", "Cameroon"),
    DialCode("+238", "🇨🇻", "Cape Verde"),
    DialCode("+236", "🇨🇫", "Central African Republic"),
    DialCode("+235", "🇹🇩", "Chad"),
    DialCode("+56", "🇨🇱", "Chile"),
    DialCode("+86", "🇨🇳", "China"),
    DialCode("+57", "🇨🇴", "Colombia"),
    DialCode("+269", "🇰🇲", "Comoros"),
    DialCode("+242", "🇨🇬", "Congo"),
    DialCode("+243", "🇨🇩", "Congo (DRC)"),
    DialCode("+506", "🇨🇷", "Costa Rica"),
    DialCode("+225", "🇨🇮", "Côte d'Ivoire"),
    DialCode("+385", "🇭🇷", "Croatia"),
    DialCode("+53", "🇨🇺", "Cuba"),
    DialCode("+357", "🇨🇾", "Cyprus"),
    DialCode("+420", "🇨🇿", "Czechia"),
    DialCode("+45", "🇩🇰", "Denmark"),
    DialCode("+253", "🇩🇯", "Djibouti"),
    DialCode("+593", "🇪🇨", "Ecuador"),
    DialCode("+20", "🇪🇬", "Egypt"),
    DialCode("+503", "🇸🇻", "El Salvador"),
    DialCode("+240", "🇬🇶", "Equatorial Guinea"),
    DialCode("+291", "🇪🇷", "Eritrea"),
    DialCode("+372", "🇪🇪", "Estonia"),
    DialCode("+268", "🇸🇿", "Eswatini"),
    DialCode("+251", "🇪🇹", "Ethiopia"),
    DialCode("+679", "🇫🇯", "Fiji"),
    DialCode("+358", "🇫🇮", "Finland"),
    DialCode("+33", "🇫🇷", "France"),
    DialCode("+241", "🇬🇦", "Gabon"),
    DialCode("+220", "🇬🇲", "Gambia"),
    DialCode("+995", "🇬🇪", "Georgia"),
    DialCode("+233", "🇬🇭", "Ghana"),
    DialCode("+30", "🇬🇷", "Greece"),
    DialCode("+502", "🇬🇹", "Guatemala"),
    DialCode("+224", "🇬🇳", "Guinea"),
    DialCode("+592", "🇬🇾", "Guyana"),
    DialCode("+509", "🇭🇹", "Haiti"),
    DialCode("+504", "🇭🇳", "Honduras"),
    DialCode("+36", "🇭🇺", "Hungary"),
    DialCode("+354", "🇮🇸", "Iceland"),
    DialCode("+62", "🇮🇩", "Indonesia"),
    DialCode("+98", "🇮🇷", "Iran"),
    DialCode("+964", "🇮🇶", "Iraq"),
    DialCode("+972", "🇮🇱", "Israel"),
    DialCode("+39", "🇮🇹", "Italy"),
    DialCode("+81", "🇯🇵", "Japan"),
    DialCode("+962", "🇯🇴", "Jordan"),
    DialCode("+7", "🇰🇿", "Kazakhstan / Russia"),
    DialCode("+254", "🇰🇪", "Kenya"),
    DialCode("+996", "🇰🇬", "Kyrgyzstan"),
    DialCode("+856", "🇱🇦", "Laos"),
    DialCode("+371", "🇱🇻", "Latvia"),
    DialCode("+961", "🇱🇧", "Lebanon"),
    DialCode("+266", "🇱🇸", "Lesotho"),
    DialCode("+231", "🇱🇷", "Liberia"),
    DialCode("+218", "🇱🇾", "Libya"),
    DialCode("+423", "🇱🇮", "Liechtenstein"),
    DialCode("+370", "🇱🇹", "Lithuania"),
    DialCode("+352", "🇱🇺", "Luxembourg"),
    DialCode("+853", "🇲🇴", "Macau"),
    DialCode("+261", "🇲🇬", "Madagascar"),
    DialCode("+265", "🇲🇼", "Malawi"),
    DialCode("+960", "🇲🇻", "Maldives"),
    DialCode("+223", "🇲🇱", "Mali"),
    DialCode("+356", "🇲🇹", "Malta"),
    DialCode("+222", "🇲🇷", "Mauritania"),
    DialCode("+230", "🇲🇺", "Mauritius"),
    DialCode("+52", "🇲🇽", "Mexico"),
    DialCode("+373", "🇲🇩", "Moldova"),
    DialCode("+377", "🇲🇨", "Monaco"),
    DialCode("+976", "🇲🇳", "Mongolia"),
    DialCode("+382", "🇲🇪", "Montenegro"),
    DialCode("+212", "🇲🇦", "Morocco"),
    DialCode("+258", "🇲🇿", "Mozambique"),
    DialCode("+95", "🇲🇲", "Myanmar"),
    DialCode("+264", "🇳🇦", "Namibia"),
    DialCode("+977", "🇳🇵", "Nepal"),
    DialCode("+31", "🇳🇱", "Netherlands"),
    DialCode("+505", "🇳🇮", "Nicaragua"),
    DialCode("+227", "🇳🇪", "Niger"),
    DialCode("+234", "🇳🇬", "Nigeria"),
    DialCode("+389", "🇲🇰", "North Macedonia"),
    DialCode("+47", "🇳🇴", "Norway"),
    DialCode("+92", "🇵🇰", "Pakistan"),
    DialCode("+970", "🇵🇸", "Palestine"),
    DialCode("+507", "🇵🇦", "Panama"),
    DialCode("+675", "🇵🇬", "Papua New Guinea"),
    DialCode("+595", "🇵🇾", "Paraguay"),
    DialCode("+51", "🇵🇪", "Peru"),
    DialCode("+63", "🇵🇭", "Philippines"),
    DialCode("+48", "🇵🇱", "Poland"),
    DialCode("+351", "🇵🇹", "Portugal"),
    DialCode("+40", "🇷🇴", "Romania"),
    DialCode("+250", "🇷🇼", "Rwanda"),
    DialCode("+685", "🇼🇸", "Samoa"),
    DialCode("+378", "🇸🇲", "San Marino"),
    DialCode("+221", "🇸🇳", "Senegal"),
    DialCode("+381", "🇷🇸", "Serbia"),
    DialCode("+248", "🇸🇨", "Seychelles"),
    DialCode("+232", "🇸🇱", "Sierra Leone"),
    DialCode("+421", "🇸🇰", "Slovakia"),
    DialCode("+386", "🇸🇮", "Slovenia"),
    DialCode("+252", "🇸🇴", "Somalia"),
    DialCode("+82", "🇰🇷", "South Korea"),
    DialCode("+211", "🇸🇸", "South Sudan"),
    DialCode("+34", "🇪🇸", "Spain"),
    DialCode("+94", "🇱🇰", "Sri Lanka"),
    DialCode("+249", "🇸🇩", "Sudan"),
    DialCode("+597", "🇸🇷", "Suriname"),
    DialCode("+46", "🇸🇪", "Sweden"),
    DialCode("+41", "🇨🇭", "Switzerland"),
    DialCode("+963", "🇸🇾", "Syria"),
    DialCode("+886", "🇹🇼", "Taiwan"),
    DialCode("+992", "🇹🇯", "Tajikistan"),
    DialCode("+255", "🇹🇿", "Tanzania"),
    DialCode("+66", "🇹🇭", "Thailand"),
    DialCode("+228", "🇹🇬", "Togo"),
    DialCode("+216", "🇹🇳", "Tunisia"),
    DialCode("+90", "🇹🇷", "Türkiye"),
    DialCode("+993", "🇹🇲", "Turkmenistan"),
    DialCode("+256", "🇺🇬", "Uganda"),
    DialCode("+380", "🇺🇦", "Ukraine"),
    DialCode("+598", "🇺🇾", "Uruguay"),
    DialCode("+998", "🇺🇿", "Uzbekistan"),
    DialCode("+58", "🇻🇪", "Venezuela"),
    DialCode("+84", "🇻🇳", "Vietnam"),
    DialCode("+967", "🇾🇪", "Yemen"),
    DialCode("+260", "🇿🇲", "Zambia"),
    DialCode("+263", "🇿🇼", "Zimbabwe"),
)

/** Everything the parser recognises: the common markets, then the rest. */
val DIAL_CODES = COMMON_DIAL_CODES + OTHER_DIAL_CODES

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

/**
 * Whether two saved rows are the same person: the last ten digits of the number.
 *
 * Two rows for one buyer rarely agree on formatting — one was typed with the
 * code folded in, the other imported with it split off — but they always agree
 * here. It is also the identity the server matches leads and invitees on.
 */
fun phoneIdentity(countryCode: String?, phone: String): String =
    dialable(countryCode, phone).takeLast(10)
