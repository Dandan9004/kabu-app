package com.kabuapp.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

enum class Origin {
    /** Fetched just now. */
    Live,
    /** The last fetch failed; showing what was saved before. */
    Cache,
    /** No data source set up yet: the bundled made-up data. */
    Sample,
}

data class AppData(
    val index: DataIndex,
    val weekly: Weekly,
    val scores: AllScores,
    val track: Track,
    val origin: Origin,
    /** When the shown files were downloaded, epoch millis (0 for the sample). */
    val savedAt: Long,
)

private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

class Repository(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val cacheDir = File(context.filesDir, "data").apply { mkdirs() }
    private val baseUrl = BuildConfig.DATA_BASE_URL.trimEnd('/')

    /** Downloads every file into the cache. Returns false when anything failed (the old copy stays). */
    suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
        if (baseUrl.isEmpty()) return@withContext false
        runCatching {
            val indexText = download("index.json")
            val index = json.decodeFromString<DataIndex>(indexText)
            val files = listOf(index.files.weekly, index.files.scores, index.files.track)
                .associateWith { download(it) }
            // Write only after everything arrived, so the cache never mixes two days
            files.forEach { (path, text) -> cacheFile(path).writeText(text) }
            cacheFile("index.json").writeText(indexText)
            true
        }.getOrDefault(false)
    }

    suspend fun load(fetched: Boolean): AppData = withContext(Dispatchers.IO) {
        val indexFile = cacheFile("index.json")
        val cached = if (indexFile.exists()) runCatching { fromCache(indexFile) }.getOrNull() else null
        cached?.copy(origin = if (fetched) Origin.Live else Origin.Cache) ?: sample()
    }

    private fun fromCache(indexFile: File): AppData {
        val index = json.decodeFromString<DataIndex>(indexFile.readText())
        return AppData(
            index = index,
            weekly = json.decodeFromString(cacheFile(index.files.weekly).readText()),
            scores = json.decodeFromString(cacheFile(index.files.scores).readText()),
            track = json.decodeFromString(cacheFile(index.files.track).readText()),
            origin = Origin.Cache,
            savedAt = indexFile.lastModified(),
        )
    }

    private fun sample(): AppData {
        fun asset(name: String) = context.assets.open("sample/$name").bufferedReader().use { it.readText() }
        return AppData(
            index = json.decodeFromString(asset("index.json")),
            weekly = json.decodeFromString(asset("weekly.json")),
            scores = json.decodeFromString(asset("scores_all.json")),
            track = json.decodeFromString(asset("track.json")),
            origin = Origin.Sample,
            savedAt = 0,
        )
    }

    private fun download(path: String): String {
        val request = Request.Builder().url("$baseUrl/$path").build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code} for $path" }
            return response.body.string()
        }
    }

    // Paths come from index.json; keep them inside the cache folder
    private fun cacheFile(path: String): File {
        val safe = path.replace('\\', '/').split('/').filter { it.isNotEmpty() && it != ".." }.joinToString("_")
        return File(cacheDir, safe)
    }
}
