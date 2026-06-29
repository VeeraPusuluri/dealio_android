package com.dealio.app.ui.builder.snagging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.EmptyState
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary

@Composable
fun BuilderSnaggingScreen(nav: NavController) {
    SubScreenScaffold("Snagging", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Total", "0", TextPrimary, Modifier.weight(1f))
                Stat("Resolved", "0", Color(0xFF047857), Modifier.weight(1f))
                Stat("Pending", "0", Color(0xFFB45309), Modifier.weight(1f))
                Stat("Overdue", "0", Color(0xFFB91C1C), Modifier.weight(1f))
            }
            DealioCard {
                EmptyState(
                    Icons.Outlined.Handyman,
                    "No snags reported yet",
                    "Defects reported by customers after possession appear here for you to assign, track and resolve.",
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, accent: Color, modifier: Modifier) {
    DealioCard(modifier = modifier, contentPadding = 12.dp) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
