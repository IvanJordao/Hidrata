package com.ivanjordao.hidrata.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ivanjordao.hidrata.data.HidrataRepository
import com.ivanjordao.hidrata.logic.DayState
import com.ivanjordao.hidrata.logic.Hydration
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Lembretes em segundo plano.
 *
 * O WorkManager é o agendador oficial do Android para tarefas que precisam rodar mesmo
 * com o app fechado (e ele sobrevive à reinicialização do celular).
 * O menor intervalo que ele permite para tarefas repetidas é de 15 minutos — por isso
 * usamos 15 e não 10. O Android ainda pode atrasar alguns minutos para economizar bateria.
 */
object Reminders {
    const val INTERVAL_MIN = 15L
    private const val WORK_NAME = "hidrata-lembrete"

    /** Piso entre duas notificações do mesmo tipo, igual ao PWA (30 min). */
    private const val MIN_GAP_MIN = 30L

    const val KIND_SOON = "soon"
    const val KIND_BEHIND = "behind"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(INTERVAL_MIN, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Decide se deve notificar agora. Chamado pelo WorkManager a cada ~15 min. */
    fun check(context: Context, now: LocalDateTime = LocalDateTime.now()) {
        val repo = HidrataRepository.get(context)
        val s = repo.settings.value ?: return
        if (!s.notify) return
        if (System.currentTimeMillis() < repo.snoozeUntil.value) return

        val r = Hydration.compute(s, repo.log.value, now)
        if (r.state == DayState.DONE || r.state == DayState.IDLE || r.state == DayState.LATE) {
            Notifier.cancel(context)
            return
        }

        val kind: String
        val title: String
        val body: String
        if (r.state == DayState.BEHIND) {
            // 1) Já ficou abaixo da meta para este horário
            kind = KIND_BEHIND
            title = "Hora de beber água 💧"
            body = "Você deveria ter bebido ${Hydration.fmt(r.expected)} até agora e bebeu " +
                "${Hydration.fmt(r.drunk)}. Faltam ${Hydration.fmt(r.expected - r.drunk)}."
        } else {
            // 2) Está no ritmo, mas vai ficar abaixo antes da próxima checagem
            val nextCheck = now.plusMinutes(INTERVAL_MIN)
            if (r.drunk >= Hydration.expectedAt(nextCheck, r.sch, r.goal)) return
            val at = Hydration.behindAt(r.drunk, r.sch, r.goal)
            val mins = Duration.between(now, at).toMinutes().coerceAtLeast(1)
            kind = KIND_SOON
            title = "Quase na hora de beber água ⏳"
            body = "Em cerca de $mins min você ficará abaixo da meta " +
                "(às ${Hydration.hhmm(at)}). Que tal beber um copo agora?"
        }

        // Evita excesso: mesmo tipo só a cada 30 min. Passar de "quase" para "abaixo" avisa na hora.
        val sinceLast = System.currentTimeMillis() - repo.lastNotifAt
        val escalated = kind == KIND_BEHIND && repo.lastNotifKind == KIND_SOON
        if (!escalated && kind == repo.lastNotifKind && sinceLast < MIN_GAP_MIN * 60_000) return

        if (Notifier.show(context, title, body)) {
            repo.lastNotifAt = System.currentTimeMillis()
            repo.lastNotifKind = kind
        }
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Reminders.check(applicationContext)
        return Result.success()
    }
}
