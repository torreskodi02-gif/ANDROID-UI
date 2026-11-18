package app.merlinlauncher

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.LruCache
import android.widget.EditText
import android.widget.Toast

data class AppInfo(val label: String, val pkg: String, val cls: String, val icon: Drawable?)

object Apps {
    private val iconCache = LruCache<String, Drawable>(64)

    /** All launchable apps, A-Z, hidden apps removed, label overrides applied. */
    fun loadApps(ctx: Context, prefs: Prefs): List<AppInfo> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hidden = prefs.hiddenApps()
        val out = ArrayList<AppInfo>()
        try {
            val infos = pm.queryIntentActivities(intent, 0)
            for (ri in infos) {
                val ai = ri.activityInfo ?: continue
                if (hidden.contains(ai.packageName)) continue
                val label = prefs.labelOverride(ai.packageName) ?: ri.loadLabel(pm)?.toString() ?: ai.packageName
                val icon = iconCache.get(ai.packageName) ?: ri.loadIcon(pm)?.also { iconCache.put(ai.packageName, it) }
                out.add(AppInfo(label, ai.packageName, ai.name, icon))
            }
        } catch (e: Exception) {
            // Never crash the launcher because the package list failed.
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    /** Sort for the drawer: A-Z, most-used (usage counts in prefs), or grouped by category. */
    fun sortedForDrawer(apps: List<AppInfo>, prefs: Prefs): List<AppInfo> {
        return when (prefs.drawerSort) {
            1 -> apps.sortedByDescending { prefs.usage(it.pkg) }
            2 -> apps.sortedWith(compareBy({ categoryOf(it) }, { it.label.lowercase() }))
            else -> apps
        }
    }

    fun categoryOf(app: AppInfo): String {
        val s = (app.pkg + " " + app.label).lowercase()
        return when {
            listOf("whatsapp", "telegram", "signal", "gmail", "outlook", "messages", "messenger", "mail", "discord", "slack", "duo", "meet").any { s.contains(it) } -> "Communication"
            listOf("youtube", "spotify", "music", "netflix", "player", "video", "tube", "podcast", "radio", "gallery", "photos", "camera").any { s.contains(it) } -> "Media"
            listOf("game", "chess", "puzzle", "play").any { s.contains(it) } -> "Games"
            listOf("instagram", "facebook", "twitter", "x.com", "tiktok", "snapchat", "reddit", "threads").any { s.contains(it) } -> "Social"
            listOf("chrome", "firefox", "browser", "maps", "drive", "docs", "calendar", "clock", "calculator", "files", "settings", "bank", "pay").any { s.contains(it) } -> "Tools"
            else -> "Other"
        }
    }

    fun launch(ctx: Context, app: AppInfo, prefs: Prefs) {
        prefs.bumpUsage(app.pkg)
        try {
            val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            i.component = ComponentName(app.pkg, app.cls)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            ctx.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    /** Shared long-press menu for Home and Drawer. onChange is called after any mutation. */
    fun showAppMenu(activity: Activity, app: AppInfo, prefs: Prefs, onChange: () -> Unit) {
        val favs = prefs.favourites()
        val isFav = favs.contains(app.pkg)
        val home = prefs.homeApps()
        val onHome = home.contains(app.pkg)
        val items = arrayOf(
            "App info",
            "Uninstall",
            if (isFav) "Remove from favourites" else "Add to favourites",
            if (onHome) "Remove from Home" else "Add to Home",
            "Rename label",
            "Hide app"
        )
        AlertDialog.Builder(activity)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            activity.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + app.pkg)))
                        } catch (e: Exception) { }
                    }
                    1 -> {
                        try {
                            activity.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg)))
                        } catch (e: Exception) { }
                    }
                    2 -> {
                        if (isFav) favs.remove(app.pkg) else favs.add(app.pkg)
                        prefs.setFavourites(favs); onChange()
                    }
                    3 -> {
                        if (onHome) home.remove(app.pkg) else home.add(app.pkg)
                        prefs.setHomeApps(home); onChange()
                    }
                    4 -> {
                        val et = EditText(activity)
                        et.setText(app.label)
                        et.setSelection(et.text.length)
                        AlertDialog.Builder(activity).setTitle("Rename ${app.label}")
                            .setView(et)
                            .setPositiveButton("Save") { _, _ ->
                                prefs.setLabelOverride(app.pkg, et.text.toString().trim())
                                onChange()
                            }
                            .setNegativeButton("Reset") { _, _ ->
                                prefs.setLabelOverride(app.pkg, null)
                                onChange()
                            }
                            .show()
                    }
                    5 -> {
                        if (prefs.lockLayout) {
                            Toast.makeText(activity, "Layout is locked — unlock it in Settings first", Toast.LENGTH_SHORT).show()
                        } else {
                            val h = prefs.hiddenApps(); h.add(app.pkg); prefs.setHidden(h); onChange()
                            Toast.makeText(activity, "${app.label} hidden (restore it in Settings > Drawer)", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }
}
