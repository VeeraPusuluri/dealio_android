package com.dealio.app.ui.builder.virtualtours

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.tools.BuilderToolsViewModel
import com.dealio.app.ui.theme.NavyMid
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import org.json.JSONArray

private data class Tour(val label: String, val url: String)

private fun parseTours(videoUrl: String?): List<Tour> {
    if (videoUrl.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(videoUrl)
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Tour(o.optString("label", "Tour"), o.optString("url"))
        }
    }.getOrElse { listOf(Tour("Project Video", videoUrl)) }
}

private fun youtubeThumb(url: String): String? {
    val m = Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/shorts/)([^&?/\\s]+)").find(url)
    return m?.groupValues?.get(1)?.let { "https://img.youtube.com/vi/$it/mqdefault.jpg" }
}

private fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun VirtualToursScreen(nav: NavController, vm: BuilderToolsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var selectedId by remember { mutableStateOf(-1L) }

    SubScreenScaffold("Virtual Tours", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val project = state.projects.firstOrNull { it.id == selectedId } ?: state.projects.firstOrNull()
        val tours = parseTours(project?.videoUrl)

        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.projects.isEmpty()) {
                DealioCard { EmptyState(Icons.Outlined.OndemandVideo, "No projects yet", "Create a project to add virtual tours.") }
                return@Column
            }

            // Project chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.projects.size) { i ->
                    val p = state.projects[i]
                    val sel = p.id == project?.id
                    Box(
                        Modifier.background(if (sel) Teal else Color.White, RoundedCornerShape(10.dp))
                            .clickable { selectedId = p.id }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text(p.name, color = if (sel) Color.White else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            }

            if (tours.isEmpty()) {
                DealioCard { EmptyState(Icons.Outlined.OndemandVideo, "No tours for this project", "Tours and walkthrough videos added on the web appear here.") }
            } else {
                SectionLabel("${tours.size} ${if (tours.size == 1) "tour" else "tours"}")
                tours.forEach { t ->
                    DealioCard(onClick = { openUrl(ctx, t.url) }, contentPadding = 0.dp) {
                        Box(Modifier.fillMaxWidth().height(180.dp).background(NavyMid), contentAlignment = Alignment.Center) {
                            val thumb = youtubeThumb(t.url)
                            if (thumb != null) AsyncImage(thumb, t.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            Icon(Icons.Outlined.PlayCircle, "Play", tint = Color.White, modifier = Modifier.size(56.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("Open", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
