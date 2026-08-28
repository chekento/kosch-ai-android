package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExternalHandoffGateTest {
    private fun candidate(
        target: String = "ai:example",
        packageName: String = "example.ai",
        prompt: String = "Analyse this",
    ) = AiExternalHandoffCandidate(target, packageName, prompt)

    @Test
    fun firstGestureOnlyStagesAndSecondIdenticalGestureConfirms() {
        val gate = AiExternalHandoffGate()
        val payload = candidate()

        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(payload))
        assertTrue(gate.hasPending())
        assertEquals(AiExternalHandoffDecision.CONFIRMED, gate.evaluate(payload))
        assertFalse(gate.hasPending())
    }

    @Test
    fun promptChangeRequiresASecondGestureAgain() {
        val gate = AiExternalHandoffGate()

        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(candidate(prompt = "First")))
        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(candidate(prompt = "Second")))
        assertEquals(AiExternalHandoffDecision.CONFIRMED, gate.evaluate(candidate(prompt = "Second")))
    }

    @Test
    fun destinationChangeRequiresASecondGestureAgain() {
        val gate = AiExternalHandoffGate()

        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(candidate(target = "ai:first", packageName = "first.ai")))
        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(candidate(target = "ai:second", packageName = "second.ai")))
        assertEquals(AiExternalHandoffDecision.CONFIRMED, gate.evaluate(candidate(target = "ai:second", packageName = "second.ai")))
    }

    @Test
    fun clearCancelsPendingConfirmation() {
        val gate = AiExternalHandoffGate()
        val payload = candidate()

        gate.evaluate(payload)
        gate.clear()

        assertFalse(gate.hasPending())
        assertEquals(AiExternalHandoffDecision.STAGED, gate.evaluate(payload))
    }
}
