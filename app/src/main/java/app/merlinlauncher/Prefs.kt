package app.merlinlauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** All Merlin Launcher settings live in SharedPreferences "ml". */
class Prefs(ctx: Context) {
    val p = ctx.applicationContext.getSharedPreferences("ml", Context.MODE_PRIVATE)

    var homeStyle: Int get() = p.getInt("homeStyle", 0); set(v) = p.edit().putInt("homeStyle", v).apply() // 0 Grid, 1 List
    var themeMode: Int get() = p.getInt("themeMode", 0); set(v) = p.edit().putInt("themeMode", v).apply() // 0 Auto, 1 Light, 2 Dark
    var theme: Int get() = p.getInt("theme", 0); set(v) = p.edit().putInt("theme", v).apply() // palette index into Themes.PALETTES
    var accent: Int get() = p.getInt("accent", 0); set(v) = p.edit().putInt("accent", v).apply() // 0 = palette accent, else index into Themes.ACCENTS
    var dynamicColor: Boolean get() = p.getBoolean("dynamicColor", false); set(v) = p.edit().putBoolean("dynamicColor", v).apply()
    var gridRows: Int get() = p.getInt("gridRows", 6); set(v) = p.edit().putInt("gridRows", v.coerceIn(3, 8)).apply()
    var gridCols: Int get() = p.getInt("gridCols", 4); set(v) = p.edit().putInt("gridCols", v.coerceIn(3, 6)).apply()
    var iconSize: Int get() = p.getInt("iconSize", 1); set(v) = p.edit().putInt("iconSize", v.coerceIn(0, 2)).apply() // 0 Small, 1 Medium, 2 Large
    var showLabels: Boolean get() = p.getBoolean("showLabels", true); set(v) = p.edit().putBoolean("showLabels", v).apply()
    var dockShow: Boolean get() = p.getBoolean("dockShow", true); set(v) = p.edit().putBoolean("dockShow", v).apply()
    var dockCount: Int get() = p.getInt("dockCount", 5); set(v) = p.edit().putInt("dockCount", v.coerceIn(3, 7)).apply()
    var drawerSort: Int get() = p.getInt("drawerSort", 0); set(v) = p.edit().putInt("drawerSort", v).apply() // 0 A-Z, 1 Most-used, 2 Category
    var drawerCols: Int get() = p.getInt("drawerCols", 4); set(v) = p.edit().putInt("drawerCols", v.coerceIn(3, 6)).apply()
    var iconPack: String get() = p.getString("iconPack", "") ?: ""; set(v) = p.edit().putString("iconPack", v).apply()
    var iconShape: Int get() = p.getInt("iconShape", 1); set(v) = p.edit().putInt("iconShape", v).apply() // 0 Circle, 1 Squircle, 2 Rounded, 3 Square
    var themedIcons: Boolean get() = p.getBoolean("themedIcons", true); set(v) = p.edit().putBoolean("themedIcons", v).apply()
    var badges: Int get() = p.getInt("badges", 1); set(v) = p.edit().putInt("badges", v).apply() // 0 Off, 1 Dot, 2 Count
    var gestureSwipeDown: Int get() = p.getInt("gestureSwipeDown", 0); set(v) = p.edit().putInt("gestureSwipeDown", v).apply() // 0 Search, 1 Drawer
    var doubleTapLock: Boolean get() = p.getBoolean("doubleTapLock", false); set(v) = p.edit().putBoolean("doubleTapLock", v).apply()
    var lockLayout: Boolean get() = p.getBoolean("lockLayout", false); set(v) = p.edit().putBoolean("lockLayout", v).apply()
    var animSpeed: Int get() = p.getInt("animSpeed", 1); set(v) = p.edit().putInt("animSpeed", v).apply() // 0 Off, 1 Normal, 2 Fast
    var feedShow: Boolean get() = p.getBoolean("feedShow", true); set(v) = p.edit().putBoolean("feedShow", v).apply()
    var preset: String get() = p.getString("preset", "Minimal") ?: "Minimal"; set(v) = p.edit().putString("preset", v).apply()
    var notes: String get() = p.getString("notes", "") ?: ""; set(v) = p.edit().putString("notes", v).apply()

    fun hiddenApps(): MutableSet<String> = HashSet(p.getStringSet("hiddenApps", emptySet()) ?: emptySet())
    fun favourites(): MutableSet<String> = HashSet(p.getStringSet("favourites", emptySet()) ?: emptySet())
    fun homeApps(): MutableSet<String> = HashSet(p.getStringSet("homeApps", emptySet()) ?: emptySet())
    fun setHidden(s: Set<String>) = p.edit().putStringSet("hiddenApps", s).apply()
    fun setFavourites(s: Set<String>) = p.edit().putStringSet("favourites", s).apply()
    fun setHomeApps(s: Set<String>) = p.edit().putStringSet("homeApps", s).apply()

    fun labelOverride(pkg: String): String? = p.getString("label_$pkg", null)
    fun setLabelOverride(pkg: String, label: String?) {
        val e = p.edit()
        if (label.isNullOrBlank()) e.remove("label_$pkg") else e.putString("label_$pkg", label)
        e.apply()
    }

    fun usage(pkg: String): Int = p.getInt("usage_$pkg", 0)
    fun bumpUsage(pkg: String) = p.edit().putInt("usage_$pkg", usage(pkg) + 1).apply()

    /** Presets set a coherent bundle of values. Minimal is the v1 default. */
    fun applyPreset(name: String) {
        preset = name
        val e = p.edit()
        when (name) {
            "Minimal" -> {
                e.putInt("homeStyle", 1); e.putInt("theme", 5); e.putInt("themeMode", 2)
                e.putInt("gridRows", 5); e.putInt("gridCols", 4); e.putInt("iconSize", 1)
                e.putBoolean("showLabels", false); e.putBoolean("dockShow", true); e.putInt("dockCount", 5)
                e.putInt("drawerSort", 0); e.putInt("iconShape", 1); e.putBoolean("themedIcons", true)
                e.putInt("badges", 1); e.putInt("animSpeed", 1); e.putBoolean("feedShow", true)
            }
            "Classic" -> {
                e.putInt("homeStyle", 0); e.putInt("theme", 1); e.putInt("themeMode", 0)
                e.putInt("gridRows", 6); e.putInt("gridCols", 4); e.putInt("iconSize", 1)
                e.putBoolean("showLabels", true); e.putBoolean("dockShow", true); e.putInt("dockCount", 5)
                e.putInt("drawerSort", 0); e.putInt("iconShape", 2); e.putBoolean("themedIcons", false)
                e.putInt("badges", 2); e.putInt("animSpeed", 1); e.putBoolean("feedShow", true)
            }
            "Dense" -> {
                e.putInt("homeStyle", 0); e.putInt("theme", 0); e.putInt("themeMode", 2)
                e.putInt("gridRows", 8); e.putInt("gridCols", 6); e.putInt("iconSize", 0)
                e.putBoolean("showLabels", true); e.putBoolean("dockShow", true); e.putInt("dockCount", 7)
                e.putInt("drawerSort", 0); e.putInt("drawerCols", 6); e.putInt("iconShape", 3)
                e.putBoolean("themedIcons", true); e.putInt("badges", 2); e.putInt("animSpeed", 2)
                e.putBoolean("feedShow", false)
            }
            "One-handed" -> {
                e.putInt("homeStyle", 1); e.putInt("theme", 0); e.putInt("themeMode", 2)
                e.putInt("gridRows", 4); e.putInt("gridCols", 4); e.putInt("iconSize", 2)
                e.putBoolean("showLabels", true); e.putBoolean("dockShow", true); e.putInt("dockCount", 4)
                e.putInt("drawerSort", 1); e.putInt("iconShape", 0); e.putBoolean("themedIcons", true)
                e.putInt("badges", 1); e.putInt("animSpeed", 1); e.putBoolean("feedShow", true)
            }
        }
        e.apply()
    }

    /** Full settings backup as JSON (org.json is built into Android). */
    fun backupToJson(): String {
        val o = JSONObject()
        val keys = listOf("homeStyle", "themeMode", "theme", "accent", "dynamicColor", "gridRows", "gridCols",
            "iconSize", "showLabels", "dockShow", "dockCount", "drawerSort", "drawerCols", "iconPack",
            "iconShape", "themedIcons", "badges", "gestureSwipeDown", "doubleTapLock", "lockLayout",
            "animSpeed", "feedShow", "preset", "notes")
        for (k in keys) {
            val v = p.all[k] ?: continue
            when (v) {
                is Boolean -> o.put(k, v)
                is Int -> o.put(k, v)
                is String -> o.put(k, v)
            }
        }
        val sets = JSONObject()
        sets.put("hiddenApps", JSONArray(hiddenApps().toList()))
        sets.put("favourites", JSONArray(favourites().toList()))
        sets.put("homeApps", JSONArray(homeApps().toList()))
        o.put("sets", sets)
        val labels = JSONObject()
        for ((k, v) in p.all) if (k.startsWith("label_") && v is String) labels.put(k, v)
        o.put("labels", labels)
        val usage = JSONObject()
        for ((k, v) in p.all) if (k.startsWith("usage_") && v is Int) usage.put(k, v)
        o.put("usage", usage)
        o.put("app", "merlin-launcher"); o.put("version", 1)
        return o.toString(2)
    }

    /** Restore a backup produced by backupToJson(). Returns false on bad input. Never crashes. */
    fun restoreFromJson(json: String): Boolean {
        return try {
            val o = JSONObject(json)
            if (o.optString("app", "") != "merlin-launcher") return false
            val e = p.edit()
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                if (k == "sets" || k == "labels" || k == "usage" || k == "app" || k == "version") continue
                when (val v = o.get(k)) {
                    is Boolean -> e.putBoolean(k, v)
                    is Int -> e.putInt(k, v)
                    is String -> e.putString(k, v)
                }
            }
            val sets = o.optJSONObject("sets")
            if (sets != null) {
                for (name in listOf("hiddenApps", "favourites", "homeApps")) {
                    val arr = sets.optJSONArray(name) ?: continue
                    val s = HashSet<String>()
                    for (i in 0 until arr.length()) s.add(arr.optString(i))
                    e.putStringSet(name, s)
                }
            }
            val labels = o.optJSONObject("labels")
            if (labels != null) {
                val lit = labels.keys()
                while (lit.hasNext()) { val k = lit.next(); e.putString(k, labels.optString(k)) }
            }
            val usage = o.optJSONObject("usage")
            if (usage != null) {
                val uit = usage.keys()
                while (uit.hasNext()) { val k = uit.next(); e.putInt(k, usage.optInt(k)) }
            }
            e.apply()
            true
        } catch (ex: Exception) {
            false
        }
    }
}
