package cloud.kosch.aiandroid.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiEverywhereFeatureCatalogTest {
    @Test
    fun idsStayUniqueAndCatalogIsBroad() {
        val ids = AiEverywhereFeatureCatalog.features.map(AiEverywhereFeature::id)
        assertTrue(ids.size >= 20)
        assertTrue(ids.distinct().size == ids.size)
    }

    @Test
    fun sensitiveExternalFeaturesRequirePreview() {
        val externalSensitive = AiEverywhereFeatureCatalog.features.filter {
            it.privacyClass == AiPrivacyClass.HIGHLY_SENSITIVE &&
                AiFeatureExecution.INSTALLED_APP_HANDOFF in it.executions
        }
        assertTrue(externalSensitive.isNotEmpty())
        assertTrue(externalSensitive.all(AiEverywhereFeature::externalPreviewRequired))
    }

    @Test
    fun screenAndCameraAreNeverBackgroundSafe() {
        val ids = setOf("screen.ask", "camera.ask")
        assertTrue(AiEverywhereFeatureCatalog.features.filter { it.id in ids }.all { !it.backgroundSafe })
    }

    @Test
    fun appHubCanWorkWithoutCloudOrApiKey() {
        val feature = AiEverywhereFeatureCatalog.features.first { it.id == "apps.ai_hub" }
        assertTrue(AiFeatureExecution.LOCAL_RULES in feature.executions)
        assertTrue(AiFeatureExecution.APP_SHORTCUT_OR_WIDGET in feature.executions)
        assertFalse(AiFeatureExecution.INSTALLED_APP_HANDOFF in feature.executions)
    }
}
