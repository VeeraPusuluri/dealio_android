package com.dealio.app.ui.builder.documents

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.dealio.app.data.api.ProjectDocument
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.LoadingState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.builder.resolveUrl
import com.dealio.app.ui.builder.tools.BuilderToolsViewModel
import com.dealio.app.ui.theme.Teal
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush

private fun docTypeColor(t: String): Pair<Color, Color> {
    val s = t.lowercase()
    return when {
        s.contains("rera") || s.contains("legal") -> Color(0xFF047857) to Color(0xFFD1FAE5)
        s.contains("deed") || s.contains("agreement") -> Color(0xFF1D4ED8) to Color(0xFFDBEAFE)
        s.contains("plan") || s.contains("floor") -> Color(0xFF7C3AED) to Color(0xFFF1ECFD)
        s.contains("brochure") -> Color(0xFFC2410C) to Color(0xFFFFEDD5)
        else -> TextSecondary to Color(0xFFF1F4F8)
    }
}

private fun openDoc(ctx: Context, url: String) {
    val full = resolveUrl(url) ?: url
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(full)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun BuilderDocumentsScreen(nav: NavController, vm: BuilderToolsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var selectedId by remember { mutableStateOf(-1L) }

    SubScreenScaffold("Document Vault", nav) { inner ->
        if (state.loading) { LoadingState(Modifier.padding(inner)); return@SubScreenScaffold }
        val project = state.projects.firstOrNull { it.id == selectedId } ?: state.projects.firstOrNull()

        if (state.projects.isEmpty()) {
            Box(Modifier.padding(inner).padding(16.dp)) {
                DealioCard { EmptyState(Icons.Outlined.Description, "No projects yet", "Create a project to manage its documents.") }
            }
            return@SubScreenScaffold
        }

        val docs by produceState(initialValue = emptyList<ProjectDocument>(), project?.id) {
            value = project?.id?.let { vm.documents(it) } ?: emptyList()
        }

        LazyColumn(
            modifier = Modifier.padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
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
            }
            item {
                Text(
                    "${docs.size} ${if (docs.size == 1) "document" else "documents"} · ${project?.name ?: ""}",
                    color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
            }
            if (docs.isEmpty()) {
                item {
                    DealioCard { EmptyState(Icons.Outlined.Description, "No documents yet", "RERA certificates, title deeds, floor plans and brochures uploaded on the web appear here.") }
                }
            } else {
                items(docs.size) { i ->
                    val d = docs[i]
                    val (fg, bg) = docTypeColor(d.docType)
                    DealioCard(onClick = { openDoc(ctx, d.url) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).background(tintBrush(Teal), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Description, null, tint = Teal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.name.ifBlank { "Document ${d.id}" }, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.background(bg, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                    Text(d.docType, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Icon(Icons.Outlined.OpenInNew, "Open", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
