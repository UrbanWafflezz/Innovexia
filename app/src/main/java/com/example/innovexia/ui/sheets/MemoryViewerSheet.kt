package com.example.innovexia.ui.sheets

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.ai.MemorySummarizer
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryViewerSheet(
    chatId: String,
    onDismiss: () -> Unit,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val chatDao = database.chatDao()
    val messageDao = database.messageDao()

    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var summary by remember { mutableStateOf("") }
    var summaryUpdatedAt by remember { mutableLongStateOf(0L) }
    var firstMessage by remember { mutableStateOf<String?>(null) }
    var totalMessages by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Load data
    LaunchedEffect(chatId) {
        val chat = chatDao.getById(chatId)
        summary = chat?.summary ?: ""
        summaryUpdatedAt = chat?.summaryUpdatedAt ?: 0L

        val firstMsg = messageDao.firstMessage(chatId)
        firstMessage = firstMsg?.text

        val allMessages = messageDao.forChatSync(chatId)
        totalMessages = allMessages.size
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        modifier = modifier,
        containerColor = if (darkTheme) Color(0xFF141A22) else Color.White,
        dragHandle = null,
        properties = ModalBottomSheetDefaults.properties(
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memory",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Timeline section
                    SectionCard(darkTheme = darkTheme) {
                        Text(
                            text = "Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow("Total messages", totalMessages.toString(), darkTheme)
                        if (summaryUpdatedAt > 0) {
                            InfoRow(
                                "Last summary update",
                                dateFormat.format(Date(summaryUpdatedAt)),
                                darkTheme
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // First message section
                    if (!firstMessage.isNullOrBlank()) {
                        SectionCard(darkTheme = darkTheme) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "First Message",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(firstMessage!!))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied to clipboard")
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy",
                                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Text(
                                text = firstMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Rolling summary section
                    SectionCard(darkTheme = darkTheme) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rolling Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                            )
                            IconButton(
                                onClick = {
                                    if (summary.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(summary))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Copied to clipboard")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (summary.isNotBlank()) summary else "No summary yet. Memory will be built as you chat.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.isNotBlank()) {
                                if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                            } else {
                                (if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText).copy(alpha = 0.5f)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refresh button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                try {
                                    val summarizer = MemorySummarizer(database, com.example.innovexia.data.ai.MemoryAssembler(database))
                                    val newSummary = summarizer.forceRefreshSummary(chatId)
                                    summary = newSummary
                                    summaryUpdatedAt = System.currentTimeMillis()
                                    snackbarHostState.showSnackbar("Memory refreshed")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to refresh: ${e.message}")
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !isRefreshing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Refresh")
                    }

                    // Clear button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val summarizer = MemorySummarizer(database, com.example.innovexia.data.ai.MemoryAssembler(database))
                                    summarizer.clearMemory(chatId)
                                    summary = ""
                                    summaryUpdatedAt = 0
                                    snackbarHostState.showSnackbar("Memory cleared")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to clear: ${e.message}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }

                    // Export button
                    OutlinedButton(
                        onClick = {
                            val exportText = buildString {
                                appendLine("# Memory Export")
                                appendLine()
                                appendLine("## Timeline")
                                appendLine("Total messages: $totalMessages")
                                if (summaryUpdatedAt > 0) {
                                    appendLine("Last updated: ${dateFormat.format(Date(summaryUpdatedAt))}")
                                }
                                appendLine()
                                if (!firstMessage.isNullOrBlank()) {
                                    appendLine("## First Message")
                                    appendLine(firstMessage)
                                    appendLine()
                                }
                                if (summary.isNotBlank()) {
                                    appendLine("## Summary")
                                    appendLine(summary)
                                }
                            }

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportText)
                                putExtra(Intent.EXTRA_TITLE, "Chat Memory Export")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export Memory"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Snackbar
                SnackbarHost(
                    hostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    darkTheme: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun InfoRow(label: String, value: String, darkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
        )
    }
}
