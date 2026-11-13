package com.socialscope.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.zip.ZipInputStream

data class Snapshot(val date: Long, val followers: Set<String>, val following: Set<String>)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("socialscope", Context.MODE_PRIVATE) }
    private var message by mutableStateOf("برای شروع، فایل ZIP خروجی اطلاعات Instagram را وارد کن.")
    private var snapshots by mutableStateOf(listOf<Snapshot>())
    private var selectedTab by mutableIntStateOf(0)
    private var downloadUrl by mutableStateOf("")

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importExport(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        snapshots = loadSnapshots()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFFB9A6FF),
                background = androidx.compose.ui.graphics.Color(0xFF101014),
                surface = androidx.compose.ui.graphics.Color(0xFF1C1B22)
            )) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(18.dp)) {
                        Text("SocialScope", style = MaterialTheme.typography.headlineLarge)
                        Text("تحلیل‌گر محلی اطلاعات شبکه‌های اجتماعی", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(18.dp))
                        TabRow(selectedTabIndex = selectedTab) {
                            listOf("تحلیل", "دانلود", "حریم خصوصی").forEachIndexed { i, title ->
                                Tab(selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        when (selectedTab) {
                            0 -> AnalysisScreen()
                            1 -> DownloadScreen()
                            else -> PrivacyScreen()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AnalysisScreen() {
        val current = snapshots.lastOrNull()
        val previous = snapshots.getOrNull(snapshots.lastIndex - 1)
        val lost = if (current != null && previous != null) previous.followers - current.followers else emptySet()
        val gained = if (current != null && previous != null) current.followers - previous.followers else emptySet()
        val notFollowingBack = current?.following?.minus(current.followers) ?: emptySet()
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("وضعیت آخرین Snapshot", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                Text("Followers: ${current?.followers?.size ?: 0}")
                Text("Following: ${current?.following?.size ?: 0}")
                Text("تعداد Snapshotها: ${snapshots.size}")
                if (previous != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("فالوورهای ازدست‌رفته: ${lost.size}")
                    Text("فالوورهای جدید: ${gained.size}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { picker.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) },
            modifier = Modifier.fillMaxWidth()) { Text("واردکردن فایل خروجی Instagram") }
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
        if (current != null) {
            Text("دنبال می‌کنی اما در Followers نیستند (${notFollowingBack.size})", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.weight(1f)) {
                items(notFollowingBack.sorted().take(300)) { name -> Text("@$name", modifier = Modifier.padding(vertical = 4.dp)) }
            }
        } else {
            Text("برای مقایسه تغییرات، در تاریخ‌های مختلف Snapshot جدید وارد کن.")
        }
    }

    @Composable
    private fun DownloadScreen() {
        Text("دانلود فایل از لینک مستقیم HTTPS")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = downloadUrl, onValueChange = { downloadUrl = it },
            label = { Text("لینک مستقیم فایل") }, modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = { downloadDirect(downloadUrl) }, modifier = Modifier.fillMaxWidth()) {
            Text("شروع دانلود")
        }
        Text("این نسخه لینک مستقیم فایل را می‌پذیرد؛ لینک صفحه پست یا Reels به‌طور خودکار استخراج نمی‌شود.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }

    @Composable
    private fun PrivacyScreen() {
        Text("حریم خصوصی و محدودیت‌ها", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        listOf(
            "برای ورود، رمز عبور Instagram دریافت نمی‌شود.",
            "فایل خروجی روی همین دستگاه پردازش می‌شود.",
            "Snapshotها در فضای خصوصی برنامه ذخیره می‌شوند.",
            "این برنامه به اطلاعات حساب‌های Private دیگران دسترسی ندارد.",
            "مخفی‌کردن Seen، Story View یا Online و حذف تبلیغات از سمت سرور Instagram کنترل می‌شوند و در این اپ مستقل قابل تضمین نیستند."
        ).forEach { Text("• $it", modifier = Modifier.padding(vertical = 6.dp)) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { snapshots = emptyList(); prefs.edit().remove("snapshots").apply(); message = "اطلاعات محلی پاک شد." }) {
            Text("پاک‌کردن همه Snapshotها")
        }
    }

    private fun importExport(uri: Uri) {
        try {
            val entries = linkedMapOf<String, String>()
            contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "فایل قابل خواندن نیست." }
                val zip = ZipInputStream(input)
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.name.endsWith(".json", true)) {
                        val text = zip.readBytes().toString(Charsets.UTF_8)
                        entries[entry.name.substringAfterLast('/')] = text
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            val followerText = entries.entries.firstOrNull { it.key.startsWith("followers") }?.value
                ?: entries.entries.firstOrNull { it.key.contains("follower", true) }?.value
            val followingText = entries.entries.firstOrNull { it.key == "following.json" }?.value
                ?: entries.entries.firstOrNull { it.key.contains("following", true) }?.value
            require(followerText != null && followingText != null) {
                "فایل followers و following در ZIP پیدا نشد. خروجی JSON را انتخاب کن."
            }
            val followers = parseNames(followerText, false)
            val following = parseNames(followingText, true)
            require(followers.isNotEmpty() || following.isNotEmpty()) { "اطلاعات قابل‌خواندن پیدا نشد." }
            snapshots = snapshots + Snapshot(System.currentTimeMillis(), followers, following)
            saveSnapshots()
            message = "Snapshot با موفقیت وارد شد: ${followers.size} Followers و ${following.size} Following."
        } catch (e: Exception) {
            message = "خطا در واردکردن فایل: ${e.message ?: "فرمت نامعتبر"}"
        }
    }

    private fun parseNames(raw: String, followingFile: Boolean): Set<String> {
        val root = JSONArrayOrObject(raw)
        val result = linkedSetOf<String>()
        fun visit(value: Any?) {
            when (value) {
                is JSONObject -> {
                    val data = value.optJSONArray("string_list_data")
                    if (data != null) {
                        for (i in 0 until data.length()) {
                            val item = data.optJSONObject(i)
                            val name = item?.optString("value").orEmpty()
                            if (name.isNotBlank() && name != "null") result.add(name.removePrefix("@"))
                        }
                    }
                    val relationships = value.optJSONArray("relationships_following")
                    if (relationships != null) for (i in 0 until relationships.length()) visit(relationships.opt(i))
                    val keys = value.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        if (key != "relationships_following") {
                            val child = value.opt(key)
                            if (child is JSONArray || child is JSONObject) visit(child)
                        }
                    }
                }
                is JSONArray -> for (i in 0 until value.length()) visit(value.opt(i))
            }
        }
        visit(root)
        return result
    }

    private fun JSONArrayOrObject(raw: String): Any {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("[")) JSONArray(trimmed) else JSONObject(trimmed)
    }

    private fun saveSnapshots() {
        val array = JSONArray()
        snapshots.forEach { s ->
            val obj = JSONObject()
            obj.put("date", s.date)
            obj.put("followers", JSONArray(s.followers.toList()))
            obj.put("following", JSONArray(s.following.toList()))
            array.put(obj)
        }
        prefs.edit().putString("snapshots", array.toString()).apply()
    }

    private fun loadSnapshots(): List<Snapshot> = try {
        val array = JSONArray(prefs.getString("snapshots", "[]"))
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            fun names(key: String): Set<String> {
                val a = obj.getJSONArray(key)
                return (0 until a.length()).map { a.getString(it) }.toSet()
            }
            Snapshot(obj.getLong("date"), names("followers"), names("following"))
        }
    } catch (_: Exception) { emptyList() }

    private fun downloadDirect(value: String) {
        try {
            val uri = Uri.parse(value.trim())
            require(uri.scheme == "https" && !uri.host.isNullOrBlank()) { "فقط لینک معتبر HTTPS مجاز است." }
            val request = DownloadManager.Request(uri)
                .setTitle("SocialScope download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, uri.lastPathSegment ?: "socialscope-file")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
            (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            message = "دانلود به Download Manager سپرده شد."
        } catch (e: Exception) {
            message = "لینک معتبر نیست: ${e.message}"
        }
    }
}
