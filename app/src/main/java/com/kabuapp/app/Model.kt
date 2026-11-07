package com.kabuapp.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The JSON the data pipeline publishes (schema_version 1). Unknown keys are ignored, so the
// pipeline can add fields without breaking older app builds; a breaking change moves to /v2/.

@Serializable
data class DataIndex(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("generated_at") val generatedAt: String = "",
    /** Trading day the prices belong to, e.g. "2026-11-07". */
    @SerialName("as_of") val asOf: String = "",
    val sources: Map<String, SourceStatus> = emptyMap(),
    val files: IndexFiles = IndexFiles(),
)

@Serializable
data class SourceStatus(
    val ok: Boolean = true,
    @SerialName("last_ok") val lastOk: String = "",
    /** Since when it has been failing, shown in the banner. */
    @SerialName("failing_since") val failingSince: String = "",
)

@Serializable
data class IndexFiles(
    val weekly: String = "weekly/latest.json",
    val scores: String = "scores_all.json",
    val track: String = "track.json",
)

@Serializable
data class Weekly(
    val date: String = "",
    @SerialName("next_date") val nextDate: String = "",
    val items: List<TopicStock> = emptyList(),
)

@Serializable
data class TopicStock(
    val rank: Int,
    val code: String,
    val name: String,
    val sector: String,
    @SerialName("media_count") val mediaCount: Int,
    /** "新" for a newcomer, "力 2→3" when a score moved since last week, "" otherwise. */
    val badge: String = "",
    val power: Int,
    val cheapness: Int,
    val headline: String,
    val summary: String,
    @SerialName("power_from") val powerFrom: String = "目利き・決算・成長・業界・株主の5人から、まとめ役が判断",
    @SerialName("cheapness_from") val cheapnessFrom: String = "値段の番人と、PER・PBRの業種内の位置から（5が割安）",
    val weakness: Weakness,
    val experts: List<Expert>,
    val calc: CalcScores = CalcScores(),
    val price: Price? = null,
    val news: List<NewsItem> = emptyList(),
    val disclosures: List<Disclosure> = emptyList(),
    val investors: List<InvestorMove> = emptyList(),
    val prev: PrevRating? = null,
)

@Serializable
data class Weakness(
    /** 大 / 中 / 小 */
    val severity: String,
    val points: List<String>,
)

@Serializable
data class Expert(
    val role: String,
    /** "power" or "cheapness": which of the two scores this expert feeds. */
    val tag: String,
    val score: Int,
    val why: String,
)

@Serializable
data class CalcScores(
    val fundamental: Int? = null,
    val investor: Int? = null,
    @SerialName("fundamental_parts") val fundamentalParts: List<ScorePart> = emptyList(),
    /** What the ranks are compared against, e.g. "半導体12社の中での順位". */
    val basis: String = "",
)

@Serializable
data class ScorePart(val label: String, val value: String, val score: Int)

@Serializable
data class Price(
    val close: Double,
    @SerialName("change_pct") val changePct: Double,
    val date: String,
)

@Serializable
data class NewsItem(
    val count: Int,
    val date: String,
    val outlets: String,
    val title: String,
    val gist: String = "",
    val url: String = "",
)

@Serializable
data class Disclosure(
    val date: String,
    val title: String,
    val gist: String = "",
    val url: String = "",
)

@Serializable
data class InvestorMove(
    val date: String,
    val filer: String,
    val before: Double? = null,
    val after: Double,
    val url: String = "",
)

@Serializable
data class PrevRating(
    val date: String,
    val power: Int,
    val cheapness: Int,
    @SerialName("move_pct") val movePct: Double,
    @SerialName("index_pct") val indexPct: Double,
)

// Rule-based scores for every listed stock; the app picks the holdings out of it, so the
// holdings never leave the phone.
@Serializable
data class AllScores(
    @SerialName("as_of") val asOf: String = "",
    val stocks: Map<String, StockScore> = emptyMap(),
)

@Serializable
data class StockScore(
    val name: String,
    val sector: String,
    val close: Double? = null,
    @SerialName("change_pct") val changePct: Double? = null,
    val fundamental: Int? = null,
    /** null = no 5% filings recently ("動きなし"). */
    val investor: Int? = null,
    @SerialName("investor_events") val investorEvents: List<InvestorMove> = emptyList(),
    val disclosures: List<Disclosure> = emptyList(),
    val news: List<NewsItem> = emptyList(),
    /** True when the latest news is bad for the holder (forecast cut, big holder selling). */
    val bad: Boolean = false,
)

@Serializable
data class Track(
    @SerialName("by_power") val byPower: TrackSpans = TrackSpans(),
    @SerialName("by_cheapness") val byCheapness: TrackSpans = TrackSpans(),
    val rows: List<TrackRow> = emptyList(),
    /** Date the first 3-month answer comes in, for the empty state. */
    @SerialName("first_m3_date") val firstM3Date: String = "",
)

@Serializable
data class TrackSpans(val m1: List<TrackBar> = emptyList(), val m3: List<TrackBar> = emptyList())

@Serializable
data class TrackBar(
    val score: Int,
    /** Average move minus the Nikkei average over the same span, in %. */
    val diff: Double,
    val n: Int,
)

@Serializable
data class TrackRow(
    val date: String,
    val code: String,
    val name: String,
    val power: Int,
    val cheapness: Int,
    val m1: Double? = null,
    @SerialName("m1_index") val m1Index: Double? = null,
)
