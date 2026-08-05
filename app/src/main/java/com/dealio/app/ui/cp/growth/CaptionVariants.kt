package com.dealio.app.ui.cp.growth

import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.titleCase

/**
 * Caption generation for the Content Studio.
 *
 * A CP posting the same project to a family group and to a LinkedIn feed needs two
 * different posts, so we never hand back a single "the" caption. Every generate
 * produces one caption per [captionTones] entry and the CP picks the angle that fits
 * the audience they're posting to.
 *
 * Nothing here is commission-facing: these captions are written to be forwarded to
 * buyers, so the CP's commission rate and internal margins never appear in them.
 */

/** The angle a caption takes. Same facts, different audience. */
data class CaptionTone(val id: String, val label: String, val blurb: String)

/** One generated option — the tone it was written in, and the copy itself. */
data class CaptionVariant(val tone: CaptionTone, val text: String)

val captionTones = listOf(
    CaptionTone("lifestyle", "Lifestyle", "Warm and aspirational — families and end-users"),
    CaptionTone("investor", "Investor", "Facts and numbers — buyers doing the math"),
    CaptionTone("urgency", "Urgency", "Short and punchy — stories and quick posts"),
)

/**
 * Three captions for [platform], one per tone.
 *
 * [seed] rotates the hook and call-to-action pools, so "Regenerate" gives genuinely
 * different copy rather than the same sentence with a different emoji.
 */
fun captionVariants(project: Project, platform: String, seed: Int): List<CaptionVariant> {
    val f = Facts(project)
    return captionTones.map { tone -> CaptionVariant(tone, compose(f, platform, tone.id, seed)) }
}

/** Compact INR — ₹1.2 Cr / ₹85 L. Shared by the caption and flyer builders. */
internal fun compactPrice(n: Double): String = when {
    n >= 1_00_00_000 -> "₹${"%.2f".format(n / 1_00_00_000).trimEnd('0').trimEnd('.')} Cr"
    n >= 1_00_000 -> "₹${(n / 1_00_000).toInt()} L"
    else -> "₹${n.toLong()}"
}

// ─── Facts ───────────────────────────────────────────────────────────────────

/** Every project field a caption might mention, pre-cleaned so blanks read as absent. */
private class Facts(p: Project) {
    val name: String = p.name.ifBlank { "This project" }
    val builder: String? = p.builderName?.takeIf { it.isNotBlank() }
    val city: String? = p.city?.takeIf { it.isNotBlank() }
    val where: String = listOfNotNull(p.locality?.takeIf { it.isNotBlank() }, city)
        .joinToString(", ").ifBlank { "" }
    val configs: String? = p.configurations?.filter { it.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }?.joinToString(" / ")
    val price: String? = p.priceLow()?.let { compactPrice(it) }
    val amenities: String? = p.amenities?.filter { it.isNotBlank() }
        ?.takeIf { it.isNotEmpty() }?.take(3)?.joinToString(" · ")
    val possession: String? = p.possessionDate?.takeIf { it.isNotBlank() }?.let { formatDate(it) }
    val status: String? = p.status?.takeIf { it.isNotBlank() }?.let { titleCase(it) }
    val rera: String? = p.reraNumber?.takeIf { it.isNotBlank() }
    val tag: String = (city ?: "India").filter { it.isLetterOrDigit() }
}

private fun pick(pool: List<String>, seed: Int): String =
    pool[((seed % pool.size) + pool.size) % pool.size]

/** Drops the lines whose underlying field was missing, so a sparse project still reads well. */
private fun lines(vararg parts: String?): String =
    parts.filterNotNull().joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()

// ─── Hook and CTA pools ──────────────────────────────────────────────────────

private fun lifestyleHook(f: Facts, seed: Int) = pick(
    listOf(
        "Space to grow into — not out of.",
        "The kind of address you stop explaining and just say the name of.",
        "Mornings by the clubhouse, evenings home before the traffic starts.",
        "A home your family grows into over the next twenty years.",
    ),
    seed,
)

private fun investorHook(f: Facts, seed: Int) = pick(
    listOf(
        "Entry pricing today in a corridor that is still being built out.",
        "Clean title, a developer with a delivery record, and a realistic payment plan.",
        "The numbers on this one hold up — worth five minutes of your time.",
        "Rental demand in ${f.city ?: "the micro-market"} is running ahead of new supply.",
    ),
    seed,
)

private fun urgencyHook(f: Facts, seed: Int) = pick(
    listOf(
        "The best inventory always goes in the first few weeks.",
        "A handful of units left in the launch price band.",
        "Prices revise as the tower fills up — this is the lowest it gets.",
        "Two floors released this week. They will not last the month.",
    ),
    seed,
)

// ─── Platform composers ──────────────────────────────────────────────────────

private fun compose(f: Facts, platform: String, tone: String, seed: Int): String = when (platform) {
    "instagram" -> instagram(f, tone, seed)
    "facebook" -> facebook(f, tone, seed)
    "linkedin" -> linkedin(f, tone, seed)
    else -> whatsapp(f, tone, seed)
}

private fun whatsapp(f: Facts, tone: String, seed: Int): String = when (tone) {
    "investor" -> lines(
        "📊 *${f.name}* — the numbers",
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        f.price?.let { "• Entry from $it" },
        f.configs?.let { "• Configurations: $it" },
        f.possession?.let { "• Possession: $it" },
        f.status?.let { "• Status: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        investorHook(f, seed),
        "",
        pick(
            listOf(
                "Reply here and I'll send the price sheet, the payment plan and a rental comparison for the area.",
                "Want the full investment note? Reply and I'll share it along with the payment schedule.",
                "I can put together a cost sheet with registration and GST included — just say the word.",
            ),
            seed,
        ),
    )

    "urgency" -> lines(
        "⚡ *${f.name}*",
        listOfNotNull(f.configs, f.price?.let { "from $it" }).joinToString(" · ").takeIf { it.isNotBlank() },
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        urgencyHook(f, seed),
        "",
        pick(
            listOf(
                "Reply today and I'll hold a shortlist of units for you.",
                "Say the word and I'll check what is still open on the good floors.",
                "Free this weekend? I can block a site visit slot before the release closes.",
            ),
            seed,
        ),
    )

    else -> lines(
        "🏡 *${f.name}*${f.builder?.let { " by $it" } ?: ""}",
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        lifestyleHook(f, seed),
        "",
        f.configs?.let { "🛏 $it" },
        f.price?.let { "💰 Starting $it" },
        f.possession?.let { "🗓 Possession $it" },
        f.amenities?.let { "✨ $it" },
        "",
        pick(
            listOf(
                "Reply here and I'll send the floor plans and the latest price sheet.",
                "Shall I send across the brochure and the payment plan?",
                "Free to visit this weekend? I can block a site visit slot for you.",
            ),
            seed,
        ),
    )
}

private fun instagram(f: Facts, tone: String, seed: Int): String {
    val tags = when (tone) {
        "investor" -> "#RealEstateInvestment #${f.tag} #PropertyInvestment #RentalYield #RealEstateIndia"
        "urgency" -> "#${f.tag}Property #NewLaunch #LimitedUnits #RealEstateIndia"
        else -> "#${f.tag}RealEstate #${f.tag}Homes #NewLaunch #DreamHome #RealEstateIndia"
    }
    return when (tone) {
        "investor" -> lines(
            "📈 ${investorHook(f, seed)}",
            "",
            f.name,
            f.where.takeIf { it.isNotBlank() },
            "",
            f.price?.let { "• Ticket size from $it" },
            f.configs?.let { "• $it" },
            f.possession?.let { "• Possession $it" },
            f.rera?.let { "• RERA $it" },
            "",
            "DM \"NUMBERS\" and I'll send the price sheet and payment plan 📊",
            "",
            tags,
        )

        "urgency" -> lines(
            "⚡ ${urgencyHook(f, seed)}",
            "",
            "${f.name}${f.where.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""}",
            listOfNotNull(f.price?.let { "From $it" }, f.configs).joinToString(" · ").takeIf { it.isNotBlank() },
            "",
            "DM before the next price revision 🔑",
            "",
            tags,
        )

        else -> lines(
            "✨ ${lifestyleHook(f, seed)}",
            "",
            "🏡 ${f.name}${f.configs?.let { " — $it" } ?: ""}",
            f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
            f.price?.let { "💰 Starting $it" },
            f.amenities?.let { "\n✨ $it" },
            "",
            "DM \"HOME\" and I'll send the full details 🔑",
            "",
            tags,
        )
    }
}

private fun facebook(f: Facts, tone: String, seed: Int): String = when (tone) {
    "investor" -> lines(
        "📊 Investment snapshot — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}",
        "",
        investorHook(f, seed),
        "",
        f.price?.let { "💰 Entry from $it" },
        f.configs?.let { "🏗️ $it" },
        f.possession?.let { "🗓️ Possession $it" },
        f.builder?.let { "🏢 Developed by $it" },
        f.rera?.let { "✅ RERA $it" },
        "",
        "Message me for the full investment note — price sheet, payment plan and comparables.",
        "",
        "#RealEstateInvestment #${f.tag}",
    )

    "urgency" -> lines(
        "⚡ ${f.name} — ${urgencyHook(f, seed)}",
        "",
        listOfNotNull(
            f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
            f.price?.let { "💰 From $it" },
            f.configs?.let { "🏗️ $it" },
        ).joinToString("\n").takeIf { it.isNotBlank() },
        "",
        "Comment \"SEND\" and I'll share the details before this release closes.",
        "",
        "#${f.tag}Homes #NewLaunch",
    )

    else -> lines(
        "🏠 Introducing ${f.name}${f.builder?.let { " by $it" } ?: ""}",
        "",
        lifestyleHook(f, seed),
        "",
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        f.configs?.let { "🏗️ $it" },
        f.price?.let { "💰 Starting $it" },
        f.amenities?.let { "✨ $it" },
        f.possession?.let { "🗓️ Possession $it" },
        "",
        "Comment \"INTERESTED\" or send me a message and I'll share the brochure and floor plans.",
        "",
        "#${f.tag}Homes #RealEstate",
    )
}

private fun linkedin(f: Facts, tone: String, seed: Int): String = when (tone) {
    "investor" -> lines(
        "Investment note — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}.",
        "",
        investorHook(f, seed),
        "",
        f.price?.let { "• Entry ticket: $it" },
        f.configs?.let { "• Configurations: $it" },
        f.possession?.let { "• Possession: $it" },
        f.builder?.let { "• Developer: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        "Happy to share the price sheet, payment plan and micro-market comparables with anyone evaluating the corridor.",
        "",
        "#RealEstateInvestment #${f.tag} #PropertyMarket",
    )

    "urgency" -> lines(
        "Availability update — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}.",
        "",
        urgencyHook(f, seed),
        "",
        listOfNotNull(f.price?.let { "Starting $it" }, f.configs).joinToString(" · ").takeIf { it.isNotBlank() },
        "",
        "If you or someone in your network is evaluating ${f.city ?: "the market"} right now, I can share the current availability.",
        "",
        "#RealEstate #${f.tag}",
    )

    else -> lines(
        "${f.name}${f.builder?.let { " by $it" } ?: ""} is now open for site visits${f.where.takeIf { it.isNotBlank() }?.let { " in $it" } ?: ""}.",
        "",
        lifestyleHook(f, seed),
        "",
        f.configs?.let { "• Configurations: $it" },
        f.price?.let { "• Starting price: $it" },
        f.possession?.let { "• Possession: $it" },
        f.status?.let { "• Status: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        "Reach out if you would like the detailed presentation or a site visit arranged.",
        "",
        "#RealEstate #${f.tag} #Housing",
    )
}
