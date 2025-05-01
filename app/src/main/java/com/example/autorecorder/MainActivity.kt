package com.example.autorecorder

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.autorecorder.common.Utils
import com.example.autorecorder.common.notification.NotificationHelper
import com.example.autorecorder.entity.Streamer
import com.example.autorecorder.entity.Template
import com.example.autorecorder.screen.adb.AdbHelpScreen
import com.example.autorecorder.screen.adb.AdbScreen
import com.example.autorecorder.screen.home.HomeScreen
import com.example.autorecorder.screen.home.HomeViewModel
import com.example.autorecorder.screen.license.LicenseScreen
import com.example.autorecorder.screen.menu.MenuMoreScreen
import com.example.autorecorder.screen.menu.MenuScreen
import com.example.autorecorder.screen.qr.QrScreen
import com.example.autorecorder.screen.qr.QrViewModel
import com.example.autorecorder.screen.recorder.BackupScreen
import com.example.autorecorder.screen.recorder.QualityScreen
import com.example.autorecorder.screen.recorder.RecorderScreen
import com.example.autorecorder.screen.streamer.StreamerEditScreen
import com.example.autorecorder.screen.streamer.StreamerScreen
import com.example.autorecorder.screen.streamer.StreamerViewModel
import com.example.autorecorder.screen.template.TemplateEditScreen
import com.example.autorecorder.screen.template.TemplateScreen
import com.example.autorecorder.screen.template.TemplateViewModel
import com.example.autorecorder.screen.terms.TermsScreen
import com.example.autorecorder.screen.up.UpScreen
import com.example.autorecorder.screen.up.UpViewModel
import com.example.autorecorder.screen.upload.UploadScreen
import com.example.autorecorder.screen.upload.UploadViewModel
import com.example.autorecorder.ui.theme.AutoRecorderTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    private val streamerViewModel: StreamerViewModel by viewModels()
    private val qrViewModel: QrViewModel by viewModels()
    private val templateViewModel: TemplateViewModel by viewModels()
    private val upViewModel: UpViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()
    private val uploadViewModel: UploadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e("CrashHandler", "Unhandled exception in ${thread.name}", throwable)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            Utils.initRecordFolder()
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf()
        } + arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WAKE_LOCK,
        )
        ActivityCompat.requestPermissions(this, permissions, 0)

        setContent {
            AutoRecorderTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = HomeScreen,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {
                    composable<HomeScreen> {
                        val uiState by homeViewModel.uiState.collectAsState()
                        LaunchedEffect(Unit) {
                            homeViewModel.onLoad()
                        }
                        HomeScreen(
                            uiState = uiState,
                            onMenuClick = {
                                navController.navigate(MenuScreen) },
                            onConnectClick = {
                                homeViewModel.onAdbConnectButtonClick()
                            },
                            onAddStreamerClick = {
                                navController.navigate(StreamerEditScreen(
                                    Streamer.new(),
                                    true
                                ))
                            },
                            onAdbClick = {
                                navController.navigate(AdbScreen)
                            },
                            onQualityClick = {
                                navController.navigate(QualityScreen)
                            },
                            onStopFetchClick = {
                                homeViewModel.onStopFetchLiveClick()
                            },
                            onPingClick = homeViewModel::onPingClick
                        )
                    }
                    composable<MenuScreen> {
                        MenuScreen(
                            onBackClick = { navController.popBackStack() },
                            onTemplateClick = {
                                navController.navigate(TemplateScreen)
                            },
                            onUpClick = {
                                navController.navigate(UpScreen)
                            },
                            onStreamerClick = {
                                navController.navigate(StreamerScreen)
                            },
                            onUploadClick = {
                                navController.navigate(UploadScreen)
                            },
                            onRecorderClick = {
                                navController.navigate(RecorderScreen) {
                                    popUpTo<MenuScreen> {
                                        inclusive = true
                                    }
                                }
                            },
                            onMenuMoreClick = {
                                navController.navigate(MenuMoreScreen)
                            }
                        )
                    }
                    composable<MenuMoreScreen> {
                        MenuMoreScreen(
                            onBackClick = { navController.popBackStack() },
                            onLicenseClick = {
                                navController.navigate(LicenseScreen)
                            },
                            onTermsClick = {
                                navController.navigate(TermsScreen)
                            }
                        )
                    }
                    composable<StreamerScreen> {
                        val uiState by streamerViewModel.uiState.collectAsState()
                        StreamerScreen(
                            modifier = Modifier,
                            uiState = uiState,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onErrorDialogDismiss = { streamerViewModel.clearError() },
                            onItemClick = { item, isNew ->
                                navController.navigate(StreamerEditScreen(item, isNew))
                            }
                        )
                    }
                    composable<StreamerEditScreen>(
                        typeMap = mapOf(typeOf<Streamer>() to StreamerNavType)
                    ) {
                        val args = it.toRoute<StreamerEditScreen>()
                        val templateUiState by templateViewModel.uiState.collectAsState()
                        LaunchedEffect(Unit) {
                            templateViewModel.onLoad()
                        }
                        StreamerEditScreen(
                            isNew = args.isNew,
                            streamer = args.item,
                            templates = templateUiState.items,
                            onDoneButtonClick = { item ->
                                streamerViewModel.upsertItem(item)
                                navController.popBackStack()
                            },
                            onBackButtonClick = {
                                navController.popBackStack()
                            },
                            onAddTemplateClick = {
                                navController.navigate(TemplateEditScreen(
                                    Template(),
                                    true
                                ))
                            },
                            onDeleteButtonClick = { item ->
                                streamerViewModel.deleteItem(item)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable<TemplateScreen> {
                        val templateUiState by templateViewModel.uiState.collectAsState()
                        LaunchedEffect(Unit) {
                            templateViewModel.onLoad()
                        }
                        TemplateScreen(
                            uiState = templateUiState,
                            onBackClick = { navController.popBackStack() },
                            onItemClick = { item, isNew ->
                                navController.navigate(TemplateEditScreen(item, isNew))
                            }
                        )
                    }
                    composable<TemplateEditScreen>(
                        typeMap = mapOf(typeOf<Template>() to TemplateNavType)
                    ) {
                        val args = it.toRoute<TemplateEditScreen>()
                        TemplateEditScreen(
                            isNew = args.isNew,
                            template = args.item,
                            onDoneButtonClick = { template ->
                                templateViewModel.upsertItem(template)
                                navController.popBackStack()
                            },
                            onBackButtonClick = {
                                navController.popBackStack()
                            },
                            onAddUpButtonClick = {
                                navController.navigate(QrScreen)
                            },
                            onDeleteClick = { template ->
                                templateViewModel.deleteItem(template)
                                navController.popBackStack()
                            }
                        )
                    }
                    composable<UpScreen> {
                        val upUiState by upViewModel.uiState.collectAsState()
                        LaunchedEffect(Unit) {
                            upViewModel.onLoad()
                        }
                        UpScreen(
                            onBackClick = { navController.popBackStack() },
                            onAddUpClick = { navController.navigate(QrScreen) },
                            onDeleteClick = { fileName ->
                                templateViewModel.deleteItem(fileName.toLong())
                                upViewModel.onDeleteClick(fileName)
                            },
                            list = upUiState.list
                        )
                    }
                    composable<QrScreen> {
                        val qrUiState by qrViewModel.uiState.collectAsState()
                        LaunchedEffect(Unit) {
                            qrViewModel.onLoad()
                        }
                        QrScreen(
                            text = qrUiState.qrData?.second ?: "",
                            onBackButtonClick = { navController.popBackStack() },
                            onClickActivateButton = {
                                qrViewModel.onQrActivateButtonClick {
                                    navController.popBackStack()
                                }
                            }
                        )
                        DisposableEffect(Unit) {
                            onDispose {
                                qrViewModel.onClear()
                            }
                        }
                    }
                    composable<AdbScreen> {
                        AdbScreen(
                            onBackButtonClick = { navController.popBackStack() },
                            onTestCommandClick = { command ->
                                homeViewModel.onTestCommandClick(command)
                            },
                            onPairClick = {
                                NotificationHelper(this@MainActivity).showNotification()
                            },
                            onHelpClick = {
                                navController.navigate(AdbHelpScreen)
                            }
                        )
                    }
                    composable<AdbHelpScreen> {
                        AdbHelpScreen(
                            onBackButtonClick = { navController.popBackStack() },
                        )
                    }
                    composable<UploadScreen> {
                        val uploadFlow by uploadViewModel.planList.collectAsState()
                        val runningList by uploadViewModel.runningFlow.collectAsState()
                        UploadScreen(
                            list = uploadFlow,
                            runningList = runningList.keys.toList(),
                            onBackButtonClick = { navController.popBackStack() },
                            onCancelClick = {
                                UploadViewModel.cancelUpload(this@MainActivity, it)
                            },
                            onUploadClick = { plan ->
                                UploadViewModel.startUpload(this@MainActivity, plan)
                            },
                            onDeleteClick = {
                                uploadViewModel.deletePlan(it)
                            }
                        )
                    }
                    composable<RecorderScreen> {
                        RecorderScreen(
                            onBackButtonClick = { navController.popBackStack() },
                            onBackupClick = {
                                navController.navigate(BackupScreen)
                            },
                        )
                    }
                    composable<BackupScreen> {
                        BackupScreen(
                            onBackButtonClick = { navController.popBackStack() },
                        )
                    }
                    composable<QualityScreen> {
                        QualityScreen {
                            navController.popBackStack()
                        }
                    }
                    composable<LicenseScreen> {
                        LicenseScreen {
                            navController.popBackStack()
                        }
                    }
                    composable<TermsScreen> {
                        TermsScreen(
                            onBackButtonClick = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}

@Serializable
object StreamerScreen

@Serializable
data class StreamerEditScreen(
    val item: Streamer,
    val isNew: Boolean
)

@Serializable
object MenuScreen

@Serializable
object MenuMoreScreen

@Serializable
object UpScreen

@Serializable
object QrScreen

@Serializable
object AdbScreen

@Serializable
object AdbHelpScreen

@Serializable
object HomeScreen

@Serializable
object UploadScreen

@Serializable
object RecorderScreen

@Serializable
object BackupScreen

@Serializable
object QualityScreen

@Serializable
object LicenseScreen

@Serializable
object TermsScreen

@Serializable
object TemplateScreen

@Serializable
data class TemplateEditScreen(
    val item: Template,
    val isNew: Boolean
)

val StreamerNavType = object : NavType<Streamer>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): Streamer {
        return bundle.getSerializable(key) as Streamer
    }

    override fun parseValue(value: String): Streamer {
        return Json.decodeFromString(value)
    }

    override fun serializeAsValue(value: Streamer): String {
        return Uri.encode(Json.encodeToString(value))
    }

    override fun put(bundle: Bundle, key: String, value: Streamer) {
        bundle.putSerializable(key, value as java.io.Serializable)
    }
}

val TemplateNavType = object : NavType<Template>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): Template {
        return bundle.getSerializable(key) as Template
    }

    override fun parseValue(value: String): Template {
        return Json.decodeFromString(value)
    }

    override fun serializeAsValue(value: Template): String {
        return Uri.encode(Json.encodeToString(value))
    }

    override fun put(bundle: Bundle, key: String, value: Template) {
        bundle.putSerializable(key, value as java.io.Serializable)
    }
}