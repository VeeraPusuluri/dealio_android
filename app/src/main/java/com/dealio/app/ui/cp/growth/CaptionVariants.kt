package com.dealio.app.ui.cp.growth

import com.dealio.app.data.api.Project
import com.dealio.app.ui.builder.formatDate
import com.dealio.app.ui.builder.priceLow
import com.dealio.app.ui.builder.titleCase

/**
 * Caption generation for the Content Studio.
 *
 * A caption is built from three inputs the CP chooses: the [OfferType] (what is actually
 * on the table), the platform (how it is written), and the tone (who it is written for).
 * The offer supplies the substance — its headline, terms and follow-up ask — while the
 * platform composer decides the shape and the tone decides the angle.
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
 * Three captions for [offer] on [platform], one per tone.
 *
 * [seed] rotates the hook, offer-term and follow-up pools, so "Regenerate" gives
 * genuinely different copy rather than the same sentence with a different emoji.
 */
fun captionVariants(project: Project, offer: OfferType, platform: String, seed: Int): List<CaptionVariant> {
    val f = Facts(project)
    return captionTones.map { tone -> CaptionVariant(tone, compose(f, offer, platform, tone.id, seed)) }
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

// ─── Offer block ─────────────────────────────────────────────────────────────

/**
 * The part of the caption that is about the deal rather than the building: a badge, the
 * offer framed for this audience, and two of its terms. [bold] wraps the badge in
 * WhatsApp's asterisks.
 */
private fun offerBlock(o: OfferType, tone: String, seed: Int, bold: Boolean = false): String {
    val badge = if (bold) "*${o.badge}*" else o.badge
    val terms = List(minOf(2, o.points.size)) { "• ${pick(o.points, seed + it)}" }
    return (listOf("${o.emoji} $badge", o.angle(tone)) + terms).joinToString("\n")
}

/** What the CP is offering to send back, phrased to sit after "send you" / "share". */
private fun OfferType.ask(seed: Int): String = pick(asks, seed)

// ─── Platform composers ──────────────────────────────────────────────────────

private fun compose(f: Facts, o: OfferType, platform: String, tone: String, seed: Int): String = when (platform) {
    "instagram" -> instagram(f, o, tone, seed)
    "facebook" -> facebook(f, o, tone, seed)
    "linkedin" -> linkedin(f, o, tone, seed)
    else -> whatsapp(f, o, tone, seed)
}

private fun whatsapp(f: Facts, o: OfferType, tone: String, seed: Int): String = when (tone) {
    "investor" -> lines(
        "📊 *${f.name}* — the numbers",
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        offerBlock(o, tone, seed, bold = true),
        "",
        f.price?.let { "• Entry from $it" },
        f.configs?.let { "• Configurations: $it" },
        f.possession?.let { "• Possession: $it" },
        f.status?.let { "• Status: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        investorHook(f, seed),
        "",
        "Reply here and I'll send you ${o.ask(seed)}.",
    )

    "urgency" -> lines(
        "⚡ *${f.name}*",
        listOfNotNull(f.configs, f.price?.let { "from $it" }).joinToString(" · ").takeIf { it.isNotBlank() },
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        offerBlock(o, tone, seed, bold = true),
        "",
        urgencyHook(f, seed),
        "",
        "Reply today and I'll send you ${o.ask(seed)}.",
    )

    else -> lines(
        "🏡 *${f.name}*${f.builder?.let { " by $it" } ?: ""}",
        f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
        "",
        lifestyleHook(f, seed),
        "",
        offerBlock(o, tone, seed, bold = true),
        "",
        f.configs?.let { "🛏 $it" },
        f.price?.let { "💰 Starting $it" },
        f.possession?.let { "🗓 Possession $it" },
        f.amenities?.let { "✨ $it" },
        "",
        "Reply here and I'll send you ${o.ask(seed)}.",
    )
}

private fun instagram(f: Facts, o: OfferType, tone: String, seed: Int): String {
    val tags = when (tone) {
        "investor" -> "${o.hashtag} #RealEstateInvestment #${f.tag} #PropertyInvestment #RealEstateIndia"
        "urgency" -> "${o.hashtag} #${f.tag}Property #LimitedUnits #RealEstateIndia"
        else -> "${o.hashtag} #${f.tag}RealEstate #${f.tag}Homes #DreamHome #RealEstateIndia"
    }
    val dm = "DM \"${o.keyword}\" and I'll send you ${o.ask(seed)}"
    return when (tone) {
        "investor" -> lines(
            "📈 ${investorHook(f, seed)}",
            "",
            f.name,
            f.where.takeIf { it.isNotBlank() },
            "",
            offerBlock(o, tone, seed),
            "",
            f.price?.let { "• Ticket size from $it" },
            f.configs?.let { "• $it" },
            f.possession?.let { "• Possession $it" },
            f.rera?.let { "• RERA $it" },
            "",
            "$dm 📊",
            "",
            tags,
        )

        "urgency" -> lines(
            "⚡ ${urgencyHook(f, seed)}",
            "",
            "${f.name}${f.where.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""}",
            listOfNotNull(f.price?.let { "From $it" }, f.configs).joinToString(" · ").takeIf { it.isNotBlank() },
            "",
            offerBlock(o, tone, seed),
            "",
            "$dm 🔑",
            "",
            tags,
        )

        else -> lines(
            "✨ ${lifestyleHook(f, seed)}",
            "",
            "🏡 ${f.name}${f.configs?.let { " — $it" } ?: ""}",
            f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
            f.price?.let { "💰 Starting $it" },
            f.amenities?.let { "✨ $it" },
            "",
            offerBlock(o, tone, seed),
            "",
            "$dm 🔑",
            "",
            tags,
        )
    }
}

private fun facebook(f: Facts, o: OfferType, tone: String, seed: Int): String {
    val comment = "Comment \"${o.keyword}\" or message me and I'll share ${o.ask(seed)}."
    return when (tone) {
        "investor" -> lines(
            "📊 Investment snapshot — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}",
            "",
            investorHook(f, seed),
            "",
            offerBlock(o, tone, seed),
            "",
            f.price?.let { "💰 Entry from $it" },
            f.configs?.let { "🏗️ $it" },
            f.possession?.let { "🗓️ Possession $it" },
            f.builder?.let { "🏢 Developed by $it" },
            f.rera?.let { "✅ RERA $it" },
            "",
            comment,
            "",
            "${o.hashtag} #RealEstateInvestment #${f.tag}",
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
            offerBlock(o, tone, seed),
            "",
            comment,
            "",
            "${o.hashtag} #${f.tag}Homes",
        )

        else -> lines(
            "🏠 Introducing ${f.name}${f.builder?.let { " by $it" } ?: ""}",
            "",
            lifestyleHook(f, seed),
            "",
            offerBlock(o, tone, seed),
            "",
            f.where.takeIf { it.isNotBlank() }?.let { "📍 $it" },
            f.configs?.let { "🏗️ $it" },
            f.price?.let { "💰 Starting $it" },
            f.amenities?.let { "✨ $it" },
            f.possession?.let { "🗓️ Possession $it" },
            "",
            comment,
            "",
            "${o.hashtag} #${f.tag}Homes #RealEstate",
        )
    }
}

private fun linkedin(f: Facts, o: OfferType, tone: String, seed: Int): String = when (tone) {
    "investor" -> lines(
        "Investment note — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}.",
        "",
        investorHook(f, seed),
        "",
        offerBlock(o, tone, seed),
        "",
        f.price?.let { "• Entry ticket: $it" },
        f.configs?.let { "• Configurations: $it" },
        f.possession?.let { "• Possession: $it" },
        f.builder?.let { "• Developer: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        "Happy to share ${o.ask(seed)} with anyone evaluating the corridor.",
        "",
        "${o.hashtag} #RealEstateInvestment #${f.tag} #PropertyMarket",
    )

    "urgency" -> lines(
        "Availability update — ${f.name}${f.where.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}.",
        "",
        urgencyHook(f, seed),
        "",
        offerBlock(o, tone, seed),
        "",
        listOfNotNull(f.price?.let { "Starting $it" }, f.configs).joinToString(" · ").takeIf { it.isNotBlank() },
        "",
        "If you or someone in your network is evaluating ${f.city ?: "the market"} right now, I can send across ${o.ask(seed)}.",
        "",
        "${o.hashtag} #RealEstate #${f.tag}",
    )

    else -> lines(
        "${f.name}${f.builder?.let { " by $it" } ?: ""} is now open for site visits${f.where.takeIf { it.isNotBlank() }?.let { " in $it" } ?: ""}.",
        "",
        lifestyleHook(f, seed),
        "",
        offerBlock(o, tone, seed),
        "",
        f.configs?.let { "• Configurations: $it" },
        f.price?.let { "• Starting price: $it" },
        f.possession?.let { "• Possession: $it" },
        f.status?.let { "• Status: $it" },
        f.rera?.let { "• RERA: $it" },
        "",
        "Reach out if you would like ${o.ask(seed)} or a site visit arranged.",
        "",
        "${o.hashtag} #RealEstate #${f.tag} #Housing",
    )
}
