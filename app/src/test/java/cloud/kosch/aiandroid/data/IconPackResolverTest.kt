package cloud.kosch.aiandroid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconPackResolverTest {
    @Test
    fun appfilterComponentInfoAndShortClassNormalizeToSameKey() {
        assertEquals(
            "com.example.app/com.example.app.MainActivity",
            IconPackComponentKey.normalize("ComponentInfo{com.example.app/.MainActivity}"),
        )
        assertEquals(
            "com.example.app/com.example.app.MainActivity",
            IconPackComponentKey.normalize("com.example.app/com.example.app.MainActivity"),
        )
    }

    @Test
    fun malformedOrUnsafeComponentNamesFailClosed() {
        assertNull(IconPackComponentKey.normalize("com.example.app"))
        assertNull(IconPackComponentKey.normalize("ComponentInfo{../bad/.Main}"))
        assertNull(IconPackComponentKey.normalize("ComponentInfo{com.example.app/}"))
    }
}
