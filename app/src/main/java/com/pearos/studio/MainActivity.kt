package com.pearos.studio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(12, 16, 29)
        window.navigationBarColor = android.graphics.Color.rgb(12, 16, 29)
        setContent { PearStudio() }
    }
}

private val Ink = Color(0xFF0C101D)
private val Panel = Color(0xFF151B2B)
private val Panel2 = Color(0xFF1C2538)
private val Mint = Color(0xFF8CF0C5)
private val Muted = Color(0xFF9BA8BE)
private val White = Color(0xFFF4F7FC)

@Composable
fun PearStudio() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("pearos-workspace", android.content.Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Overview") }
    var projectName by remember { mutableStateOf(preferences.getString("project_name", "My pearOS") ?: "My pearOS") }
    var code by remember { mutableStateOf(preferences.getString("source_config", defaultCode) ?: defaultCode) }
    var importedName by remember { mutableStateOf<String?>(null) }
    var showBuild by remember { mutableStateOf(false) }
    var buildReport by remember { mutableStateOf("Run a configuration check before preparing a preview.") }
    var showNew by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(projectName, code) {
        preferences.edit().putString("project_name", projectName).putString("source_config", code).apply()
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            importedName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported OS file"
            val lower = importedName.orEmpty().lowercase()
            if (listOf(".json", ".txt", ".xml", ".kt", ".md").any { lower.endsWith(it) }) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText().take(500_000) } }
                    .onSuccess { importedText -> if (!importedText.isNullOrBlank()) { code = importedText; notice = "Source imported into editor" } else notice = "File selected" }
                    .onFailure { notice = "Could not read source file" }
            } else notice = "Selected $importedName. Binary images and archives are not flashed or unpacked."
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(code) } ?: error("Could not open destination") }
                .onSuccess { notice = "Source exported successfully" }
                .onFailure { notice = "Export failed" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Mint, onPrimary = Ink,
        onBackground = White, onSurface = White, secondary = Color(0xFF9AB7FF)
    )) {
        Column(Modifier.fillMaxSize().background(Ink)) {
            TopBar(projectName, importedName)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    "Overview" -> OverviewScreen(projectName, importedName, { page = "Code editor" }, { importLauncher.launch(arrayOf("*/*")) }, { showNew = true })
                    "Code editor" -> EditorScreen(code, { code = it }, {
                        buildReport = runCatching {
                            val config = org.json.JSONObject(code)
                            "Configuration valid.\\n\\nProject: ${config.optString("name", "Unnamed OS")}\\nVersion: ${config.optString("version", "unspecified")}\\n\\nThis checks JSON only. A bootable image still needs matching device source, kernel and vendor files."
                        }.getOrElse { "Configuration check failed: " + (it.localizedMessage ?: "Invalid JSON") + ". Fix the JSON syntax and try again." }
                        showBuild = true
                    }, { saveLauncher.launch("pearos-config.json") })
                    "Appearance" -> AppearanceScreen()
                    "Files" -> FilesScreen(importedName, { importLauncher.launch(arrayOf("*/*")) })
                    "Builds" -> BuildsScreen { showBuild = true }
                    "Preview" -> PhonePreviewScreen(projectName)
                    "Templates" -> TemplatesScreen { template -> code = template; page = "Code editor"; notice = "Template loaded" }
                    else -> OverviewScreen(projectName, importedName, { page = "Code editor" }, { importLauncher.launch(arrayOf("*/*")) }, { showNew = true })
                }
            }
            PhoneNavigationBar(page = page, onPage = { page = it })
        }
        if (showBuild) {
            AlertDialog(
                onDismissRequest = { showBuild = false },
                containerColor = Panel2,
                title = { Text("Build preview", color = White, fontWeight = FontWeight.Bold) },
                text = { Text(buildReport, color = Muted) },
                confirmButton = { TextButton(onClick = { showBuild = false; page = "Builds" }) { Text("View builds") } },
                dismissButton = { TextButton(onClick = { showBuild = false }) { Text("Close") } }
            )
        }
        if (showNew) {
            AlertDialog(
                onDismissRequest = { showNew = false },
                containerColor = Panel2,
                title = { Text("Create a pearOS project", color = White) },
                text = {
                    Column {
                        Text("Give your operating system a name.", color = Muted)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = projectName, onValueChange = { projectName = it }, singleLine = true, label = { Text("Project name") })
                    }
                },
                confirmButton = { TextButton(onClick = { showNew = false; page = "Code editor"; notice = "Project created" }) { Text("Create") } },
                dismissButton = { TextButton(onClick = { showNew = false }) { Text("Cancel") } }
            )
        }
        notice?.let { message ->
            LaunchedEffect(message) { kotlinx.coroutines.delay(2400); notice = null }
            SnackbarHost(hostState = remember { SnackbarHostState() }, modifier = Modifier.padding(16.dp).wrapContentSize(Alignment.BottomCenter)) {
                Snackbar(containerColor = Panel2) { Text(message) }
            }
        }
    }
}

@Composable
private fun PhoneNavigationBar(page: String, onPage: (String) -> Unit) {
    val items = listOf(
        Triple("Overview", Icons.Rounded.Home, "Home"),
        Triple("Preview", Icons.Rounded.PhoneAndroid, "Preview"),
        Triple("Appearance", Icons.Rounded.Palette, "Style"),
        Triple("Code editor", Icons.Rounded.Code, "Code"),
        Triple("Files", Icons.Rounded.FolderOpen, "Files"),
        Triple("Builds", Icons.Rounded.RocketLaunch, "Build"),
        Triple("Templates", Icons.Rounded.DashboardCustomize, "More")
    )
    Row(Modifier.fillMaxWidth().background(Color(0xFF101625)).navigationBarsPadding().padding(horizontal = 3.dp, vertical = 7.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (key, icon, label) ->
            val selected = page == key
            Column(Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(if (selected) Color(0xFF263849) else Color.Transparent).clickable { onPage(key) }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = if (selected) Mint else Muted, modifier = Modifier.size(19.dp))
                Spacer(Modifier.height(3.dp))
                Text(label, fontSize = 8.sp, color = if (selected) Mint else Muted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TopBar(project: String, imported: String?) {
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF101625)).padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("pearOS STUDIO", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text(project, color = White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.size(9.dp).clip(CircleShape).background(Mint))
        Spacer(Modifier.width(7.dp))
        Text(if (imported == null) "Workspace ready" else "File imported", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun OverviewScreen(
    project: String, imported: String?, onOpenEditor: () -> Unit, onImport: () -> Unit, onNew: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF243C50), Color(0xFF18253B), Color(0xFF30294A))))
                .padding(22.dp)
        ) {
            Column {
                Text("YOUR IDEAS. YOUR OS.", color = Mint, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text("Build a world\nthat boots your way.", color = White, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                Text("Design the interface, write your system code and bring your own OS files into one workspace.", color = Color(0xFFC1CCDC), fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(onClick = onOpenEditor, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) {
                        Icon(Icons.Rounded.Code, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text("Open editor")
                    }
                    OutlinedButton(onClick = onNew, border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(Mint, Color(0xFF8EAFFF))))) {
                        Text("New project", color = White)
                    }
                }
            }
        }
        Text("Workspace", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("PROJECT", project, Icons.Rounded.Layers, Modifier.weight(1f))
            StatCard("SOURCE", "Editable", Icons.Rounded.Terminal, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("DESIGN", " pearOS UI", Icons.Rounded.Palette, Modifier.weight(1f))
            StatCard("BUILD", "Not built yet", Icons.Rounded.RocketLaunch, Modifier.weight(1f))
        }
        Text("Start creating", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        ActionCard(Icons.Rounded.Code, "Write your OS", "Edit the starter system configuration and code.", "Open code editor", onOpenEditor)
        ActionCard(Icons.Rounded.UploadFile, "Import an existing OS", imported ?: "Bring in a ZIP, image or project file from your device.", "Choose file", onImport)
        ActionCard(Icons.Rounded.Brush, "Shape the interface", "Explore the visual direction for your own operating system.", "Open appearance", {})
        Text("A note on real OS builds", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("The editor and design workspace run inside Android. Creating a bootable OS for a specific phone is a separate engineering target and cannot be done by installing this APK alone.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Panel).padding(14.dp)) {
        Icon(icon, null, tint = Mint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(13.dp))
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.2.sp)
        Text(value, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Panel2), Alignment.Center) { Icon(icon, null, tint = Mint) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(body, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Text(action, color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorScreen(code: String, onCode: (String) -> Unit, onBuild: () -> Unit, onExport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("System source", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Edit your pearOS starter configuration", color = Muted, fontSize = 11.sp)
            }
            TextButton(onClick = onExport) { Icon(Icons.Rounded.SaveAlt, null); Text("Export") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("pear.config") }, leadingIcon = { Icon(Icons.Rounded.Description, null) })
            AssistChip(onClick = {}, label = { Text("JSON") })
        }
        Column(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF090E18)).border(1.dp, Color(0xFF26334A), RoundedCornerShape(18.dp)).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { Box(Modifier.padding(end = 5.dp).size(8.dp).clip(CircleShape).background(listOf(Color(0xFFFF7777), Color(0xFFFFD166), Mint)[it])) }
                Text("  pearOS / config", color = Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = code, onValueChange = onCode,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFB7D9D0), fontSize = 12.sp, lineHeight = 19.sp, fontFamily = FontFamily.Monospace),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Mint)
            )
        }
        Button(onClick = onBuild, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) {
            Icon(Icons.Rounded.RocketLaunch, null); Spacer(Modifier.width(8.dp)); Text("Check configuration")
        }
        Text("This editor currently edits a starter configuration, not a Linux/Android kernel source tree.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun AppearanceScreen() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Make it yours", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("A first look at the pearOS visual system: clean Apple-like spacing, Samsung-style personalisation and a desktop-inspired workspace.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFFB5F7D8), Color(0xFF83AFFF), Color(0xFF8C79D8)))).height(170.dp), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("pear", color = Color(0xFF101625), fontSize = 45.sp, fontWeight = FontWeight.Black)
                Text("A fresh start.", color = Color(0xFF18253B), fontSize = 13.sp)
            }
        }
        Text("Design ingredients", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        listOf("Liquid glass surfaces" to "Soft translucent panels and depth", "Adaptive accent colours" to "A colour system for every theme", "Desktop + mobile layouts" to "Responsive windows, dock and quick settings", "Personal widgets" to "A home screen that feels like yours").forEach {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(15.dp)) {
                Text(it.first, color = White, fontWeight = FontWeight.SemiBold)
                Text(it.second, color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilesScreen(imported: String?, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Project files", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Bring an existing project or OS image into your workspace.", color = Muted, fontSize = 13.sp)
        ActionCard(Icons.Rounded.UploadFile, "Import from device", "Select a ZIP, IMG, ISO or source file.", "Browse", onImport)
        if (imported != null) ActionCard(Icons.Rounded.InsertDriveFile, imported, "Selected from your device. Import metadata is shown here.", "Imported", {})
        else Text("No files imported yet.", color = Muted, fontSize = 12.sp)
        Text("Importing a file does not flash it or install it on your phone.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun BuildsScreen(onBuild: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Build centre", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Your builds and previews live here.", color = Muted, fontSize = 13.sp)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(18.dp)) {
            Icon(Icons.Rounded.RocketLaunch, null, tint = Mint, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("No builds yet", color = White, fontWeight = FontWeight.Bold)
            Text("Create your first design preview from the code editor.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Button(onClick = onBuild, colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) { Text("Create build preview") }
        }
    }
}

@Composable
private fun PhonePreviewScreen(project: String) {
    var screen by remember { mutableStateOf("Home") }
    var openedApp by remember { mutableStateOf<String?>(null) }
    var quickPanel by remember { mutableStateOf(false) }
    val apps = listOf(
        Triple("Phone", Icons.Rounded.Call, Color(0xFF5ACB91)),
        Triple("Messages", Icons.Rounded.ChatBubble, Color(0xFF69A9FF)),
        Triple("Camera", Icons.Rounded.PhotoCamera, Color(0xFFB49AFF)),
        Triple("Gallery", Icons.Rounded.Photo, Color(0xFFFF9B83)),
        Triple("Music", Icons.Rounded.MusicNote, Color(0xFFE58BB7)),
        Triple("Files", Icons.Rounded.Folder, Color(0xFFFFC66D)),
        Triple("Browser", Icons.Rounded.Language, Color(0xFF75C9D8)),
        Triple("Settings", Icons.Rounded.Settings, Color(0xFFA8B4C8))
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Phone preview", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("A touch-first pearOS mobile shell", color = Muted, fontSize = 11.sp)
            }
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(Panel).padding(3.dp)) {
                listOf("Lock", "Home").forEach { choice ->
                    Text(choice, color = if (screen == choice) Ink else Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(if (screen == choice) Mint else Color.Transparent).clickable { screen = choice; openedApp = null }.padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.widthIn(max = 340.dp).fillMaxWidth().clip(RoundedCornerShape(38.dp)).border(2.dp, Color(0xFF64728B), RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(Color(0xFF152A3D), Color(0xFF38466D), Color(0xFF71558A)))).padding(13.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("9:41", color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.width(66.dp).height(17.dp).clip(CircleShape).background(Color(0xFF080C14)))
                Text("5G  ▰", color = White, fontSize = 10.sp)
            }
            if (screen == "Lock") {
                Spacer(Modifier.height(48.dp))
                Text("Tuesday, 18 November", color = Color(0xFFDCE6F8), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("9:41", color = White, fontSize = 62.sp, fontWeight = FontWeight.Light, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x55202C48)).padding(14.dp)) {
                    Text("NOTIFICATIONS", color = Color(0xFFD4E2F7), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Messages", color = White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Your pearOS setup is looking good.", color = Color(0xFFD4E2F7), fontSize = 10.sp)
                    Spacer(Modifier.height(9.dp))
                    Text("Calendar", color = White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Nothing planned for the afternoon.", color = Color(0xFFD4E2F7), fontSize = 10.sp)
                }
                Spacer(Modifier.height(35.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("●", color = White, modifier = Modifier.clip(CircleShape).background(Color(0x55405070)).clickable { quickPanel = !quickPanel }.padding(12.dp))
                    Text("Tap to unlock", color = White, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterVertically).clickable { screen = "Home" })
                    Text("◉", color = White, modifier = Modifier.clip(CircleShape).background(Color(0x55405070)).clickable { openedApp = "Camera" }.padding(12.dp))
                }
            } else {
                Spacer(Modifier.height(22.dp))
                Text("Tuesday, 18 November", color = Color(0xFFDCE6F8), fontSize = 11.sp)
                Text("Good morning", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(project, color = Color(0xFFDCE6F8), fontSize = 11.sp)
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(19.dp)).background(Color(0x66202C48)).padding(12.dp)) {
                        Text("WEATHER", color = Color(0xFFDCE6F8), fontSize = 8.sp, letterSpacing = 1.sp)
                        Text("18°", color = White, fontSize = 27.sp, fontWeight = FontWeight.Light)
                        Text("Partly cloudy", color = Color(0xFFDCE6F8), fontSize = 9.sp)
                    }
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(19.dp)).background(Color(0x66202C48)).padding(12.dp)) {
                        Text("YOUR DAY", color = Color(0xFFDCE6F8), fontSize = 8.sp, letterSpacing = 1.sp)
                        Text("All clear", color = White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("No upcoming events", color = Color(0xFFDCE6F8), fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (openedApp == null) {
                    Text("YOUR APPS", color = Color(0xFFDCE6F8), fontSize = 9.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    apps.chunked(4).forEach { rowApps ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            rowApps.forEach { (name, icon, tint) ->
                                Column(Modifier.weight(1f).clickable { openedApp = name }.padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint), Alignment.Center) { Icon(icon, null, tint = Color(0xFF101625), modifier = Modifier.size(24.dp)) }
                                    Spacer(Modifier.height(5.dp))
                                    Text(name, color = White, fontSize = 9.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xDD172238)).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(openedApp!!, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("✕", color = White, modifier = Modifier.clickable { openedApp = null }.padding(5.dp))
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("$openedApp preview", color = Color(0xFFDCE6F8), fontSize = 13.sp)
                        Text("Interactive mock app screen inside the pearOS concept.", color = Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xAA111A2B)).padding(9.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf(Icons.Rounded.Call, Icons.Rounded.ChatBubble, Icons.Rounded.Language, Icons.Rounded.PhotoCamera).forEachIndexed { index, icon ->
                        Icon(icon, null, tint = White, modifier = Modifier.size(23.dp).clickable { openedApp = listOf("Phone", "Messages", "Browser", "Camera")[index] })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(95.dp).height(4.dp).clip(CircleShape).background(Color(0xCCFFFFFF)).align(Alignment.CenterHorizontally).clickable { screen = if (screen == "Lock") "Home" else "Lock"; openedApp = null })
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).clickable { quickPanel = !quickPanel }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Tune, null, tint = Mint)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Quick settings preview", color = White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(if (quickPanel) "Wi-Fi  •  Bluetooth  •  Do not disturb  •  Brightness" else "Tap to inspect the phone control panel", color = Muted, fontSize = 10.sp)
            }
            Icon(if (quickPanel) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = Muted)
        }
        Text("Tap app icons, switch between lock and home screens, and open quick settings. This is a design preview, not a replacement Android launcher yet.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun TemplatesScreen(onChoose: (String) -> Unit) {
    val templates = listOf(
        "pearOS Midnight" to defaultCode,
        "PearOS Minimal" to defaultCode.replace("#8CF0C5", "#A7B9FF").replace("midnight", "minimal"),
        "PearOS Desktop" to defaultCode.replace("custom", "desktop").replace("midnight", "desktop")
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Starter templates", color = White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Load a configuration into the editor and customise it.", color = Muted, fontSize = 12.sp)
        templates.forEach { (name, source) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).clickable { onChoose(source) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DashboardCustomize, null, tint = Mint)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = White, fontWeight = FontWeight.SemiBold)
                    Text("System configuration starter", color = Muted, fontSize = 10.sp)
                }
                Text("Use", color = Mint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val defaultCode = """{
  "name": "pearOS",
  "version": "0.1.0",
  "device": "custom",
  "experience": {
    "style": ["apple", "samsung", "windows"],
    "theme": "midnight",
    "accent": "#8CF0C5",
    "glass": true,
    "desktopMode": false
  },
  "features": {
    "adaptiveHome": true,
    "lockScreen": true,
    "notificationCenter": true,
    "quickSettingsPanel": true,
    "gestureNavigation": true,
    "mobileDock": true,
    "windowManager": true,
    "quickSettings": true,
    "appDock": true,
    "widgets": true,
    "customBootAnimation": true
  }
}
"""
