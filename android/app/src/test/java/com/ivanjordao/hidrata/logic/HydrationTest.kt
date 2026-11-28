package com.ivanjordao.hidrata.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Testes das regras de meta. Rodam no computador (ou no GitHub Actions), sem celular. */
class HydrationTest {
    private val s = Settings(goalMl = 3200, wake = "06:00", sleep = "22:00", notify = true)

    @Test
    fun diaLogicoComecaNoMeioDoSono() {
        // Dorme 22h e acorda 6h → o dia lógico vira às 2h da manhã.
        val sch = Hydration.schedule(s, LocalDateTime.of(2026, 11, 28, 1, 30))
        assertEquals("2026-11-27", sch.key)
        val sch2 = Hydration.schedule(s, LocalDateTime.of(2026, 11, 28, 2, 30))
        assertEquals("2026-11-28", sch2.key)
        assertEquals(LocalDateTime.of(2026, 11, 28, 6, 0), sch2.wakeAt)
        assertEquals(LocalDateTime.of(2026, 11, 28, 22, 0), sch2.sleepAt)
    }

    @Test
    fun metaProporcionalAoHorario() {
        val sch = Hydration.schedule(s, LocalDateTime.of(2026, 11, 28, 14, 0))
        // 14h é a metade entre 6h e 22h → metade da meta
        assertEquals(1600, Hydration.expectedAt(LocalDateTime.of(2026, 11, 28, 14, 0), sch, 3200))
        assertEquals(0, Hydration.expectedAt(LocalDateTime.of(2026, 11, 28, 5, 0), sch, 3200))
        assertEquals(3200, Hydration.expectedAt(LocalDateTime.of(2026, 11, 28, 23, 0), sch, 3200))
    }

    @Test
    fun quemDormeDepoisDaMeiaNoite() {
        val night = Settings(2500, "09:00", "01:00", true)
        // 00:30 ainda pertence ao dia anterior
        val sch = Hydration.schedule(night, LocalDateTime.of(2026, 11, 29, 0, 30))
        assertEquals("2026-11-28", sch.key)
        assertEquals(LocalDateTime.of(2026, 11, 29, 1, 0), sch.sleepAt)
    }

    @Test
    fun estados() {
        val now = LocalDateTime.of(2026, 11, 28, 14, 0)
        val key = Hydration.schedule(s, now).key
        fun log(ml: Int) = mapOf(key to listOf(Entry("a", 0, ml)))
        assertEquals(DayState.BEHIND, Hydration.compute(s, log(1000), now).state)
        assertEquals(DayState.OK, Hydration.compute(s, log(2000), now).state)
        assertEquals(DayState.DONE, Hydration.compute(s, log(3200), now).state)
        assertEquals(DayState.IDLE, Hydration.compute(s, emptyMap(), LocalDateTime.of(2026, 11, 28, 5, 0)).state)
    }

    @Test
    fun horaEmQueFicaraAbaixo() {
        val sch = Hydration.schedule(s, LocalDateTime.of(2026, 11, 28, 10, 0))
        // 1.600 ml bebidos → só fica abaixo às 14h
        val at = Hydration.behindAt(1600, sch, 3200)
        assertEquals(LocalDateTime.of(2026, 11, 28, 14, 0), at)
        assertTrue(Hydration.fmt(1600) == "1.600 ml")
    }
}
