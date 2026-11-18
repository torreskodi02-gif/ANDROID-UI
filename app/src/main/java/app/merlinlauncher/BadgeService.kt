package app.merlinlauncher

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Badge counts for Home / Drawer icons. Counts ACTIVE notifications only —
 *  it never reads notification content, titles, or message text. The user
 *  must grant "Notification access" in system Settings; until then counts
 *  stay empty and badges simply do not show. */
class BadgeService : NotificationListenerService() {
    companion object {
        private val counts = HashMap<String, Int>()

        fun countFor(pkg: String): Int = synchronized(counts) { counts[pkg] ?: 0 }

        fun refreshFrom(active: Array<StatusBarNotification>?) {
            synchronized(counts) {
                counts.clear()
                if (active != null) {
                    for (sbn in active) {
                        val pkg = sbn.packageName ?: continue
                        counts[pkg] = (counts[pkg] ?: 0) + 1
                    }
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try { refreshFrom(activeNotifications) } catch (e: Exception) { }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        try { refreshFrom(activeNotifications) } catch (e: Exception) { }
    }

    override fun onListenerConnected() {
        try { refreshFrom(activeNotifications) } catch (e: Exception) { }
    }
}
