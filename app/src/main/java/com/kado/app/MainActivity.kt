package com.kado.app

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.kado.app.barcode.BarcodeTools
import com.kado.app.data.BarcodeKind
import com.kado.app.data.CardRecord
import com.kado.app.data.CardRepository
import com.kado.app.ui.screens.AddCardScreen
import com.kado.app.ui.screens.CardDetailScreen
import com.kado.app.ui.screens.CardEditorScreen
import com.kado.app.ui.screens.CheckoutScreen
import com.kado.app.ui.screens.ScannerScreen
import com.kado.app.ui.screens.SettingsScreen
import com.kado.app.ui.screens.WalletScreen
import com.kado.app.ui.theme.KadoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Page {
    data object Home : Page
    data object AllCards : Page
    data object Settings : Page
    data object Add : Page
    data object Scan : Page
    data class Editor(val id: String? = null, val scanned: String = "", val kind: BarcodeKind? = null) : Page
    data class Detail(val id: String) : Page
    data class Checkout(val id: String) : Page
}

class MainActivity : ComponentActivity() {
    private val repository by lazy { CardRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KadoTheme { KadoApp(repository) }
        }
    }
}

@Composable
private fun KadoApp(repository: CardRepository) {
    val context = LocalContext.current
    val activity = context as Activity
    val cards by repository.cards.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var page by remember { mutableStateOf<Page>(Page.Home) }
    var scannerReturn by remember { mutableStateOf<Page.Editor?>(null) }
    var importReturn by remember { mutableStateOf<Page.Editor?>(null) }
    var focusId by remember { mutableStateOf<String?>(null) }

    fun show(message: String) { scope.launch { snackbar.showSnackbar(message) } }
    fun back() {
        page = when (val current = page) {
            Page.Home -> Page.Home
            Page.Scan -> scannerReturn ?: Page.Add
            Page.AllCards, Page.Settings, Page.Add -> Page.Home
            is Page.Detail -> Page.Home
            is Page.Checkout -> Page.Detail(current.id)
            is Page.Editor -> current.id?.let { Page.Detail(it) } ?: Page.Add
        }
    }

    LaunchedEffect(Unit) {
        runCatching { repository.load() }.onFailure { show("Не удалось загрузить карты: ${it.message}") }
    }
    LaunchedEffect(focusId) {
        if (focusId != null) {
            delay(1600)
            focusId = null
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) page = Page.Scan else show("Для сканирования разрешите доступ к камере")
    }
    fun requestScanner(returnPage: Page.Editor?) {
        scannerReturn = returnPage
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            page = Page.Scan
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val scanner = BarcodeScanning.getClient()
            try {
                val image = InputImage.fromFilePath(context, uri)
                scanner.process(image).addOnSuccessListener { found ->
                    val match = found.firstNotNullOfOrNull { b ->
                        val type = BarcodeTools.kindFromMlKit(b.format)
                        val code = b.rawValue
                        if (type != null && !code.isNullOrBlank()) Pair(code, type) else null
                    }
                    if (match != null) {
                        val target = importReturn ?: Page.Editor()
                        page = target.copy(scanned = match.first, kind = match.second)
                        show("Код распознан. Укажите название карты")
                    } else show("На изображении не найден поддерживаемый штрихкод или QR-код")
                }.addOnFailureListener { show("Не удалось прочитать изображение: ${it.message}") }
                    .addOnCompleteListener { scanner.close() }
            } catch (e: Exception) {
                scanner.close()
                show("Не удалось открыть фото: ${e.message}")
            }
        }
    }
    fun requestGallery(returnPage: Page.Editor?) {
        importReturn = returnPage
        gallery.launch("image/*")
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val content = repository.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                        ?: error("Не удалось создать файл")
                }
            }.onSuccess { show("Резервная копия сохранена") }
                .onFailure { show("Ошибка экспорта: ${it.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: error("Не удалось открыть файл")
                }
                repository.importJson(text)
            }.onSuccess { show("Добавлено карт: $it. Существующие карты сохранены") }
                .onFailure { show("Ошибка импорта: ${it.message}") }
        }
    }

    if (page is Page.Checkout) {
        DisposableEffect(page) {
            val window = activity.window
            val originalBrightness = window.attributes.screenBrightness
            val hadKeepOn = window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = 1f }
            onDispose {
                window.attributes = window.attributes.apply { screenBrightness = originalBrightness }
                if (!hadKeepOn) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    BackHandler(enabled = page != Page.Home) { back() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (page == Page.Home || page == Page.AllCards || page == Page.Settings) {
                NavigationBar {
                    NavigationBarItem(
                        selected = page == Page.Home, onClick = { page = Page.Home },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Главная") },
                        label = { Text("Главная") }
                    )
                    NavigationBarItem(
                        selected = page == Page.AllCards, onClick = { page = Page.AllCards },
                        icon = { Icon(Icons.Outlined.CreditCard, contentDescription = "Карты") },
                        label = { Text("Карты") }
                    )
                    NavigationBarItem(
                        selected = page == Page.Settings, onClick = { page = Page.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                        label = { Text("Настройки") }
                    )
                }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding)) {
                when (val current = page) {
                    Page.Home, Page.AllCards -> WalletScreen(
                        cards = cards, allMode = current == Page.AllCards, focusId = focusId,
                        onOpen = { card ->
                            page = Page.Detail(card.id)
                            scope.launch { repository.markOpened(card.id) }
                        },
                        onAdd = { page = Page.Add },
                        onFavorite = { id -> scope.launch { repository.toggleFavorite(id) } },
                    )
                    Page.Settings -> SettingsScreen(
                        cardCount = cards.size,
                        onExport = { exportLauncher.launch("KADO-backup.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    )
                    Page.Add -> AddCardScreen(
                        onBack = { back() },
                        onScan = { requestScanner(null) },
                        onGallery = { requestGallery(null) },
                        onManual = { page = Page.Editor() }
                    )
                    is Page.Editor -> CardEditorScreen(
                        existing = cards.firstOrNull { it.id == current.id },
                        initialValue = current.scanned,
                        initialKind = current.kind,
                        onSave = { card ->
                            scope.launch {
                                runCatching { repository.put(card) }
                                    .onSuccess { focusId = card.id; page = Page.Home; show("Карта сохранена") }
                                    .onFailure { show("Ошибка сохранения: ${it.message}") }
                            }
                        },
                        onBack = { back() },
                        onScan = { requestScanner(current) },
                        onGallery = { requestGallery(current) },
                    )
                    Page.Scan -> ScannerScreen(onBack = { back() }, onDetected = { value, kind ->
                        val target = scannerReturn ?: Page.Editor()
                        page = target.copy(scanned = value, kind = kind)
                    })
                    is Page.Detail -> {
                        val card = cards.firstOrNull { it.id == current.id }
                        if (card == null) LaunchedEffect(current.id) { page = Page.Home }
                        else CardDetailScreen(
                            card = card,
                            onBack = { back() },
                            onFullscreen = { page = Page.Checkout(card.id) },
                            onEdit = { page = Page.Editor(card.id) },
                            onFavorite = { scope.launch { repository.toggleFavorite(card.id) } },
                            onDelete = { scope.launch { repository.delete(card.id); page = Page.Home } },
                            onCopy = { code ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Номер карты", code))
                                show("Номер скопирован")
                            }
                        )
                    }
                    is Page.Checkout -> {
                        val card = cards.firstOrNull { it.id == current.id }
                        if (card == null) LaunchedEffect(current.id) { page = Page.Home }
                        else CheckoutScreen(card = card, onBack = { back() })
                    }
                }
        }
    }
}
