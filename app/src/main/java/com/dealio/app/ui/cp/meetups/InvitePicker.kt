package com.dealio.app.ui.cp.meetups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dealio.app.data.api.CpMeetupInviteePayload
import com.dealio.app.data.api.InvitableContact
import com.dealio.app.data.api.InvitableCustomer
import com.dealio.app.data.api.InvitableResponse
import com.dealio.app.ui.builder.initialsOf
import com.dealio.app.ui.components.IconBlue
import com.dealio.app.ui.components.IconGreen
import com.dealio.app.ui.components.dealioFieldColors
import com.dealio.app.ui.theme.CardBorder
import com.dealio.app.ui.theme.Mist
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

/**
 * One person the organiser could invite, from either source.
 *
 * Contacts and customers are different rows on the wire but the same decision
 * here, so they are flattened into one type. [key] is the last 10 digits — the
 * same identity the server dedupes on, which is what lets someone who is both a
 * saved contact and a Dealio customer appear once, not twice.
 */
data class Invitee(
    val key: String,
    val name: String,
    val phone: String,
    val email: String?,
    val contactId: Long?,
    /** True when this person has a Dealio account — the invite reaches their app. */
    val hasAccount: Boolean,
    /** Their preferred city, for customers. */
    val city: String?,
) {
    fun toPayload() = CpMeetupInviteePayload(contactId = contactId, name = name, phone = phone, email = email)
}

private fun phoneKey(raw: String) = raw.filter { it.isDigit() }.takeLast(10)

/**
 * Whether this is a number someone could actually be reached on.
 *
 * Google-OAuth accounts carry their subject id in the phone column
 * ("google-106241990488465495017"), which has plenty of digits and is not a
 * phone number at all. Inviting one produces a row nobody can be called or
 * messaged on, so they are left off the list rather than shown and left to fail.
 */
private fun isReachable(raw: String): Boolean =
    raw.none { it.isLetter() } && raw.count { it.isDigit() } >= 10

/**
 * Merges the two sources into one list.
 *
 * A customer and a contact holding the same number are one person. The customer
 * record wins on `hasAccount` and city, the contact keeps its `contactId` so the
 * invite stays traceable back to the partner's own book.
 */
fun mergeInvitable(src: InvitableResponse): List<Invitee> {
    val byKey = LinkedHashMap<String, Invitee>()
    src.contacts.forEach { c ->
        val phone = listOfNotNull(c.countryCode?.takeIf { it.isNotBlank() }, c.phone).joinToString("")
        val k = phoneKey(phone)
        if (k.isBlank() || !isReachable(phone)) return@forEach
        byKey[k] = Invitee(k, c.name, phone, c.email, c.id, hasAccount = false, city = null)
    }
    src.customers.forEach { u ->
        val k = phoneKey(u.phone)
        if (k.isBlank() || !isReachable(u.phone)) return@forEach
        val existing = byKey[k]
        byKey[k] = Invitee(
            key = k,
            name = existing?.name?.takeIf { it.isNotBlank() } ?: u.name,
            phone = existing?.phone ?: u.phone,
            email = existing?.email ?: u.email,
            contactId = existing?.contactId,
            hasAccount = true,
            city = u.city,
        )
    }
    return byKey.values.sortedWith(compareByDescending<Invitee> { it.hasAccount }.thenBy { it.name.lowercase() })
}

/**
 * The invite list.
 *
 * Dealio customers sort to the top and carry a badge, because inviting one is
 * materially different from inviting a contact: it lands in their app rather
 * than only in a WhatsApp message they may never open. Their preferred city
 * shows next to them so a partner can see at a glance who is even local.
 */
@Composable
fun InvitePicker(
    people: List<Invitee>,
    selected: Set<String>,
    onToggle: (Invitee) -> Unit,
    modifier: Modifier = Modifier,
    /** Already on the meetup's list — shown, but not selectable again. */
    alreadyInvited: Set<String> = emptySet(),
    maxHeight: Int = 260,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(people, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) people
        else people.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
    }

    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search name or number", color = TextSecondary.copy(alpha = 0.6f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            shape = RoundedCornerShape(12.dp), colors = dealioFieldColors(),
        )
        Spacer(Modifier.height(10.dp))

        when {
            people.isEmpty() -> Text(
                "Nobody to invite yet. Add contacts under More → Contacts, and any Dealio customer will show up here too.",
                color = TextSecondary, fontSize = 12.sp, lineHeight = 17.sp,
            )
            filtered.isEmpty() -> Text("No match for \"$query\".", color = TextSecondary, fontSize = 12.sp)
            else -> LazyColumn(
                Modifier.fillMaxWidth().heightIn(max = maxHeight.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(filtered.size) { i ->
                    val p = filtered[i]
                    InviteeRow(
                        person = p,
                        selected = selected.contains(p.key),
                        already = alreadyInvited.contains(p.key),
                        onToggle = { onToggle(p) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteeRow(person: Invitee, selected: Boolean, already: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !already) { onToggle() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(if (selected) Teal else if (already) IconGreen.copy(alpha = 0.15f) else Mist),
            contentAlignment = Alignment.Center,
        ) {
            when {
                selected -> Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                already -> Icon(Icons.Outlined.Check, null, tint = IconGreen, modifier = Modifier.size(18.dp))
                else -> Text(initialsOf(person.name), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    person.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
                )
                if (person.hasAccount) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "On Dealio", color = IconBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(IconBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(person.phone, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                if (!person.city.isNullOrBlank()) {
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Outlined.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(person.city!!, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
        if (already) {
            Text("Invited", color = IconGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** "3 selected" / "Who to invite" — the label above the picker. */
fun invitePickerLabel(count: Int): String =
    if (count == 0) "Who to invite" else "Who to invite · $count selected"

@Composable
fun SelectedCountBar(count: Int, modifier: Modifier = Modifier) {
    if (count == 0) return
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Teal.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Check, null, tint = Teal, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "$count ${if (count == 1) "person" else "people"} will get this invite",
            color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        )
    }
}
