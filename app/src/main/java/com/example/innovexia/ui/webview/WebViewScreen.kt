package com.example.innovexia.ui.webview

import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Modern in-app WebView browser with premium UI
 * Full-screen immersive experience with smooth animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    // Animated entry
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "webview_alpha"
    )

    // Handle back button
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedAlpha },
            color = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar with modern design
                Surface(
                    color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFFAFAFA),
                    tonalElevation = 4.dp,
                    shadowElevation = if (isLoading) 0.dp else 2.dp
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Column(modifier = Modifier.padding(end = 8.dp)) {
                                    Text(
                                        text = pageTitle.ifEmpty { "Loading..." },
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentUrl,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFB7C0CC) else Color(0xFF86868B)
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = if (isDark) Color(0xFFECEFF4) else Color(0xFF1C1C1E)
                                    )
                                }
                            },
                            actions = {
                                // Back button
                                IconButton(
                                    onClick = { webView?.goBack() },
                                    enabled = canGoBack
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = if (canGoBack) {
                                            InnovexiaColors.BlueAccent
                                        } else {
                                            if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                                        }
                                    )
                                }

                                // Forward button
                                IconButton(
                                    onClick = { webView?.goForward() },
                                    enabled = canGoForward
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowForward,
                                        contentDescription = "Forward",
                                        tint = if (canGoForward) {
                                            InnovexiaColors.BlueAccent
                                        } else {
                                            if (isDark) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)
                                        }
                                    )
                                }

                                // Refresh button
                                IconButton(onClick = { webView?.reload() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Refresh",
                                        tint = InnovexiaColors.BlueAccent
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )

                        // Loading progress bar
                        AnimatedVisibility(
                            visible = isLoading,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LinearProgressIndicator(
                                progress = { loadProgress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = InnovexiaColors.BlueAccent,
                                trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
                            )
                        }
                    }
                }

                // WebView content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        loadProgress = 0
                                        currentUrl = url ?: ""
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        loadProgress = 100
                                        pageTitle = view?.title ?: ""
                                        canGoBack = view?.canGoBack() ?: false
                                        canGoForward = view?.canGoForward() ?: false
                                    }

                                    override fun onLoadResource(view: WebView?, url: String?) {
                                        super.onLoadResource(view, url)
                                        // Update progress
                                        if (loadProgress < 90) {
                                            loadProgress += 5
                                        }
                                    }
                                }

                                webChromeClient = object : android.webkit.WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        loadProgress = newProgress
                                        if (newProgress == 100) {
                                            isLoading = false
                                        }
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        pageTitle = title ?: ""
                                    }
                                }

                                loadUrl(url)
                                webView = this
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view ->
                            if (view.url != url) {
                                view.loadUrl(url)
                            }
                        }
                    )

                    // Loading shimmer overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isLoading && loadProgress < 20,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isDark) Color(0xFF1C1C1E) else Color(0xFFFAFAFA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = InnovexiaColors.BlueAccent,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "Loading page...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isDark) Color(0xFFB7C0CC) else Color(0xFF86868B)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}
