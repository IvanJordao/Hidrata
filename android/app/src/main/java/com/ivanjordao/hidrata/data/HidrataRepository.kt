package com.ivanjordao.hidrata.data

import android.content.Context
import com.ivanjordao.hidrata.logic.Entry
import com.ivanjordao.hidrata.logic.Hydration
import com.ivanjordao.hidrata.logic.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/**
 * Guarda os dados no celular — faz o papel do localStorage do PWA.
 * Usa SharedPreferences (um arquivo chave→valor privado do app) com os mesmos dados em JSON.
 *
 * É um "singleton" (uma única instância no app), assim a tela, a notificação e o futuro
 * widget enxergam sempre os mesmos totais. Os StateFlow avisam a tela quando algo muda.
 */
class HidrataRepository private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hidrata", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings?> = _settings.asStateFlow()

    private val _log = MutableStateFlow(loadLog())
    val log: StateFlow<Map<String, List<Entry>>> = _log.asStateFlow()

    private val _snoozeUntil = MutableStateFlow(prefs.getLong(KEY_SNOOZE, 0L))
    val snoozeUntil: StateFlow<Long> = _snoozeUntil.asStateFlow()

    // ---------- Configurações ----------
    private fun loadSettings(): Settings? {
        val raw = prefs.getString(KEY_SETTINGS, null) ?: return null
        return try {
            val o = JSONObject(raw)
            Settings(o.getInt("goal"), o.getString("wake"), o.getString("sleep"), o.optBoolean("notify", false))
        } catch (e: Exception) {
            null
        }
    }

    fun saveSettings(s: Settings) {
        val o = JSONObject().put("goal", s.goalMl).put("wake", s.wake).put("sleep", s.sleep).put("notify", s.notify)
        prefs.edit().putString(KEY_SETTINGS, o.toString()).apply()
        _settings.value = s
    }

    // ---------- Registros ----------
    private fun loadLog(): Map<String, List<Entry>> {
        val raw = prefs.getString(KEY_LOG, null) ?: return emptyMap()
        return try {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { day ->
                val arr = o.getJSONArray(day)
                (0 until arr.length()).map { i ->
                    val e = arr.getJSONObject(i)
                    Entry(e.getString("id"), e.getLong("t"), e.getInt("ml"))
                }
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveLog(log: Map<String, List<Entry>>) {
        val o = JSONObject()
        log.forEach { (day, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(JSONObject().put("id", it.id).put("t", it.t).put("ml", it.ml)) }
            o.put(day, arr)
        }
        prefs.edit().putString(KEY_LOG, o.toString()).apply()
        _log.value = log
    }

    /** Registra [ml] no dia lógico atual e devolve o par (dia, registro) para permitir "Desfazer". */
    @Synchronized
    fun addWater(ml: Int, now: LocalDateTime = LocalDateTime.now()): Pair<String, Entry>? {
        val s = _settings.value ?: return null
        if (ml <= 0) return null
        val key = Hydration.schedule(s, now).key
        val entry = Entry(newId(), System.currentTimeMillis(), ml)
        val current = _log.value
        saveLog(current + (key to ((current[key] ?: emptyList()) + entry)))
        return key to entry
    }

    @Synchronized
    fun removeEntry(key: String, id: String) {
        val current = _log.value
        val list = current[key] ?: return
        saveLog(current + (key to list.filterNot { it.id == id }))
    }

    @Synchronized
    fun clearDay(key: String) = saveLog(_log.value - key)

    // ---------- Soneca e controle de notificações ----------
    fun snooze(minutes: Long = SNOOZE_MIN) {
        val until = System.currentTimeMillis() + minutes * 60_000
        prefs.edit().putLong(KEY_SNOOZE, until).apply()
        _snoozeUntil.value = until
    }

    var lastNotifAt: Long
        get() = prefs.getLong(KEY_LAST_NOTIF, 0L)
        set(v) = prefs.edit().putLong(KEY_LAST_NOTIF, v).apply()

    var lastNotifKind: String?
        get() = prefs.getString(KEY_LAST_KIND, null)
        set(v) = prefs.edit().putString(KEY_LAST_KIND, v).apply()

    private fun newId() = java.lang.Long.toString(System.currentTimeMillis(), 36) +
        (1..4).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")

    companion object {
        const val SNOOZE_MIN = 30L
        private const val KEY_SETTINGS = "settings"
        private const val KEY_LOG = "log"
        private const val KEY_SNOOZE = "snooze"
        private const val KEY_LAST_NOTIF = "lastNotif"
        private const val KEY_LAST_KIND = "lastNotifKind"

        @Volatile private var instance: HidrataRepository? = null

        fun get(context: Context): HidrataRepository =
            instance ?: synchronized(this) {
                instance ?: HidrataRepository(context).also { instance = it }
            }
    }
}
