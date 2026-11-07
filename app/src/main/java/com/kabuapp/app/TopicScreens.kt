package com.kabuapp.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopicHeader(weekly: Weekly, compact: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val next = if (weekly.nextDate.isNotEmpty()) "・次は${shortDate(weekly.nextDate)}" else ""
        T("${dayDate(weekly.date)} 朝の評価$next", if (compact) 12.sp else 13.sp, Sub)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            T("今週の話題", if (compact) 22.sp else 26.sp, bold = true, modifier = Modifier.semantics { heading() })
            N("${weekly.items.size}", if (compact) 28.sp else 34.sp)
        }
        if (!compact) T("報道したメディアが多い順・同じ業種は5つまで", 12.sp, Sub)
    }
}

/** One stock in the list; [selected] draws the outline the inner display uses. */
@Composable
fun TopicRow(item: TopicStock, selected: Boolean, onClick: () -> Unit) {
    val say = "${item.rank}位 ${item.name}、${item.sector}、メディア${item.mediaCount}、" +
        "会社の力${item.power}、割安さ${item.cheapness} ${cheapnessWord(item.cheapness)}" +
        if (item.badge.isNotEmpty()) "、${item.badge}" else ""
    Row(
        Modifier
            .fillMaxWidth()
            .background(TileFill, TileShape)
            .then(if (selected) Modifier.border(2.dp, Ink, TileShape) else Modifier)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = say }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        N("${item.rank}", 14.sp, SubOnTile, Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.Text(
                    item.name,
                    style = androidx.compose.ui.text.TextStyle(color = Ink, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.badge.isNotEmpty()) Badge(item.badge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                T(item.sector, 12.sp, SubOnTile, maxLines = 1)
                T("メディア", 12.sp, SubOnTile)
                N("${item.mediaCount}", 14.sp, modifier = Modifier.padding(start = 0.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
            ScoreLine("力", item.power)
            ScoreLine("割安さ", item.cheapness)
        }
    }
}

@Composable
private fun ScoreLine(label: String, score: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        T(label, 12.sp, SubOnTile)
        Squares(score)
        N("$score", 16.sp, modifier = Modifier.width(10.dp))
    }
}

@Composable
fun TopicList(
    weekly: Weekly,
    banners: List<String>,
    selected: String?,
    showDisclaimer: Boolean,
    onDismissDisclaimer: () -> Unit,
    onOpen: (String) -> Unit,
    onAddHolding: () -> Unit,
    compactHeader: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 16.dp),
) {
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Box(Modifier.padding(start = 4.dp, bottom = 4.dp)) { TopicHeader(weekly, compactHeader) } }
        items(banners) { SystemBanner(it) }
        if (showDisclaimer) item { DisclaimerCard(onDismissDisclaimer) }
        if (weekly.items.isEmpty()) {
            item { FirstWeekCard(weekly.nextDate, onAddHolding) }
        }
        items(weekly.items, key = { it.code }) { item ->
            TopicRow(item, selected == item.code) { onOpen(item.code) }
        }
        if (weekly.items.isNotEmpty()) item {
            T("AIと計算による参考の評価です。売り買いをすすめるものではありません。", 12.sp, Sub, lineHeight = 19.sp, modifier = Modifier.padding(4.dp))
        }
    }
}

@Composable
fun DisclaimerCard(onDismiss: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().border(1.5.dp, Ink, CardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        T("このアプリについて", 15.sp, bold = true)
        T(
            "AIと計算による参考の評価です。売り買いをすすめるものではありません。株価やニュースは無料の取り先から集めていて、遅れたり止まったりすることがあります。",
            14.sp, lineHeight = 24.sp,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            OutlineButton("わかった", onDismiss)
        }
    }
}

@Composable
private fun FirstWeekCard(nextDate: String, onAddHolding: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Surface, CardShape).padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val day = if (nextDate.isNotEmpty()) dayDate(nextDate) else "今度の土曜"
        T("最初の評価は ${day}の朝に届きます", 18.sp, bold = true, lineHeight = 27.sp)
        T("毎週土曜の朝、報道が多かった10銘柄を専門家7人が評価します。それまでに持ち株を登録しておくと、ニュースや公式発表がすぐ見られます。", 14.sp, lineHeight = 24.sp)
        FilledButton("持ち株を追加", onAddHolding)
    }
}

@Composable
fun FilledButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .background(Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { T("＋ $text", 15.sp, Surface, bold = true) }
}

@Composable
fun OutlineButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .border(1.5.dp, Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 14.sp, bold = true) }
}

/** The detail, answer first: one-line verdict, the two scores, weaknesses, then the reasons. */
@Composable
fun TopicDetail(item: TopicStock, weeklyDate: String, contentPadding: PaddingValues, onBack: (() -> Unit)?) {
    val context = LocalContext.current
    fun open(url: String) {
        if (url.isNotEmpty()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onBack != null) item { BackLink("今週の話題", onBack) }
        item { StockTitle(item.code, item.sector, item.mediaCount, item.name, item.price) }
        item {
            Column(
                Modifier.fillMaxWidth().background(Surface, CardShape).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                T("まとめ役のひとこと・${shortDate(weeklyDate)} 評価", 12.sp, Sub)
                T(item.headline, 19.sp, bold = true, lineHeight = 28.sp)
                T(item.summary, 14.sp, lineHeight = 24.sp)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScoreCard("会社の力", item.power, powerWord(item.power), item.powerFrom, Modifier.weight(1f))
                ScoreCard("割安さ", item.cheapness, cheapnessWord(item.cheapness), item.cheapnessFrom, Modifier.weight(1f))
            }
        }
        val fundamental = item.calc.fundamental
        if (fundamental != null && kotlin.math.abs(fundamental - item.power) >= 2) item {
            SystemBanner("AIと計算で見方が分かれています（会社の力 ${item.power}・数字の点 $fundamental）")
        }
        item { WeaknessCard(item.weakness) }
        item { SectionTitle("専門家${item.experts.size}人") }
        items(item.experts) { ExpertRow(it) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("計算で出した点")
                T("AIを使わず、毎日同じ式で計算" + if (item.calc.basis.isNotEmpty()) "・${item.calc.basis}" else "", 12.sp, Sub)
                FundamentalCard(item.calc)
                InvestorCard(item.calc.investor, item.investors, ::open)
            }
        }
        if (item.disclosures.isNotEmpty()) {
            item { SectionTitle("会社の公式発表") }
            items(item.disclosures) { d -> DisclosureCard(d) { open(d.url) } }
        }
        if (item.news.isNotEmpty()) {
            item { SectionTitle("話題のニュース") }
            items(item.news) { n -> NewsCard(n) { open(n.url) } }
        }
        item.prev?.let { prev -> item { PrevLine(prev) } }
    }
}

@Composable
fun BackLink(label: String, onBack: () -> Unit) {
    Row(
        Modifier.heightIn(min = 48.dp).clickable(onClick = onBack).padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        T("‹", 22.sp, Sub)
        T(label, 14.sp, Sub)
    }
}

@Composable
fun StockTitle(code: String, sector: String, mediaCount: Int?, name: String, price: Price?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        T(listOfNotNull(code, sector, mediaCount?.let { "メディア$it" }).joinToString("・"), 13.sp, Sub)
        T(name, 28.sp, bold = true, modifier = Modifier.semantics { heading() })
        if (price != null) Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            N("${yen(price.close)}円", 32.sp)
            T("前日比", 13.sp, modifier = Modifier.padding(bottom = 4.dp))
            N(change(price.changePct), 17.sp, modifier = Modifier.padding(bottom = 3.dp))
            T("${shortDate(price.date)} 終値", 12.sp, Sub, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ScoreCard(title: String, score: Int, word: String, from: String, modifier: Modifier) {
    Column(
        modifier
            .background(TileFill, CardShape)
            .semantics(mergeDescendants = true) { contentDescription = "$title、5段階の$score、$word" }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        T(title, 14.sp, bold = true)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            N("$score", 44.sp)
            Squares(score, 14.dp)
        }
        T(word, 14.sp, bold = true)
        T(from, 12.sp, SubOnTile, lineHeight = 18.sp)
    }
}

@Composable
private fun WeaknessCard(weakness: Weakness) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface, CardShape)
            .border(1.5.dp, Warn, CardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(12.dp).background(Warn, RoundedCornerShape(2.dp)))
            T("弱点（反対役）", 16.sp, bold = true, modifier = Modifier.weight(1f))
            T("重さ：${weakness.severity}", 13.sp, bold = true)
        }
        weakness.points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T("・", 14.sp)
                T(point, 14.sp, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun ExpertRow(expert: Expert) {
    val tag = tagWord(expert.tag)
    Column(
        Modifier
            .fillMaxWidth()
            .background(TileFill, TileShape)
            .semantics(mergeDescendants = true) { contentDescription = "${expert.role}、${tag}の点、5段階の${expert.score}。${expert.why}" }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            T(expert.role, 14.sp, bold = true)
            Badge(tag, SubOnTile)
            Spacer(Modifier.weight(1f))
            Squares(expert.score)
            N("${expert.score}", 18.sp, modifier = Modifier.width(12.dp))
        }
        T(expert.why, 13.sp, lineHeight = 21.sp)
    }
}

@Composable
fun FundamentalCard(calc: CalcScores) {
    Column(
        Modifier.fillMaxWidth().background(TileFill, CardShape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T(if (calc.basis.contains("金融")) "数字の点（金融の物差し）" else "数字の点", 14.sp, bold = true, modifier = Modifier.weight(1f))
            if (calc.fundamental != null) N("${calc.fundamental}", 26.sp) else T("データ不足", 13.sp, SubOnTile)
        }
        calc.fundamentalParts.forEach { part ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                T(part.label, 13.sp, modifier = Modifier.weight(1f))
                T(part.value, 12.sp, SubOnTile)
                N("${part.score}", 15.sp, modifier = Modifier.width(10.dp))
            }
        }
    }
}

@Composable
fun InvestorCard(score: Int?, moves: List<InvestorMove>, open: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(TileFill, CardShape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("投資家の動きの点", 14.sp, bold = true, modifier = Modifier.weight(1f))
            if (score != null) N("$score", 26.sp) else T("動きなし", 13.sp, bold = true)
        }
        moves.forEach { move ->
            Row(
                Modifier.then(if (move.url.isNotEmpty()) Modifier.clickable { open(move.url) } else Modifier),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                N(shortDate(move.date), 14.sp, SubOnTile, Modifier.width(40.dp))
                val change = move.before?.let { "%.1f%% → %.1f%%".format(it, move.after) } ?: "新しく %.1f%%".format(move.after)
                T("${move.filer}　$change", 13.sp, lineHeight = 20.sp)
            }
        }
        T("5%以上の届け出（大量保有報告書）から", 12.sp, SubOnTile)
    }
}

@Composable
fun DisclosureCard(d: Disclosure, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Surface, TileShape).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            N(shortDate(d.date), 14.sp, Sub)
            T(d.title, 14.sp, bold = true, lineHeight = 21.sp)
        }
        if (d.gist.isNotEmpty()) T(d.gist, 13.sp, lineHeight = 21.sp)
    }
}

@Composable
fun NewsCard(n: NewsItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Surface, TileShape).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            N("${n.count}", 14.sp)
            T("媒体が報道・${shortDate(n.date)}・${n.outlets}", 12.sp, Sub)
        }
        T(n.title, 14.sp, bold = true, lineHeight = 21.sp)
        if (n.gist.isNotEmpty()) T(n.gist, 13.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun PrevLine(prev: PrevRating) {
    Column {
        Box(Modifier.fillMaxWidth().padding(bottom = 14.dp).background(Divider).heightIn(min = 1.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                T("前回の評価（${shortDate(prev.date)}）は 力${prev.power}・割安さ${prev.cheapness}", 13.sp)
                T("そこから今日まで（日経平均 ${signed(prev.indexPct)}）", 13.sp, Sub)
            }
            N(signed(prev.movePct), 26.sp)
        }
    }
}
