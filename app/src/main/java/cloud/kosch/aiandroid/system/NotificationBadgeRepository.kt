package cloud.kosch.aiandroid.system

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Process-local package counters. Titles, message text, people and notification extras are never copied.
 */
object NotificationBadgeRepository {
    fun interface Listener {
        fun onBadgeCountsChanged(counts: Map<String, Int>)
    }

    private val counts = ConcurrentHashMap<String, Int>()
    private val listeners = CopyOnWriteArraySet<Listener>()

    fun snapshot(): Map<String, Int> = counts.toMap()

    fun addListener(listener: Listener) {
        listeners += listener
        listener.onBadgeCountsChanged(snapshot())
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    internal fun publish(notifications: Array<StatusBarNotification>, ownPackage: String) {
        val updated = notifications.asSequence()
            .filterNot { it.packageName == ownPackage }
            .filterNot { it.notification.flags and Notification.FLAG_ONGOING_EVENT != 0 }
            .filterNot { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
            .groupingBy(StatusBarNotification::getPackageName)
            .eachCount()

        counts.clear()
        counts.putAll(updated)
        val immutable = counts.toMap()
        listeners.forEach { it.onBadgeCountsChanged(immutable) }
    }

    internal fun clear() {
        counts.clear()
        listeners.forEach { it.onBadgeCountsChanged(emptyMap()) }
    }
}

class KoSchNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = refresh()

    override fun onListenerDisconnected() {
        NotificationBadgeRepository.clear()
    }

    private fun refresh() {
        val notifications = runCatching { activeNotifications }.getOrNull() ?: return
        val rankingMap = runCatching { currentRanking }.getOrNull()
        val badgeable = notifications.filter { notification ->
            if (rankingMap == null) return@filter true
            val ranking = Ranking()
            val found = runCatching { rankingMap.getRanking(notification.key, ranking) }
                .getOrDefault(false)
            !found || ranking.canShowBadge()
        }.toTypedArray()
        NotificationBadgeRepository.publish(badgeable, packageName)
    }
}

object NotificationAccess {
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
}
