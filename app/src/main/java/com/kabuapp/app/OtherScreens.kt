package com.kabuapp.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HoldingsList(
    data: AppData,
    holdings: List<String>,
    banners: List<String>,
    selected: String?,
    onOpen: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    var removing by remember { mutableStateOf<String?>(null) }
    val topCodes = data.weekly.items.map { it.code }.toSet()
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    T("毎朝6時ごろ更新" + if (data.scores.asOf.isNotEmpty()) "・${shortDate(data.scores.asOf)}" else "", 13.sp, Sub)
                    T("持ち株", 26.sp, bold = true, modifier = Modifier.semantics { heading() })
                }
                OutlineButton("＋ 追加", onAdd)
            }
        }
        items(banners) { SystemBanner(it) }
        if (holdings.isEmpty()) item {
            Column(
                Modifier.fillMaxWidth().background(Surface, CardShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                T("持ち株はまだありません", 16.sp, bold = true)
                T("銘柄名だけ登録すると、その会社のニュース・公式発表・計算の点が毎朝届きます。株数や金額は入れません。", 14.sp, lineHeight = 24.sp)
                FilledButton("持ち株を追加", onAdd)
            }
        }
        items(holdings, key = { it }) { code ->
            val stock = data.scores.stocks[code]
            val latest = stock?.disclosures?.firstOrNull()?.let { "${shortDate(it.date)} 公式発表：${it.title}" }
                ?: stock?.news?.firstOrNull()?.let { "メディア${it.count}：${it.title}" }
                ?: "新しい知らせはなし"
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(TileFill, TileShape)
                    .then(if (selected == code) Modifier.border(2.dp, Ink, TileShape) else Modifier)
                    .combinedClickable(onClick = { onOpen(code) }, onLongClick = { removing = code })
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    T(stock?.name ?: code, 16.sp, bold = true, modifier = Modifier.weight(1f))
                    if (code in topCodes) Badge("話題10入り")
                    stock?.changePct?.let {
                        T("前日比", 12.sp, SubOnTile)
                        N(change(it), 16.sp)
                    }
                }
                if (stock == null) {
                    T("この銘柄のデータがまだありません", 13.sp, SubOnTile)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
                        T(code, 12.sp, SubOnTile)
                        Row(verticalAlignment = Alignment.Bottom) {
                            T("数字の点 ", 12.sp, SubOnTile)
                            if (stock.fundamental != null) N("${stock.fundamental}", 15.sp) else T("データ不足", 12.sp, bold = true)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            T("投資家 ", 12.sp, SubOnTile)
                            if (stock.investor != null) N("${stock.investor}", 15.sp) else T("動きなし", 12.sp, bold = true)
                        }
                    }
                    if (stock.bad) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BadNewsTag()
                            T(latest, 13.sp, lineHeight = 20.sp)
                        }
                    } else {
                        T(latest, 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
        item {
            T(
                "長押しで持ち株から外せます。7人の評価がほしいときは、Discordで「〇〇を7人で見て」と送ってね。登録するのは銘柄名だけ（株数や金額は残さない）。",
                12.sp, Sub, lineHeight = 19.sp, modifier = Modifier.padding(4.dp),
            )
        }
    }
    removing?.let { code ->
        val name = data.scores.stocks[code]?.name ?: code
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { T("持ち株から外す？", 17.sp, bold = true) },
            text = { T("「$name」を持ち株から外します。", 14.sp) },
            confirmButton = { TextButton({ onRemove(code); removing = null }) { T("外す", 15.sp, bold = true) } },
            dismissButton = { TextButton({ removing = null }) { T("やめる", 15.sp) } },
            containerColor = Surface,
        )
    }
}

@Composable
fun HoldingDetail(data: AppData, code: String, contentPadding: PaddingValues, onBack: (() -> Unit)?, onOpenTopic: (String) -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        if (url.isNotEmpty()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    val stock = data.scores.stocks[code]
    val topic = data.weekly.items.firstOrNull { it.code == code }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onBack != null) item { BackLink("持ち株", onBack) }
        item {
            StockTitle(
                code, stock?.sector ?: "", null, stock?.name ?: code,
                stock?.close?.let { Price(it, stock.changePct ?: 0.0, data.scores.asOf) },
            )
        }
        item {
            Column(
                Modifier.fillMaxWidth().background(Surface, CardShape).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                T("専門家7人の評価", 15.sp, bold = true)
                if (topic != null) {
                    T("今週の話題10に入っています。", 14.sp)
                    OutlineButton("話題10の評価を見る") { onOpenTopic(code) }
                } else {
                    T("7人の評価はまだありません。Discordで「${stock?.name ?: code}を7人で見て」と送ると、ここに届きます。", 14.sp, lineHeight = 24.sp)
                }
            }
        }
        if (stock != null) {
            item { SectionTitle("計算で出した点") }
            item { FundamentalCard(CalcScores(fundamental = stock.fundamental)) }
            item { InvestorCard(stock.investor, stock.investorEvents, ::open) }
            if (stock.disclosures.isNotEmpty()) {
                item { SectionTitle("会社の公式発表") }
                items(stock.disclosures) { d -> DisclosureCard(d) { open(d.url) } }
            }
            if (stock.news.isNotEmpty()) {
                item { SectionTitle("ニュース") }
                items(stock.news) { n -> NewsCard(n) { open(n.url) } }
            }
        }
    }
}

@Composable
fun AddHolding(data: AppData, holdings: List<String>, onAdd: (String) -> Unit, onBack: () -> Unit, contentPadding: PaddingValues) {
    var query by remember { mutableStateOf("") }
    val hits = remember(query, data.scores) { searchStocks(query, data.scores) }
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { BackLink("持ち株", onBack) }
        item { T("持ち株を追加", 24.sp, bold = true, modifier = Modifier.semantics { heading() }) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                T("社名かコード", 13.sp, bold = true)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(Surface, TileShape)
                        .border(1.5.dp, Ink, TileShape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Ink, fontSize = 16.sp),
                        cursorBrush = SolidColor(Ink),
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "社名かコード" },
                    )
                    if (query.isEmpty()) T("例：つばさ、0004", 16.sp, Sub)
                }
                T("ひらがな・カタカナ・全角半角は区別しません", 12.sp, Sub)
            }
        }
        items(hits, key = { it.first }) { (code, stock) ->
            val added = code in holdings
            Row(
                Modifier.fillMaxWidth().background(TileFill, TileShape).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    T(stock.name, 16.sp, bold = true)
                    T("$code・${stock.sector}", 12.sp, SubOnTile)
                }
                if (added) {
                    T("登録ずみ", 13.sp, SubOnTile)
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .border(1.5.dp, Ink, RoundedCornerShape(12.dp))
                            .clickable { onAdd(code) }
                            .semantics { contentDescription = "${stock.name}を持ち株に追加" },
                        contentAlignment = Alignment.Center,
                    ) { T("＋", 20.sp, bold = true) }
                }
            }
        }
        item {
            T("登録するのは銘柄名だけ。株数や金額は入れません。持ち株のリストはこのスマホの中だけに置きます。", 12.sp, Sub, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TrackScreen(track: Track, contentPadding: PaddingValues) {
    var byPower by remember { mutableStateOf(true) }
    var threeMonths by remember { mutableStateOf(false) }
    val spans = if (byPower) track.byPower else track.byCheapness
    val bars = if (threeMonths) spans.m3 else spans.m1
    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(start = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                T("これまでの評価 ${track.rows.size}件", 13.sp, Sub)
                T("答え合わせ", 26.sp, bold = true, modifier = Modifier.semantics { heading() })
            }
        }
        item { Segmented("会社の力", "割安さ", byPower) { byPower = it } }
        item { Segmented("1か月後", "3か月後", !threeMonths) { threeMonths = !it } }
        item {
            if (bars.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().background(Surface, CardShape).padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    T(if (threeMonths) "3か月後の答えは、まだありません" else "答えは、まだありません", 15.sp, bold = true)
                    val first = track.firstM3Date.takeIf { threeMonths && it.isNotEmpty() }?.let { "最初の答えは ${dayDate(it)}に出ます。" } ?: "評価から1か月たつと、ここに並びます。"
                    T(first, 13.sp, lineHeight = 21.sp)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().background(Surface, CardShape).padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    T("点ごとの、日経平均との差（平均）", 15.sp, bold = true)
                    bars.forEach { TrackBarRow(it) }
                    T("線だけの棒は10件未満で、まだ参考になりません。評価は「上がるか」の予想ではないので、3か月以上で見るのがおすすめ。", 12.sp, Sub, lineHeight = 19.sp)
                }
            }
        }
        if (track.rows.isNotEmpty()) item { T("ひとつずつ（1か月後）", 15.sp, bold = true) }
        items(track.rows) { row ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).background(TileFill, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                N(shortDate(row.date), 14.sp, SubOnTile, Modifier.width(40.dp))
                Column(Modifier.weight(1f)) {
                    T(row.name, 14.sp, bold = true)
                    T("力${row.power}・割安さ${row.cheapness}", 12.sp, SubOnTile)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (row.m1 != null) N(signed(row.m1), 18.sp) else T("まだ", 13.sp, SubOnTile)
                    row.m1Index?.let { T("日経平均 ${signed(it)}", 12.sp, SubOnTile) }
                }
            }
        }
    }
}

@Composable
private fun TrackBarRow(bar: TrackBar) {
    val ink = Ink
    Row(
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${bar.score}点、日経平均との差 ${signed(bar.diff)}、${bar.n}件" + if (bar.n < 10) "、まだ参考になりません" else ""
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        N("${bar.score}", 18.sp, modifier = Modifier.width(12.dp))
        BoxWithConstraints(Modifier.weight(1f).height(16.dp)) {
            val zero = maxWidth * 0.4f
            val length = maxWidth * (minOf(kotlin.math.abs(bar.diff) * 9, 60.0) / 100).toFloat()
            Box(Modifier.offset(x = zero).width(1.dp).fillMaxHeight().background(Sub))
            val start = if (bar.diff >= 0) zero else zero - length
            val shape = RoundedCornerShape(3.dp)
            val m = Modifier.offset(x = start).width(length).fillMaxHeight()
            Box(if (bar.n >= 10) m.background(ink, shape) else m.border(1.5.dp, ink, shape))
        }
        N(signed(bar.diff), 15.sp, modifier = Modifier.width(56.dp))
        T("${bar.n}件", 12.sp, Sub, modifier = Modifier.width(34.dp))
    }
}

@Composable
private fun Segmented(left: String, right: String, leftOn: Boolean, onPick: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TileFill, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(left to true, right to false).forEach { (label, isLeft) ->
            val on = leftOn == isLeft
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(if (on) Surface else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onPick(isLeft) }
                    .semantics { contentDescription = label + if (on) "、選択中" else "" },
                contentAlignment = Alignment.Center,
            ) { T(label, 14.sp, bold = true) }
        }
    }
}
