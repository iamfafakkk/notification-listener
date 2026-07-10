package com.karuhundeveloper.notification.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.karuhundeveloper.notification.data.local.entity.InterceptHistory
import com.karuhundeveloper.notification.ui.theme.*
import com.karuhundeveloper.notification.ui.viewmodel.RulesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    ruleId: Int,
    ruleName: String,
    onNavigateBack: () -> Unit,
    viewModel: RulesViewModel = viewModel()
) {
    val history by viewModel.getHistoryForRule(ruleId).collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        containerColor = NavyDeep,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("History", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(ruleName, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistoryForRule(ruleId) }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = ErrorRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDeep)
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            EmptyHistoryPlaceholder(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemCard(item)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: InterceptHistory) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
    val dateStr = sdf.format(Date(item.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(isSuccess = item.isSuccess)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (item.isSuccess) "Success" else "Failed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isSuccess) CyanAccent else ErrorRed
                )
                Spacer(Modifier.weight(1f))
                Text(dateStr, fontSize = 10.sp, color = TextSecondary)
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text("Notification:", fontSize = 10.sp, color = IndigoLight, fontWeight = FontWeight.Bold)
            Text(
                "${item.appName}: ${item.notificationTitle}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(item.notificationText, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)

            if (!item.isSuccess || item.errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = Divider, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Error: ${item.errorMessage ?: "Status ${item.httpStatusCode}"}",
                        fontSize = 11.sp,
                        color = ErrorRed
                    )
                }
            } else if (item.httpStatusCode != null) {
                Spacer(Modifier.height(4.dp))
                Text("HTTP ${item.httpStatusCode}", fontSize = 10.sp, color = CyanAccent.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatusDot(isSuccess: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isSuccess) CyanAccent else ErrorRed)
    )
}

@Composable
private fun EmptyHistoryPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("No history for this rule", color = TextSecondary, fontSize = 14.sp)
    }
}
