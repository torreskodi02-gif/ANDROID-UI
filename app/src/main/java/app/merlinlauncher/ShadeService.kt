package app.merlinlauncher

import android.accessibilityservice.AccessibilityService
import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/** Double-tap-to-lock device admin. Declaring this receiver is NOT enough:
 *  the user must enable it once in system Settings (the launcher offers a
 *  button that opens the device-admin activation screen). Without that,
 *  lockNow() is refused by the system. */
class LockAdmin : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {}
    override fun onDisabled(context: Context, intent: Intent) {}
}

/** Notification-shade swipe-down stub.
 *
 *  HONEST LIMIT: a launcher cannot pull the notification shade down with a
 *  normal API. The only way is an AccessibilityService, and it only works
 *  AFTER the user grants Merlin Launcher access in system Settings >
 *  Accessibility. Until granted, requestShade() does nothing and the
 *  Settings screen says so plainly. */
class ShadeService : AccessibilityService() {
    companion object {
        var instance: ShadeService? = null
            private set

        /** Ask the bound service to pull the shade down. Returns false if
         *  the service is not granted/running — the caller should then
         *  offer to open Accessibility settings instead of pretending. */
        fun requestShade(): Boolean {
            val s = instance ?: return false
            return try {
                s.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We do not observe, read, or act on any accessibility events.
        // This service exists only to perform the shade pull-down action.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
