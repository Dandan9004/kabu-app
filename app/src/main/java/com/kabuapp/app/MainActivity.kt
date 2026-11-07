package com.kabuapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RefreshWorker.schedule(this)
        setContent {
            val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
            CompositionLocalProvider(LocalPalette provides palette) {
                val state by vm.state.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize().background(Bg).safeDrawingPadding()) {
                    App(state, vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

@Composable
private fun App(state: UiState, vm: MainViewModel) {
    val data = state.data ?: return
    val inner = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val banners = banners(data)
    // Cover: detail → list. Inner: only the add screen sits on top of something.
    BackHandler(enabled = if (inner) state.page == Page.AddHolding else state.page != Page.List) { vm.back() }
    if (inner) InnerLayout(state, data, banners, vm) else CoverLayout(state, data, banners, vm)
}

@Composable
private fun CoverLayout(state: UiState, data: AppData, banners: List<String>, vm: MainViewModel) {
    val list = PaddingValues(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 16.dp)
    val detail = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (val page = state.page) {
                is Page.TopicDetail -> data.weekly.items.firstOrNull { it.code == page.code }
                    ?.let { TopicDetail(it, data.weekly.date, detail, onBack = { vm.back() }) }
                is Page.HoldingDetail -> HoldingDetail(data, page.code, detail, onBack = { vm.back() }, onOpenTopic = { vm.selectTab(Tab.Topics); vm.openTopic(it) })
                Page.AddHolding -> AddHolding(data, state.holdings, vm::addHolding, { vm.back() }, detail)
                Page.List -> when (state.tab) {
                    Tab.Topics -> TopicList(
                        data.weekly, banners, null, !state.disclaimerSeen, vm::dismissDisclaimer,
                        vm::openTopic, vm::openAddHolding, contentPadding = list,
                    )
                    Tab.Holdings -> HoldingsList(data, state.holdings, banners, null, vm::openHolding, vm::openAddHolding, vm::removeHolding, list)
                    Tab.Track -> TrackScreen(data.track, list)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp)) {
            Tab.entries.forEach { tab ->
                TabButton(tab, state.tab == tab, Modifier.weight(1f)) { vm.selectTab(tab) }
            }
        }
    }
}

@Composable
private fun InnerLayout(state: UiState, data: AppData, banners: List<String>, vm: MainViewModel) {
    val listPad = PaddingValues(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 16.dp)
    val detailPad = PaddingValues(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 24.dp)
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(84.dp).fillMaxHeight().padding(horizontal = 8.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Tab.entries.forEach { tab ->
                TabButton(tab, state.tab == tab, Modifier.fillMaxWidth(), rail = true) { vm.selectTab(tab) }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(Divider))
        when (state.tab) {
            Tab.Topics -> {
                val selected = state.selectedTopic ?: data.weekly.items.firstOrNull()?.code
                Box(Modifier.width(300.dp).fillMaxHeight()) {
                    TopicList(
                        data.weekly, banners, selected, !state.disclaimerSeen, vm::dismissDisclaimer,
                        vm::openTopic, vm::openAddHolding, compactHeader = true, contentPadding = listPad,
                    )
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Divider))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    data.weekly.items.firstOrNull { it.code == selected }
                        ?.let { TopicDetail(it, data.weekly.date, detailPad, onBack = null) }
                }
            }
            Tab.Holdings -> {
                val selected = state.selectedHolding?.takeIf { it in state.holdings } ?: state.holdings.firstOrNull()
                Box(Modifier.width(300.dp).fillMaxHeight()) {
                    HoldingsList(data, state.holdings, banners, selected, vm::openHolding, vm::openAddHolding, vm::removeHolding, listPad)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(Divider))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when {
                        state.page == Page.AddHolding -> AddHolding(data, state.holdings, vm::addHolding, { vm.back() }, detailPad)
                        selected != null -> HoldingDetail(data, selected, detailPad, onBack = null, onOpenTopic = { vm.selectTab(Tab.Topics); vm.openTopic(it) })
                    }
                }
            }
            Tab.Track -> Box(Modifier.weight(1f).fillMaxHeight()) {
                TrackScreen(data.track, PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp))
            }
        }
    }
}

@Composable
private fun TabButton(tab: Tab, on: Boolean, modifier: Modifier, rail: Boolean = false, onClick: () -> Unit) {
    val label = when (tab) {
        Tab.Topics -> "話題"
        Tab.Holdings -> "持ち株"
        Tab.Track -> "答え合わせ"
    }
    Column(
        modifier
            .heightIn(min = if (rail) 64.dp else 56.dp)
            .then(if (rail && on) Modifier.background(TileFill, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .semantics { selected = on; contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        TabIcon(tab, if (on) Ink else Sub)
        T(label, 12.sp, if (on) Ink else Sub, bold = on, modifier = Modifier.padding(top = 3.dp))
        if (!rail) Box(
            Modifier.padding(top = 3.dp).width(18.dp).height(3.dp)
                .background(if (on) Accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
private fun TabIcon(tab: Tab, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8f * u, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
        val path = androidx.compose.ui.graphics.Path()
        when (tab) {
            Tab.Topics -> {
                path.moveTo(4 * u, 18 * u); path.lineTo(9 * u, 12 * u); path.lineTo(13 * u, 15 * u); path.lineTo(20 * u, 6 * u)
                path.moveTo(15 * u, 6 * u); path.lineTo(20 * u, 6 * u); path.lineTo(20 * u, 11 * u)
            }
            Tab.Holdings -> {
                path.addRoundRect(androidx.compose.ui.geometry.RoundRect(4 * u, 5 * u, 20 * u, 19 * u, 2 * u, 2 * u))
                path.moveTo(4 * u, 10 * u); path.lineTo(20 * u, 10 * u)
            }
            Tab.Track -> {
                path.moveTo(5 * u, 12 * u); path.lineTo(9 * u, 16 * u); path.lineTo(19 * u, 6 * u)
                path.moveTo(5 * u, 20 * u); path.lineTo(19 * u, 20 * u)
            }
        }
        drawPath(path, color, style = stroke)
    }
}

private val sourceNames = mapOf(
    "prices" to "株価",
    "tdnet" to "会社の公式発表",
    "edinet" to "大量保有の届け出",
    "news" to "ニュース",
    "financials" to "決算の数字",
)

/** The notices above the lists: sample data, old data, a stopped feed, a late weekly run. */
fun banners(data: AppData): List<String> {
    val out = mutableListOf<String>()
    when (data.origin) {
        Origin.Sample -> out += "見本データを表示中です（データの取り先をまだつないでいません）。銘柄名と数字は架空です。"
        Origin.Cache -> if (data.savedAt > 0) {
            out += "前回の取り込み ${SimpleDateFormat("M/d H:mm", Locale.JAPAN).format(Date(data.savedAt))} のデータです"
        }
        Origin.Live -> {}
    }
    data.index.sources.filterValues { !it.ok }.forEach { (key, status) ->
        val name = sourceNames[key] ?: key
        val since = if (status.failingSince.isNotEmpty()) "（${shortDate(status.failingSince)}〜）" else ""
        out += if (key == "prices" && status.lastOk.isNotEmpty()) {
            "株価の取り込みが止まっています$since。表示中の株価は${shortDate(status.lastOk)}のものです。"
        } else {
            "${name}の取り込みが止まっています$since。"
        }
    }
    if (data.origin != Origin.Sample) {
        val weeklyDate = runCatching { LocalDate.parse(data.weekly.date.take(10)) }.getOrNull()
        if (weeklyDate != null && weeklyDate.plusDays(8).isBefore(LocalDate.now())) {
            out += "今週の評価はまだです。${shortDate(data.weekly.date)}の分を表示中。"
        }
    }
    return out
}
