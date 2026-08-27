package cloud.kosch.aiandroid.ai

import cloud.kosch.aiandroid.model.AssistantObservationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AssistantVisualContextRequestParserTest {
    @Test
    fun genericVisualQuestion_usesCurrentVisibleSource() {
        val request = AssistantVisualContextRequestParser.parseRequest("Was siehst du gerade?")
        assertNotNull(request)
        assertNull(request?.source)
    }

    @Test
    fun explicitScreenQuestion_targetsScreen() {
        assertEquals(
            AssistantObservationSource.SCREEN,
            AssistantVisualContextRequestParser.parseRequest("Analysiere bitte meinen Bildschirm")?.source,
        )
    }

    @Test
    fun explicitCameraQuestion_targetsCamera() {
        assertEquals(
            AssistantObservationSource.CAMERA,
            AssistantVisualContextRequestParser.parseRequest("Schau dir das Kamerabild an")?.source,
        )
    }

    @Test
    fun ordinaryCameraCommand_isNotMisclassifiedAsVisionRequest() {
        assertNull(AssistantVisualContextRequestParser.parseRequest("Öffne Kamera"))
        assertNull(AssistantVisualContextRequestParser.parseRequest("Kamera"))
    }
}
