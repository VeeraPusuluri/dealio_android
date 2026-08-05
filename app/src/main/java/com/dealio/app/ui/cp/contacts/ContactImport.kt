package com.dealio.app.ui.cp.contacts

import android.content.Context
import android.provider.ContactsContract
import com.dealio.app.data.api.CpContactPayload

/**
 * A salaried buyer can realistically commit about a fifth of what they earn to
 * property in a year. An imported list carries salary but never an investment
 * figure, so seed one at this rate — the CP can then sort a fresh address book
 * by who can actually buy, and correct the number as they learn it.
 *
 * The backend seeds identically (utils/contactMoney.ts); it is repeated here so
 * the import preview can show the CP what is about to be saved.
 */
private const val INVESTMENT_RATE = 0.2

fun seedInvestment(salary: Double?): Double? =
    salary?.takeIf { it > 0 }?.let { Math.round(it * INVESTMENT_RATE).toDouble() }

/**
 * One row staged for import, from a spreadsheet or the phone's address book.
 * Selectable so the CP can untick rows before committing.
 */
data class ImportContact(
    val name: String,
    val phone: String,
    val countryCode: String = DEFAULT_DIAL_CODE,
    val email: String? = null,
    val designation: String? = null,
    val salary: Double? = null,
    val investment: Double? = null,
    val address: String? = null,
    val bhkPreference: String? = null,
    val selected: Boolean = true,
) {
    fun toPayload() = CpContactPayload(
        name = name,
        phone = phone,
        countryCode = countryCode,
        email = email,
        tags = "Imported",
        bhkPreference = bhkPreference,
        designation = designation,
        salary = salary,
        investment = investment,
        address = address,
    )
}

/**
 * Digits only, with any country code the number carried split off rather than
 * truncated away. See [splitDialCode] — this used to keep the last ten digits of
 * everything, which turned "+971 50 123 4567" into a plausible-looking Indian
 * number instead of a UAE one.
 */
internal fun normalizePhone(raw: String): String = splitDialCode(raw).second

/**
 * Maps a sheet's header row onto contact fields.
 *
 * Matching is by substring against the names people actually use, so an export
 * from another CRM imports without being renamed first. Name and phone are
 * required; everything else is best-effort.
 */
fun rowsToContacts(rows: List<List<String>>): List<ImportContact> {
    if (rows.size < 2) return emptyList()
    val header = rows.first().map { it.lowercase().trim() }
    fun find(vararg keys: String) = header.indexOfFirst { h -> keys.any { h.contains(it) } }

    val nameIdx = find("name", "contact", "customer")
    val phoneIdx = find("phone", "mobile", "number", "contact no")
    if (nameIdx < 0 || phoneIdx < 0) return emptyList()

    val emailIdx = find("email", "mail")
    val desigIdx = find("designation", "role", "job", "title", "occupation")
    val salaryIdx = find("salary", "income", "ctc", "package")
    // A sheet that already carries a real figure beats anything derived from salary.
    val investIdx = find("investment", "budget", "corpus", "capacity")
    val addrIdx = find("address", "location", "city", "area")
    val bhkIdx = find("bhk", "preference", "config")
    // An explicit column wins; otherwise the code is read off the number itself.
    val ccIdx = find("country code", "countrycode", "dial", "isd")

    fun cell(row: List<String>, i: Int) = if (i >= 0) row.getOrNull(i)?.trim().orEmpty() else ""
    fun money(row: List<String>, i: Int) =
        cell(row, i).filter { it.isDigit() || it == '.' }.toDoubleOrNull()?.takeIf { it > 0 }

    return rows.drop(1).mapNotNull { row ->
        val name = cell(row, nameIdx)
        val declared = cell(row, ccIdx).ifBlank { null }
        val (code, phone) = splitDialCode(
            cell(row, phoneIdx),
            fallback = declared?.let(::normalizeDialCode) ?: DEFAULT_DIAL_CODE,
        )
        if (name.isBlank() || phone.length < 6) return@mapNotNull null
        val salary = money(row, salaryIdx)
        ImportContact(
            name = name,
            phone = phone,
            countryCode = code,
            email = cell(row, emailIdx).ifBlank { null },
            designation = cell(row, desigIdx).ifBlank { null },
            salary = salary,
            investment = money(row, investIdx) ?: seedInvestment(salary),
            address = cell(row, addrIdx).ifBlank { null },
            bhkPreference = cell(row, bhkIdx).ifBlank { null },
        )
    }.distinctBy { it.countryCode + it.phone }
}

/**
 * Everyone in the phone's address book who has a number.
 *
 * Deduped by normalized phone — a person saved with both a mobile and a work
 * number would otherwise appear twice with the same name.
 */
fun readDeviceContacts(context: Context): List<ImportContact> {
    val out = LinkedHashMap<String, ImportContact>()
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )
    context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
    )?.use { c ->
        val nameCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (c.moveToNext()) {
            val name = c.getString(nameCol)?.trim().orEmpty()
            // Address books usually store the code ("+971 50…"); keep it rather
            // than assuming everyone in the book is Indian.
            val (code, phone) = splitDialCode(c.getString(numCol).orEmpty())
            if (name.isBlank() || phone.length < 6) continue
            out.putIfAbsent(code + phone, ImportContact(name = name, phone = phone, countryCode = code, selected = false))
        }
    }
    return out.values.toList()
}
