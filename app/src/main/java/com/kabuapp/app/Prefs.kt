package com.kabuapp.app

import android.content.Context
import org.json.JSONArray
import java.text.Normalizer

/** Holdings are stock codes only (no share counts or amounts) and never leave the phone. */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("kabu", Context.MODE_PRIVATE)

    var holdings: List<String>
        get() = runCatching {
            val array = JSONArray(prefs.getString("holdings", "[]"))
            List(array.length()) { array.getString(it) }
        }.getOrDefault(emptyList())
        set(value) = prefs.edit().putString("holdings", JSONArray(value).toString()).apply()

    /** The one-time "reference only, not a recommendation" card. */
    var disclaimerSeen: Boolean
        get() = prefs.getBoolean("disclaimer_seen", false)
        set(value) = prefs.edit().putBoolean("disclaimer_seen", value).apply()
}

/** Folds width, case and hiragana/katakana so 「つば」 finds ツバメ and ｔｏｙｏ finds TOYO. */
fun foldForSearch(text: String): String {
    val nfkc = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase()
    val out = StringBuilder(nfkc.length)
    for (c in nfkc) {
        out.append(if (c in 'ぁ'..'ゖ') c + ('ァ' - 'ぁ') else c)
    }
    return out.toString().replace(" ", "").replace("　", "")
}

fun searchStocks(query: String, scores: AllScores, limit: Int = 30): List<Pair<String, StockScore>> {
    val q = foldForSearch(query)
    if (q.isEmpty()) return emptyList()
    return scores.stocks.entries
        .mapNotNull { (code, stock) ->
            val name = foldForSearch(stock.name)
            val rank = when {
                code.startsWith(q) -> 0
                name.startsWith(q) -> 1
                name.contains(q) -> 2
                else -> return@mapNotNull null
            }
            Triple(rank, code, stock)
        }
        .sortedWith(compareBy({ it.first }, { it.second }))
        .take(limit)
        .map { it.second to it.third }
}
