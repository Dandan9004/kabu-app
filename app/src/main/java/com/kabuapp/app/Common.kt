package com.kabuapp.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.abs

val TileShape = RoundedCornerShape(14.dp)
val CardShape = RoundedCornerShape(16.dp)

/** Text in the app's body font (the phone's Japanese font). */
@Composable
fun T(
    text: String,
    size: TextUnit = 14.sp,
    color: Color = Ink,
    bold: Boolean = false,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) = Text(
    text = text,
    modifier = modifier,
    style = TextStyle(
        color = color,
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        lineHeight = lineHeight,
    ),
    maxLines = maxLines,
    softWrap = maxLines > 1,
)

/** Numbers in DINish. */
@Composable
fun N(text: String, size: TextUnit, color: Color = Ink, modifier: Modifier = Modifier) = Text(
    text = text,
    modifier = modifier,
    style = TextStyle(color = color, fontSize = size, fontFamily = Din, fontWeight = FontWeight.SemiBold),
    maxLines = 1,
)

/**
 * A 1–5 score as five squares: filled up to the score, outlined after it. The squares are
 * decoration for TalkBack; the row around them says the score in words.
 */
@Composable
fun Squares(score: Int, size: Dp = 12.dp, onTile: Boolean = true) {
    val line = if (onTile) SubOnTile else Sub
    val ink = Ink
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.clearAndSetSemantics { }) {
        repeat(5) { i ->
            val shape = RoundedCornerShape(2.dp)
            val m = Modifier.size(size)
            Box(if (i < score) m.background(ink, shape) else m.border(1.5.dp, line, shape))
        }
    }
}

/** Outlined label like 「新」「力 2→3」「話題10入り」. */
@Composable
fun Badge(text: String, color: Color = Ink) {
    Box(
        Modifier
            .border(1.5.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) { T(text, 12.sp, color, bold = true) }
}

/** System trouble (a feed stopped, old data). Navy triangle, never the bad-news terracotta. */
@Composable
fun SystemBanner(text: String, modifier: Modifier = Modifier) {
    val ink = Ink
    Row(
        modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Canvas(Modifier.size(20.dp).padding(top = 1.dp)) {
            val w = size.width
            val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val p = Path().apply {
                moveTo(w * 0.5f, w * 0.16f); lineTo(w * 0.88f, w * 0.84f); lineTo(w * 0.12f, w * 0.84f); close()
            }
            drawPath(p, ink, style = stroke)
            drawLine(ink, Offset(w * 0.5f, w * 0.42f), Offset(w * 0.5f, w * 0.58f), stroke.width, StrokeCap.Round)
            drawLine(ink, Offset(w * 0.5f, w * 0.7f), Offset(w * 0.5f, w * 0.71f), stroke.width, StrokeCap.Round)
        }
        T(text, 13.sp, lineHeight = 20.sp)
    }
}

/** Bad news for something the owner holds. */
@Composable
fun BadNewsTag() {
    Box(
        Modifier
            .background(WarnOnTile, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) { T("悪い知らせ", 12.sp, Bg, bold = true) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    T(text, 17.sp, bold = true, modifier = modifier.padding(top = 4.dp))

@Composable
fun RowSpacer() = Spacer(Modifier.width(8.dp))

// Words for the scores. Neutral on purpose: nothing that reads as "buy" or "hold".
fun powerWord(score: Int) = when (score) {
    5 -> "土台がとても強い"
    4 -> "土台がしっかり"
    3 -> "ふつう"
    2 -> "弱いところがある"
    else -> "土台が弱い"
}

fun cheapnessWord(score: Int) = when (score) {
    5 -> "かなり割安"
    4 -> "やや割安"
    3 -> "ふつう"
    2 -> "やや割高"
    else -> "かなり割高"
}

fun tagWord(tag: String) = if (tag == "cheapness") "割安さ" else "力"

fun yen(value: Double): String = NumberFormat.getIntegerInstance(Locale.JAPAN).format(value.toLong())

/** ▲1.2% / ▼3.1%: arrows, not red and green. */
fun change(pct: Double): String = when {
    pct > 0 -> "▲%.1f%%".format(pct)
    pct < 0 -> "▼%.1f%%".format(abs(pct))
    else -> "±0.0%"
}

fun signed(pct: Double): String = (if (pct > 0) "+" else if (pct < 0) "−" else "±") + "%.1f%%".format(abs(pct))

/** "2026-11-08" → "11/8" */
fun shortDate(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10)); "${d.monthValue}/${d.dayOfMonth}"
}.getOrDefault(iso)

/** "2026-11-08" → "11/8（土）" */
fun dayDate(iso: String): String = runCatching {
    val d = LocalDate.parse(iso.take(10))
    "${d.monthValue}/${d.dayOfMonth}（${d.dayOfWeek.getDisplayName(DateTextStyle.NARROW, Locale.JAPAN)}）"
}.getOrDefault(iso)

@Composable
fun CenterBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier, contentAlignment = Alignment.Center) { content() }
