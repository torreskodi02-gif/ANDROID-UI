package app.merlinlauncher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/** Default-home (ROLE_HOME) helpers.
 *
 *  Preferred path on API 29+: RoleManager.requestRole(ROLE_HOME), which shows
 *  the system "set as default Home" dialog. Fallback: the system Home
 *  chooser intent. isDefaultLauncher() prefers RoleManager.isRoleHeld and
 *  falls back to resolving the HOME intent and comparing packages. */
object DefaultHome {
    const val REQUEST_DEFAULT_HOME = 9001

    fun isDefaultLauncher(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = ctx.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return rm.isRoleHeld(RoleManager.ROLE_HOME)
                }
            } catch (e: Exception) {
            }
        }
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val ri = ctx.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ri?.activityInfo?.packageName == ctx.packageName
        } catch (e: Exception) {
            false
        }
    }

    /** Starts the default-home request. Returns true if a request was launched.
     *  For the RoleManager path the result arrives in the activity's
     *  onActivityResult with requestCode REQUEST_DEFAULT_HOME. */
    fun requestDefaultHome(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val rm = activity.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    activity.startActivityForResult(
                        rm.createRequestRoleIntent(RoleManager.ROLE_HOME),
                        REQUEST_DEFAULT_HOME
                    )
                    return true
                }
            } catch (e: Exception) {
            }
        }
        return try {
            val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            activity.startActivity(Intent.createChooser(home, "Choose your Home app"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
