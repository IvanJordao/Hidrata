package com.ivanjordao.hidrata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ivanjordao.hidrata.data.HidrataRepository
import com.ivanjordao.hidrata.notify.Notifier
import com.ivanjordao.hidrata.notify.Reminders
import com.ivanjordao.hidrata.ui.HidrataApp

/** Ponto de entrada do app: é a "página" que o Android abre ao tocar no ícone. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Notifier.createChannel(this)
        val repo = HidrataRepository.get(this)
        // Garante que os lembretes estejam agendados se o usuário os ativou
        if (repo.settings.value?.notify == true) Reminders.schedule(this)

        setContent { HidrataApp(repo) }
    }
}
