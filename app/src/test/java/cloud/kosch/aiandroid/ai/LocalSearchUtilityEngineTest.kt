package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSearchUtilityEngineTest {
    @Test
    fun `calculator respects precedence parentheses powers and percentages`() {
        val basic = LocalSearchUtilityEngine.evaluate("2 + 3 * 4") as LocalSearchUtilityResult.Calculation
        assertEquals(14.0, basic.value, 0.000001)

        val grouped = LocalSearchUtilityEngine.evaluate("(2 + 3) * 4") as LocalSearchUtilityResult.Calculation
        assertEquals(20.0, grouped.value, 0.000001)

        val power = LocalSearchUtilityEngine.evaluate("2 ^ 3 ^ 2") as LocalSearchUtilityResult.Calculation
        assertEquals(512.0, power.value, 0.000001)

        val percent = LocalSearchUtilityEngine.evaluate("25% * 200") as LocalSearchUtilityResult.Calculation
        assertEquals(50.0, percent.value, 0.000001)
    }

    @Test
    fun `metric imperial and data conversions stay local and deterministic`() {
        val distance = LocalSearchUtilityEngine.evaluate("10 km in mi") as LocalSearchUtilityResult.Conversion
        assertEquals(6.21371192, distance.value, 0.00001)

        val mass = LocalSearchUtilityEngine.evaluate("2 lb to kg") as LocalSearchUtilityResult.Conversion
        assertEquals(0.90718474, mass.value, 0.000001)

        val data = LocalSearchUtilityEngine.evaluate("2 GiB to MB") as LocalSearchUtilityResult.Conversion
        assertEquals(2147.483648, data.value, 0.000001)
    }

    @Test
    fun `temperature conversion handles affine units`() {
        val f = LocalSearchUtilityEngine.evaluate("0 c to f") as LocalSearchUtilityResult.Conversion
        assertEquals(32.0, f.value, 0.000001)

        val c = LocalSearchUtilityEngine.evaluate("32 f nach c") as LocalSearchUtilityResult.Conversion
        assertEquals(0.0, c.value, 0.000001)

        val k = LocalSearchUtilityEngine.evaluate("100 c to k") as LocalSearchUtilityResult.Conversion
        assertEquals(373.15, k.value, 0.000001)
    }

    @Test
    fun `unsafe or nonsensical expressions fail closed`() {
        assertNull(LocalSearchUtilityEngine.evaluate("Runtime.exec('x')"))
        assertNull(LocalSearchUtilityEngine.evaluate("1 / 0"))
        assertNull(LocalSearchUtilityEngine.evaluate("1 kg to m"))
        assertNull(LocalSearchUtilityEngine.evaluate("2 ^ 1000"))
        assertNull(LocalSearchUtilityEngine.evaluate("javascript:alert(1)"))
    }

    @Test
    fun `display output is bounded and human readable`() {
        val result = LocalSearchUtilityEngine.evaluate("1 / 3") as LocalSearchUtilityResult.Calculation
        assertTrue(result.display.startsWith("0.333"))
        assertTrue(result.display.length < 32)
    }
}
