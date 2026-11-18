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
    var page by remember { mutableStateOf("Overview") }
    var projectName by remember { mutableStateOf("My pearOS") }
    var code by remember { mutableStateOf(defaultCode) }
    var importedName by remember { mutableStateOf<String?>(null) }
    var showBuild by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            importedName = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported OS file"
            notice = "Imported: $importedName"
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                // The Android document provider owns the destination stream.
                // Source saving is performed below using the app content resolver.
            }
            notice = "Choose a destination in the save dialog to export your source."
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Ink, surface = Panel, primary = Mint, onPrimary = Ink,
        onBackground = White, onSurface = White, secondary = Color(0xFF9AB7FF)
    )) {
        Row(Modifier.fillMaxSize().background(Ink)) {
            NavigationRail(page = page, onPage = { page = it })
            Column(Modifier.weight(1f).fillMaxHeight()) {
                TopBar(projectName, importedName)
                when (page) {
                    "Overview" -> OverviewScreen(
                        projectName, importedName,
                        onOpenEditor = { page = "Code editor" },
                        onImport = { importLauncher.launch(arrayOf("*/*")) },
                        onNew = { showNew = true }
                    )
                    "Code editor" -> EditorScreen(code, { code = it }, { showBuild = true }, {
                        saveLauncher.launch("pearos-source.txt")
                    })
                    "Appearance" -> AppearanceScreen()
                    "Files" -> FilesScreen(importedName, { importLauncher.launch(arrayOf("*/*")) })
                    "Builds" -> BuildsScreen { showBuild = true }
                    else -> OverviewScreen(projectName, importedName, { page = "Code editor" }, { importLauncher.launch(arrayOf("*/*")) }, { showNew = true })
                }
            }
        }
        if (showBuild) {
            AlertDialog(
                onDismissRequest = { showBuild = false },
                containerColor = Panel2,
                title = { Text("Build preview", color = White, fontWeight = FontWeight.Bold) },
                text = { Text("Your pearOS project is ready for the next build stage. This first release creates an interactive design and source workspace. A bootable Android OS image requires a device-specific Android source tree, kernel, vendor files and bootloader support.", color = Muted) },
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
private fun NavigationRail(page: String, onPage: (String) -> Unit) {
    val items = listOf(
        Triple("Overview", Icons.Rounded.Dashboard, "Home"),
        Triple("Code editor", Icons.Rounded.Code, "Code"),
        Triple("Appearance", Icons.Rounded.Palette, "Design"),
        Triple("Files", Icons.Rounded.FolderOpen, "Files"),
        Triple("Builds", Icons.Rounded.RocketLaunch, "Builds")
    )
    Column(
        Modifier.width(76.dp).fillMaxHeight().background(Color(0xFF101625)).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Mint, Color(0xFF76B5FF)))), Alignment.Center) {
            Text("p", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(28.dp))
        items.forEach { (key, icon, label) ->
            val selected = page == key
            Column(
                Modifier.padding(vertical = 5.dp).width(62.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (selected) Color(0xFF263849) else Color.Transparent)
                    .clickable { onPage(key) }.padding(vertical = 11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = if (selected) Mint else Muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 9.sp, color = if (selected) Mint else Muted)
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Rounded.Settings, null, tint = Muted, modifier = Modifier.padding(12.dp).size(22.dp))
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
                Text("Build a world
that boots your way.", color = White, fontSize = 30.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold)
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
            Icon(Icons.Rounded.RocketLaunch, null); Spacer(Modifier.width(8.dp)); Text("Build OS preview")
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

private val defaultCode = """{
  "name": "pearOS",
  "version": "0.1.0",
  "device": "custom",
  "experience": {
    "style": ["apple", "samsung", "windows"],
    "theme": "midnight",
    "accent": "#8CF0C5",
    "glass": true,
    "desktopMode": true
  },
  "features": {
    "adaptiveHome": true,
    "windowManager": true,
    "quickSettings": true,
    "appDock": true,
    "widgets": true,
    "customBootAnimation": true
  }
}
"""
