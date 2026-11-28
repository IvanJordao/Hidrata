package com.ivanjordao.hidrata.logic

import java.text.NumberFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Regras de negócio do Hidrata — tradução direta das funções schedule(), expectedAt(),
 * nextMilestone() e compute() do index.html. Não depende de nada do Android,
 * por isso dá para testar com testes de unidade comuns (veja HydrationTest.kt).
 */

/** Configurações do usuário. [wake] e [sleep] no formato "HH:mm". */
data class Settings(
    val goalMl: Int,
    val wake: String,
    val sleep: String,
    val notify: Boolean,
)

/** Um registro de água. [t] é o instante em milissegundos (como Date.now() no JS). */
data class Entry(val id: String, val t: Long, val ml: Int)

/** O "dia lógico": chave de histórico (aaaa-mm-dd), hora de acordar e de dormir. */
data class DaySchedule(val key: String, val wakeAt: LocalDateTime, val sleepAt: LocalDateTime)

enum class DayState { IDLE, OK, BEHIND, LATE, DONE }

/** Fotografia do dia em um instante: tudo que a tela e as notificações precisam. */
data class Snapshot(
    val now: LocalDateTime,
    val sch: DaySchedule,
    val entries: List<Entry>,
    val drunk: Int,
    val goal: Int,
    val expected: Int,
    val state: DayState,
    val title: String,
    val text: String,
)

object Hydration {
    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR"))
    private val hhmmFmt = DateTimeFormatter.ofPattern("HH:mm")

    fun fmt(ml: Int): String = nf.format(ml) + " ml"
    fun num(n: Int): String = nf.format(n)
    fun hhmm(t: LocalDateTime): String = t.format(hhmmFmt)
    fun toMin(s: String): Int = s.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    /**
     * O dia lógico começa no meio do período de sono (ex.: dorme 22h, acorda 6h → dia começa às 2h).
     * Assim, quem dorme depois da meia-noite não tem o dia "zerado" antes de ir dormir.
     */
    fun schedule(s: Settings, now: LocalDateTime): DaySchedule {
        val wake = toMin(s.wake)
        val sl = toMin(s.sleep)
        val awakeRaw = ((sl - wake) + 1440) % 1440
        val awake = if (awakeRaw == 0) 1440 else awakeRaw
        val asleep = 1440 - awake
        val startMin = (sl + asleep / 2) % 1440
        var start = now.toLocalDate().atStartOfDay().plusMinutes(startMin.toLong())
        if (start.isAfter(now)) start = start.minusDays(1)
        val wakeOff = ((wake - startMin) + 1440) % 1440
        val wakeAt = start.plusMinutes(wakeOff.toLong())
        val sleepAt = wakeAt.plusMinutes(awake.toLong())
        return DaySchedule(start.toLocalDate().toString(), wakeAt, sleepAt)
    }

    /** Quanto já deveria ter sido bebido no instante [t] (meta distribuída por igual no período acordado). */
    fun expectedAt(t: LocalDateTime, sch: DaySchedule, goal: Int): Int {
        val total = Duration.between(sch.wakeAt, sch.sleepAt).toMillis().toDouble()
        val done = Duration.between(sch.wakeAt, t).toMillis().toDouble()
        val f = min(1.0, max(0.0, done / total))
        return (goal * f).roundToInt()
    }

    /** Próxima hora cheia (ou a hora de dormir, se vier antes). */
    fun nextMilestone(now: LocalDateTime, sch: DaySchedule): LocalDateTime {
        val t = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
        return if (t.isAfter(sch.sleepAt)) sch.sleepAt else t
    }

    /** Instante em que a meta esperada vai alcançar o que já foi bebido (a partir daí, fica "abaixo"). */
    fun behindAt(drunk: Int, sch: DaySchedule, goal: Int): LocalDateTime {
        val total = Duration.between(sch.wakeAt, sch.sleepAt).toMillis()
        val f = min(1.0, drunk.toDouble() / goal)
        return sch.wakeAt.plusNanos((total * f * 1_000_000).toLong())
    }

    fun compute(s: Settings, log: Map<String, List<Entry>>, now: LocalDateTime = LocalDateTime.now()): Snapshot {
        val sch = schedule(s, now)
        val entries = (log[sch.key] ?: emptyList()).sortedByDescending { it.t }
        val drunk = entries.sumOf { it.ml }
        val goal = s.goalMl
        val exp = expectedAt(now, sch, goal)
        val state: DayState
        val title: String
        val text: String
        when {
            drunk >= goal -> {
                state = DayState.DONE; title = "Meta do dia concluída! 🎉"
                text = "Você bebeu ${fmt(drunk)} hoje. Excelente trabalho!"
            }
            now.isBefore(sch.wakeAt) -> {
                state = DayState.IDLE; title = "Seu dia ainda não começou"
                text = "A contagem começa às ${hhmm(sch.wakeAt)}. Bom descanso!"
            }
            !now.isBefore(sch.sleepAt) -> {
                state = DayState.LATE; title = "Dia encerrado"
                text = "Faltaram ${fmt(goal - drunk)} para bater a meta de hoje. Amanhã tem mais!"
            }
            drunk < exp -> {
                val m = nextMilestone(now, sch)
                state = DayState.BEHIND; title = "Abaixo da meta para este horário"
                text = "Às ${hhmm(now)} você já deveria ter bebido ${fmt(exp)}, mas bebeu ${fmt(drunk)}. " +
                    "Faltam ${fmt(exp - drunk)} para entrar no ritmo " +
                    "(até as ${hhmm(m)}, o ideal é chegar a ${fmt(expectedAt(m, sch, goal))})."
            }
            else -> {
                val m = nextMilestone(now, sch)
                val need = max(0, expectedAt(m, sch, goal) - drunk)
                state = DayState.OK; title = "No ritmo certo 💪"
                text = "Você está ${fmt(drunk - exp)} à frente do esperado. " +
                    (if (need > 0) "Até as ${hhmm(m)}, beba mais ${fmt(need)}." else "Continue assim!")
            }
        }
        return Snapshot(now, sch, entries, drunk, goal, exp, state, title, text)
    }
}
