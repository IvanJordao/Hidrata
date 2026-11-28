package com.ivanjordao.hidrata.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivanjordao.hidrata.data.HidrataRepository
import com.ivanjordao.hidrata.logic.DayState
import com.ivanjordao.hidrata.logic.Hydration
import com.ivanjordao.hidrata.logic.Settings
import com.ivanjordao.hidrata.notify.Notifier
import com.ivanjordao.hidrata.notify.Reminders
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Tela principal. Em Compose a tela é uma função: sempre que um "estado" muda
 * (registros, configurações, hora atual), a função roda de novo e a tela se redesenha.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HidrataApp(repo: HidrataRepository) = HidrataTheme {
    val context = LocalContext.current
    val settings by repo.settings.collectAsStateWithLifecycle()
    val log by repo.log.collectAsStateWithLifecycle()
    val snoozeUntil by repo.snoozeUntil.collectAsStateWithLifecycle()

    // Relógio da tela: atualiza a cada minuto e sempre que o app volta para a frente (como o setInterval do PWA)
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000); now = LocalDateTime.now()
        }
    }
    LifecycleResumeEffect(Unit) {
        now = LocalDateTime.now()
        onPauseOrDispose { }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var showSettings by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var bumpKey by remember { mutableIntStateOf(0) }

    fun toast(msg: String, undo: (() -> Unit)? = null) = scope.launch {
        snackbar.currentSnackbarData?.dismiss()
        val res = snackbar.showSnackbar(msg, actionLabel = if (undo != null) "Desfazer" else null, duration = SnackbarDuration.Short)
        if (res == SnackbarResult.ActionPerformed) undo?.invoke()
    }

    // ---------- Notificações ----------
    fun setNotify(on: Boolean, askedUser: Boolean) {
        val s = repo.settings.value ?: return
        repo.saveSettings(s.copy(notify = on))
        if (on) Reminders.schedule(context) else Reminders.cancel(context)
        if (askedUser && !on) toast("Notificações bloqueadas. Libere nas configurações do Android.")
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        setNotify(granted && Notifier.hasPermission(context), askedUser = true)
    }
    fun enableNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.hasPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setNotify(Notifier.hasPermission(context), askedUser = true)
        }
    }

    // ---------- Ações ----------
    fun addWater(ml: Int) {
        val (key, entry) = repo.addWater(ml) ?: return
        Notifier.cancel(context)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        bumpKey++
        now = LocalDateTime.now()
        toast("+${Hydration.fmt(ml)} registrado") { repo.removeEntry(key, entry.id) }
    }

    val background = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(C.bg1, C.bg2)))
        .background(Brush.radialGradient(listOf(C.w1.copy(alpha = .18f), Color.Transparent), center = androidx.compose.ui.geometry.Offset(900f, -100f), radius = 1400f))

    Box(background) {
        val s = settings
        if (s == null) {
            // Primeiro uso: tela de boas-vindas
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                    .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                SettingsForm(
                    initial = null, first = true, notifyOn = false,
                    onSave = { goal, wake, sleep ->
                        repo.saveSettings(Settings(goal, wake, sleep, notify = false))
                        enableNotifications()   // o objetivo do app nativo: já pede para notificar
                    },
                    onToggleNotify = {}, onReset = {},
                )
            }
            return@Box
        }

        val r = Hydration.compute(s, log, now)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbar) },
        ) { inner ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Header(now, onSettings = { showSettings = true })
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Glass(r.drunk, r.goal, bumpKey)
                    }
                    StatusCard(r)
                    CupsCard(onAdd = ::addWater, onCustom = { showCustom = true })
                    HistoryCard(r.entries, onRemove = { e ->
                        repo.removeEntry(r.sch.key, e.id)
                        toast("Registro removido")
                    })
                }
            }
        }

        // Alerta em tela (o mesmo banner vermelho do PWA)
        val showBanner = r.state == DayState.BEHIND && System.currentTimeMillis() >= snoozeUntil
        AnimatedVisibility(
            visible = showBanner,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp).widthIn(max = 456.dp),
            enter = slideInVertically { -it * 2 },
            exit = slideOutVertically { -it * 2 },
        ) {
            BehindBanner(
                r,
                onDrink = { addWater(Notifier.BANNER_CUP) },
                onSnooze = { repo.snooze() },
            )
        }

        if (showSettings) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = sheetState,
                containerColor = C.sheet,
            ) {
                Column(
                    Modifier.verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp).padding(bottom = 22.dp).navigationBarsPadding(),
                ) {
                    SettingsForm(
                        initial = s, first = false,
                        notifyOn = s.notify && Notifier.hasPermission(context),
                        onSave = { goal, wake, sleep ->
                            repo.saveSettings(s.copy(goalMl = goal, wake = wake, sleep = sleep))
                            showSettings = false
                            now = LocalDateTime.now()
                        },
                        onToggleNotify = {
                            if (s.notify && Notifier.hasPermission(context)) setNotify(false, askedUser = false)
                            else enableNotifications()
                        },
                        onReset = {
                            repo.clearDay(Hydration.schedule(s, LocalDateTime.now()).key)
                            showSettings = false
                        },
                    )
                }
            }
        }

        if (showCustom) {
            CustomAmountDialog(
                onDismiss = { showCustom = false },
                onAdd = { ml -> showCustom = false; addWater(ml) },
            )
        }
    }
}
