package com.dealio.app.ui.customer.handover

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dealio.app.ui.builder.DealioCard
import com.dealio.app.ui.builder.SectionLabel
import com.dealio.app.ui.builder.SubScreenScaffold
import com.dealio.app.ui.theme.TextPrimary
import com.dealio.app.ui.theme.TextSecondary
import com.dealio.app.ui.theme.tintBrush
import com.dealio.app.ui.theme.Teal

private val typicalMilestones = listOf(
    "Occupancy Certificate (OC) received",
    "Snagging defects cleared",
    "Final payment & dues settled",
    "Registration & sale deed completed",
    "Utility connections activated",
    "Key handover scheduled",
)

@Composable
fun CustomerPossessionScreen(nav: NavController) {
    SubScreenScaffold("Possession Tracker", nav) { inner ->
        Column(
            Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Info / empty state
            DealioCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(54.dp).background(tintBrush(Teal), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.HomeWork, null, tint = Teal, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Tracking begins after handover", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your builder sets up the possession checklist once the project is ready. You'll see live progress here.",
                        color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp,
                    )
                }
            }

            // Typical milestones preview
            SectionLabel("Typical handover milestones")
            DealioCard {
                typicalMilestones.forEachIndexed { i, m ->
                    if (i > 0) Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(m, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Box(Modifier.background(androidx.compose.ui.graphics.Color(0xFFF1F4F8), RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                            Text("Pending", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
