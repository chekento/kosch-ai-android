package cloud.kosch.aiandroid.ai

/**
 * Pure routing decision for AI/provider cards.
 *
 * KoSch never guesses undocumented deep links. If a known provider is installed the launcher uses its normal
 * launch/share surface. If it is missing and an official Play package is known, the user is sent to that Play
 * listing. Web/source fallback is reserved for providers without a verified store package.
 */
sealed interface AiProviderDestination {
    data class Installed(val appKey: String) : AiProviderDestination
    data class PlayStore(val packageName: String) : AiProviderDestination
    data class Web(val url: String) : AiProviderDestination
}

object AiProviderRoutePolicy {
    fun destination(
        provider: AiProviderProfile,
        installedAppKey: String?,
    ): AiProviderDestination = when {
        !installedAppKey.isNullOrBlank() -> AiProviderDestination.Installed(installedAppKey)
        !provider.playStorePackageName.isNullOrBlank() ->
            AiProviderDestination.PlayStore(provider.playStorePackageName)
        else -> AiProviderDestination.Web(provider.webUrl)
    }
}
