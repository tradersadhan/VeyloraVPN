package com.veylora.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.OpenableColumns
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

const val SUPPORT_EMAIL = "sadhanpatar@gmail.com"
const val DEVELOPER = "TK Sadhan"
const val TAGLINE = "Your privacy. Your Freedom."

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    SERVERS("Servers", Icons.Filled.Public),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@Composable
fun VeyloraApp(vm: VpnViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val message by vm.message.collectAsStateWithLifecycle()

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    BackHandler(enabled = sub != null) { sub = null }

    when (sub) {
        "privacy" -> WebPage("Privacy Policy", "privacy.html") { sub = null }
        "terms" -> WebPage("Terms & Conditions", "terms.html") { sub = null }
        "about" -> AboutScreen { sub = null }
        else -> Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.values().forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Navy,
                                selectedTextColor = Teal,
                                indicatorColor = Teal,
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    Tab.HOME -> HomeScreen(vm)
                    Tab.SERVERS -> ServersScreen(vm)
                    Tab.SETTINGS -> SettingsScreen(vm) { sub = it }
                }
            }
        }
    }
}

private fun formatTime(total: Long): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun HomeScreen(vm: VpnViewModel) {
    val ctx = LocalContext.current
    val status by vm.status.collectAsStateWithLifecycle()
    val seconds by vm.seconds.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val server = servers.firstOrNull { it.id == selectedId }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connect()
        else vm.showMessage("VPN permission is required to connect.")
    }

    fun onPowerClick() {
        when (status) {
            VpnStatus.CONNECTED -> vm.disconnect()
            VpnStatus.CONNECTING -> Unit
            VpnStatus.DISCONNECTED -> {
                val intent = VpnService.prepare(ctx)
                if (intent == null) vm.connect() else permissionLauncher.launch(intent)
            }
        }
    }

    val ringColor = when (status) {
        VpnStatus.CONNECTED -> Teal
        VpnStatus.CONNECTING -> Color(0xFFFFC857)
        VpnStatus.DISCONNECTED -> Color(0xFF2A3656)
    }
    val statusText = when (status) {
        VpnStatus.CONNECTED -> "Protected"
        VpnStatus.CONNECTING -> "Connecting…"
        VpnStatus.DISCONNECTED -> "Not connected"
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Shield, null, tint = Teal, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text("VEYLORA VPN", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
        Text(TAGLINE, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = 0.18f))
                .clickable { onPowerClick() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(140.dp).clip(CircleShape).background(ringColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew, "Connect or disconnect",
                    tint = if (status == VpnStatus.DISCONNECTED) Color.White else Navy,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(statusText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        if (status == VpnStatus.CONNECTED) {
            Text(formatTime(seconds), color = Teal, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Spacer(Modifier.weight(1f))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Public, null, tint = Teal)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Selected server", color = Muted, fontSize = 12.sp)
                    Text(server?.name ?: "No server", fontWeight = FontWeight.SemiBold)
                }
                Text(server?.country.orEmpty(), color = Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ServersScreen(vm: VpnViewModel) {
    val ctx = LocalContext.current
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val text = runCatching {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text.isNullOrBlank()) {
                vm.showMessage("Could not read the selected file.")
            } else {
                vm.importConfig(displayName(ctx, uri), text)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("Servers", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Import WireGuard (.conf)")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(servers, key = { it.id }) { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = if (s.id == selectedId) BorderStroke(1.dp, Teal) else null,
                    modifier = Modifier.fillMaxWidth().clickable { vm.select(s.id) }
                ) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s.id == selectedId, onClick = { vm.select(s.id) })
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold)
                            Text(s.country, color = Muted, fontSize = 12.sp)
                        }
                        if (s.imported) {
                            IconButton(onClick = { vm.remove(s.id) }) {
                                Icon(Icons.Filled.Delete, "Remove", tint = Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun displayName(ctx: Context, uri: Uri): String {
    var name = "Imported server"
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && c.moveToFirst()) name = c.getString(idx)
    }
    return name.removeSuffix(".conf")
}

@Composable
private fun SettingsScreen(vm: VpnViewModel, open: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        SettingRow(Icons.Filled.VpnKey, "Always-on VPN & kill switch", "Open Android VPN settings") {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                .onFailure { vm.showMessage("Open Settings > Network > VPN manually.") }
        }
        SettingRow(Icons.Filled.PrivacyTip, "Privacy Policy", null) { open("privacy") }
        SettingRow(Icons.Filled.Description, "Terms & Conditions", null) { open("terms") }
        SettingRow(Icons.Filled.Email, "Contact support", SUPPORT_EMAIL) {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL"))
                .putExtra(Intent.EXTRA_SUBJECT, "VEYLORA VPN support")
            runCatching { ctx.startActivity(intent) }
                .onFailure { vm.showMessage("No email app found. Write to $SUPPORT_EMAIL") }
        }
        SettingRow(Icons.Filled.Info, "About", null) { open("about") }
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Teal)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { pad -> content(Modifier.padding(pad).fillMaxSize()) }
}

@Composable
private fun WebPage(title: String, asset: String, onBack: () -> Unit) {
    SubScaffold(title, onBack) { mod ->
        AndroidView(
            modifier = mod,
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url
                            if (url.scheme == "file") return false
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, url)) }
                            return true
                        }
                    }
                    loadUrl("file:///android_asset/$asset")
                }
            }
        )
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    SubScaffold("About", onBack) { mod ->
        Column(
            mod.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Shield, null, tint = Teal, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("VEYLORA VPN", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(TAGLINE, color = Muted, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(24.dp))
            Text("Version ${BuildConfigVersion.NAME}", color = Muted)
            Text("Developer: $DEVELOPER", modifier = Modifier.padding(top = 6.dp))
            Text(SUPPORT_EMAIL, color = Teal, modifier = Modifier.padding(top = 6.dp), textAlign = TextAlign.Center)
        }
    }
}

object BuildConfigVersion { const val NAME = "1.0.0" }
