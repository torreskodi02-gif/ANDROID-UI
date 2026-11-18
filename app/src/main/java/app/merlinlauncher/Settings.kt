package app.merlinlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** Merlin Settings. Every control reads/writes Prefs — no dead toggles.
 *  Search box at the top filters rows by title. */
class SettingsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var container: LinearLayout
    private val rows = ArrayList<Pair<String, LinearLayout>>()

    internal fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val REQ_EXPORT = 1001
    private val REQ_IMPORT = 1002
    private val REQ_HOME_ROLE = 1003

    internal fun tv(s: String, sz: Float, col: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sz; setTextColor(col)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    internal fun btn(s: String, bg: Int, fg: Int, f: () -> Unit) = Button(this).apply {
        text = s; setAllCaps(false); setTextColor(fg); setPadding(dp(16), dp(10), dp(16), dp(10))
        val g = GradientDrawable(); g.setColor(bg); g.cornerRadius = dp(24).toFloat(); background = g
        setOnClickListener { f() }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_HOME_ROLE) {
            // RoleManager result: RESULT_OK means Merlin is now the default Home.
            if (resultCode == RESULT_OK) Toast.makeText(this, "Merlin is now your default launcher", Toast.LENGTH_SHORT).show()
            render() // refresh the "Set as default launcher" status row
            return
        }
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        when (requestCode) {
            REQ_EXPORT -> {
                try {
                    contentResolver.openOutputStream(uri)?.use { it.write(prefs.backupToJson().toByteArray(Charsets.UTF_8)) }
                    Toast.makeText(this, "Backup exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            REQ_IMPORT -> {
                try {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                    if (prefs.restoreFromJson(json)) {
                        Toast.makeText(this, "Backup restored", Toast.LENGTH_SHORT).show()
                        render()
                    } else {
                        Toast.makeText(this, "That file is not a Merlin Launcher backup", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun render() {
        val bg = Themes.bg(prefs, this)
        val card = Themes.card(prefs, this)
        val tx = Themes.text(prefs, this)
        val sub = Themes.sub(prefs, this)
        val accent = Themes.accent(prefs, this)
        rows.clear()

        val root = FrameLayout(this)
        root.setBackgroundColor(bg)
        setContentView(root)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(16), dp(12), dp(16), dp(8))
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        val title = tv("Merlin Settings", 24f, tx, true)
        main.addView(title)

        // Settings search.
        val search = EditText(this)
        search.hint = "Search settings…"
        search.setTextColor(tx); search.setHintTextColor(sub)
        val sg = GradientDrawable(); sg.setColor(card); sg.cornerRadius = dp(24).toFloat()
        search.background = sg
        search.setPadding(dp(18), dp(12), dp(18), dp(12))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { filterRows(s?.toString() ?: "") }
        })
        val slp = LinearLayout.LayoutParams(-1, -2); slp.setMargins(0, dp(10), 0, dp(4))
        main.addView(search, slp)

        val sv = ScrollView(this)
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        sv.addView(container)
        main.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---------- (0) Default launcher ----------
        // Sits at the top of the list so it is the first thing a new user sees.
        // Status refreshes on every render (including onResume after the role request).
        val isDefault = isDefaultHome()
        buttonRow("Set Merlin as default launcher",
            if (isDefault) "Merlin is your current default Home — taps below do nothing until you pick another launcher."
            else "Merlin is not your default Home yet — tap to set it.",
            if (isDefault) "Current default" else "Set as default") { requestDefaultHome() }

        // ---------- (1) Looks & Personalities ----------
        // Six personalities — each one visibly changes Home and the drawer.
        // Tapping a card applies it right away (via Personalities.apply when
        // that object exists, otherwise the built-in fallback below).
        section("Looks & Personalities")
        personalityCardsRow()
        buttonRow("Surprise me", "Pick a random personality — different from the one you have now.", "🎲 Surprise me") {
            val options = PERSONALITIES.indices.filter { it != prefs.personality }
            val pick = options.random()
            applyPersonality(pick)
            Toast.makeText(this, "Now rocking ${PERSONALITIES[pick].first}!", Toast.LENGTH_SHORT).show()
            render()
        }
        chipsRow("Clock style", listOf("Big", "Compact", "Date-first"), prefs.clockStyle) { prefs.clockStyle = it; render() }
        chipsRow("Search bar style", listOf("Pill", "Bar", "Minimal"), prefs.searchStyle) { prefs.searchStyle = it; render() }
        chipsRow("App tile style", listOf("Flat", "Glassy card", "Bold outline"), prefs.tileStyle) { prefs.tileStyle = it; render() }
        chipsRow("Drawer style", listOf("Grid", "List rows"), prefs.drawerStyle) { prefs.drawerStyle = it; render() }
        chipsRow("Text size", listOf("Small", "Normal", "Large"), prefs.fontScale) { prefs.fontScale = it; render() }
        stepperRow("Home dim", prefs.homeDim, 0, 100, 10) { prefs.homeDim = it }

        // ---------- (2) Look & Feel ----------
        section("Look & Feel")
        chipsRow("Theme palette", Themes.PALETTES.map { it.name }, prefs.theme) { prefs.theme = it; render() }
        chipsRow("Theme mode", listOf("Auto", "Light", "Dark"), prefs.themeMode) { prefs.themeMode = it; render() }
        chipsRow("Accent colour", listOf("Palette", "Blue", "Red", "Green", "Amber", "Pink", "Violet", "Sky", "Yellow"), prefs.accent) { prefs.accent = it; render() }
        switchRow("Dynamic colour from wallpaper", "Accent follows your wallpaper's average colour.", prefs.dynamicColor) { prefs.dynamicColor = it; render() }
        buttonRow("Wallpaper", "Open the system wallpaper picker.", "Choose wallpaper") {
            try { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) } catch (e: Exception) {
                Toast.makeText(this, "No wallpaper picker found", Toast.LENGTH_SHORT).show()
            }
        }

        // ---------- (3) Home ----------
        section("Home")
        chipsRow("Home style", listOf("Grid", "List"), prefs.homeStyle) { prefs.homeStyle = it; render() }
        pickerRow("Preset", "Applies a full bundle of settings at once.", listOf("Minimal", "Classic", "Dense", "One-handed"), listOf("Minimal", "Classic", "Dense", "One-handed").indexOf(prefs.preset)) {
            prefs.applyPreset(listOf("Minimal", "Classic", "Dense", "One-handed")[it])
            Toast.makeText(this, "Preset applied", Toast.LENGTH_SHORT).show()
            render()
        }
        switchRow("Lock layout", "Blocks hiding apps and moving Home items until unlocked.", prefs.lockLayout) { prefs.lockLayout = it }

        // ---------- (4) Home grid ----------
        section("Home grid")
        stepperRow("Grid columns", prefs.gridCols, 3, 6) { prefs.gridCols = it }
        stepperRow("Grid rows", prefs.gridRows, 3, 7) { prefs.gridRows = it }

        // ---------- (5) Icons ----------
        section("Icons")
        chipsRow("Icon size", listOf("Small", "Medium", "Large"), prefs.iconSize) { prefs.iconSize = it }
        switchRow("Show app labels", "Labels under Home and drawer icons.", prefs.showLabels) { prefs.showLabels = it }
        chipsRow("Label size", listOf("Small", "Medium", "Large"), prefs.labelSize) { prefs.labelSize = it }

        // ---------- (6) Drawer ----------
        section("Drawer")
        chipsRow("Drawer sort", listOf("A–Z", "Most used", "Category"), prefs.drawerSort) { prefs.drawerSort = it }
        stepperRow("Drawer columns", prefs.drawerCols, 3, 6) { prefs.drawerCols = it }
        buttonRow("Hidden apps", "Unhide apps you hid from the drawer. ${prefs.hiddenApps().size} hidden.", "Manage hidden apps") {
            val hidden = prefs.hiddenApps().toList()
            if (hidden.isEmpty()) { Toast.makeText(this, "No hidden apps", Toast.LENGTH_SHORT).show(); return@buttonRow }
            val labels = hidden.map { pkg ->
                val a = Apps.loadApps(this, prefs).find { it.pkg == pkg }
                a?.label ?: pkg
            }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Unhide an app")
                .setItems(labels) { _, which ->
                    val h = prefs.hiddenApps(); h.remove(hidden[which]); prefs.setHidden(h)
                    Toast.makeText(this, "${labels[which]} is back", Toast.LENGTH_SHORT).show()
                    render()
                }.show()
        }

        // ---------- (7) Dock ----------
        section("Dock")
        switchRow("Show dock", "App row at the bottom of Home.", prefs.dockShow) { prefs.dockShow = it }
        stepperRow("Dock apps", prefs.dockCount, 3, 7) { prefs.dockCount = it }
        chipsRow("Dock style", listOf("Floating", "Full-width bar", "Transparent"), prefs.dockStyle) { prefs.dockStyle = it }

        // ---------- (8) Home screen ----------
        section("Home screen")
        switchRow("Search bar on Home", "Search box at the top of the Home screen.", prefs.searchBarOnHome) { prefs.searchBarOnHome = it }
        switchRow("At a Glance", "Clock, date and weather summary at the top of Home.", prefs.showAtAGlance) { prefs.showAtAGlance = it }
        switchRow("Show date on Home", "Date line under the At-a-Glance clock.", prefs.showDateOnHome) { prefs.showDateOnHome = it }
        chipsRow("Clock format", listOf("12-hour", "24-hour"), if (prefs.clockFormat24) 1 else 0) { prefs.clockFormat24 = it == 1 }

        // ---------- (9) Icons & Folders ----------
        section("Icons & Folders")
        pickerRow("Icon pack", "Installed icon packs (ADW / Nova themes).", listOf("None") + IconPack.listIconPacks(this).map { it.label },
            run {
                val packs = IconPack.listIconPacks(this)
                val idx = packs.indexOfFirst { it.pkg == prefs.iconPack }
                if (prefs.iconPack.isBlank()) 0 else idx + 1
            }) {
            val packs = IconPack.listIconPacks(this)
            prefs.iconPack = if (it == 0) "" else packs[it - 1].pkg
            render()
        }
        chipsRow("Icon shape", listOf("Circle", "Squircle", "Rounded", "Square", "Teardrop", "Hexagon"), prefs.iconShape) { prefs.iconShape = it }
        switchRow("Themed icons", "Mask unthemed icons into the chosen shape on an accent-tinted background.", prefs.themedIcons) { prefs.themedIcons = it }
        buttonRow("Folders", "Cover folders arrive in v1.1. For now, long-press any app to add it to Home or Favourites.", "OK") { }

        // ---------- (10) Gestures ----------
        section("Gestures")
        chipsRow("Swipe down", listOf("Search", "App drawer"), prefs.gestureSwipeDown) { prefs.gestureSwipeDown = it }
        switchRow("Double-tap to lock", "Double-tap empty Home space to lock the screen. Needs device-admin — grant it below.", prefs.doubleTapLock) { prefs.doubleTapLock = it }
        buttonRow("Device admin for lock", "Required before double-tap lock can work.", "Grant device admin") {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(this, LockAdmin::class.java)
            if (dpm.isAdminActive(admin)) {
                Toast.makeText(this, "Device admin is already active", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets double-tap lock the screen.")
                    })
                } catch (e: Exception) { }
            }
        }

        // ---------- (11) Gestures & feel ----------
        section("Gestures & feel")
        switchRow("Swipe up for app drawer", "Swipe up from the bottom of Home to open the app drawer.", prefs.swipeUpForDrawer) { prefs.swipeUpForDrawer = it }
        switchRow("Haptics", "Small vibration on taps and long-presses.", prefs.haptics) { prefs.haptics = it }
        chipsRow("Animation speed", listOf("Off", "Normal", "Fast"), prefs.animSpeed) { prefs.animSpeed = it }

        // ---------- (12) Notifications ----------
        section("Notifications")
        chipsRow("Badges", listOf("Off", "Dot", "Count"), prefs.badges) { prefs.badges = it }
        buttonRow("Notification access", "Required for badges. Grants counts of active notifications only — content is never read.", "Grant notification access") {
            try { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (e: Exception) { }
        }
        buttonRow("Notification shade service", "Swipe-down shade pull needs an AccessibilityService, granted in system Settings. Until granted, the shade button does nothing — that is an Android limit, not a bug.", "Grant accessibility access") {
            try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } catch (e: Exception) { }
            Toast.makeText(this, "Find Merlin Launcher and turn it on", Toast.LENGTH_LONG).show()
        }
        buttonRow("Test shade pull-down", "Tries the AccessibilityService shade action now.", "Pull shade down") {
            if (ShadeService.requestShade()) Toast.makeText(this, "Shade requested", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Shade service not granted yet — use the button above", Toast.LENGTH_LONG).show()
        }

        // ---------- (13) Feed ----------
        section("Feed")
        switchRow("Show Feed", "Swipe left on Home for notes, recent apps and favourites.", prefs.feedShow) { prefs.feedShow = it }

        // ---------- (14) Backup ----------
        section("Backup")
        buttonRow("Export backup", "Saves all settings, favourites, hidden apps and notes as JSON.", "Export") {
            val i = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "merlin-launcher-backup.json")
            }
            try { startActivityForResult(i, REQ_EXPORT) } catch (e: Exception) {
                // Fallback: share the JSON as text.
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, prefs.backupToJson())
                }
                startActivity(Intent.createChooser(share, "Share backup JSON"))
            }
        }
        buttonRow("Import backup", "Restores a backup exported from this launcher.", "Import") {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            try { startActivityForResult(i, REQ_IMPORT) } catch (e: Exception) {
                Toast.makeText(this, "No file picker found", Toast.LENGTH_SHORT).show()
            }
        }

        // ---------- (15) About ----------
        section("About")
        infoRow("Merlin Launcher", "Version 1.0.0 · app.merlinlauncher")
        infoRow("Device", "Built for Motorola One Action — 6.3\" 1080×2520, 4 GB RAM, Android 11 (last update). Target API 30, min API 28.")
        infoRow("Honest limits",
            "• Cannot replace the lock screen, status bar, or notification shade — Android does not allow it.\n" +
            "• Gesture-nav Recents will be choppy without root; 3-button navigation is recommended.\n" +
            "• Shade pull-down needs the AccessibilityService granted in system Settings.\n" +
            "• Double-tap lock needs device-admin granted in system Settings.\n" +
            "• Badges need Notification access granted in system Settings.\n" +
            "• No weather: the At-a-Glance shows a placeholder until a widget host lands in v1.1.")
        infoRow("Not in v1 (planned for v1.1)",
            "Widget host · Shutters (widget preview on icon swipe) · Cover folders · Auto-backup schedule · Real weather.")

        // ---------- (16) Reset ----------
        section("Reset")
        buttonRow("Reset all settings", "Restores every setting to its default. Favourites, hidden apps, Home apps and notes are kept.", "Reset all settings") {
            AlertDialog.Builder(this).setTitle("Reset all settings?")
                .setMessage("Every Merlin Launcher setting returns to its default. Favourites, hidden apps, Home apps and notes are kept.")
                .setPositiveButton("Reset") { _, _ ->
                    prefs.resetAll()
                    Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show()
                    render()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    /** True when this app is currently the system's default Home app. */
    private fun isDefaultHome(): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            info?.activityInfo?.packageName == packageName
        } catch (e: Exception) {
            false
        }
    }

    /** Asks to make Merlin the default launcher.
     *  1. Tries MainActivity.requestDefaultHome() first, if that method exists
     *     (added by another agent) — reflectively, so Settings still compiles
     *     and runs when it does not exist yet.
     *  2. Otherwise uses RoleManager (API 29+, this phone is API 30), the
     *     standard way to request the Home role.
     *  3. Last resort: opens system Home settings and tells the user where to tap. */
    private fun requestDefaultHome() {
        if (tryMainActivityRequestDefaultHome()) return
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            try {
                val rm = getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
                if (rm.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)) {
                    startActivityForResult(
                        rm.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME), REQ_HOME_ROLE)
                    return
                }
            } catch (e: Exception) {
                // Fall through to system Home settings below.
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Open system Settings → Apps → Default apps → Home app to set Merlin as default", Toast.LENGTH_LONG).show()
        }
    }

    /** Reflectively calls MainActivity.requestDefaultHome() if it exists.
     *  Handles a static method requestDefaultHome() / (Context) / (Activity),
     *  and a companion-object method of the same name. Returns false when no
     *  such method exists, so the caller can fall back. Note: Settings runs in
     *  its own Activity, so there is no MainActivity instance to call an
     *  instance method on — the MainActivity hook must be static/companion. */
    private fun tryMainActivityRequestDefaultHome(): Boolean {
        return try {
            val cls = Class.forName("app.merlinlauncher.MainActivity")
            for (m in cls.declaredMethods) {
                if (m.name != "requestDefaultHome") continue
                if (!java.lang.reflect.Modifier.isStatic(m.modifiers)) continue
                m.isAccessible = true
                when (m.parameterTypes.size) {
                    0 -> m.invoke(null)
                    1 -> m.invoke(null, this)
                    else -> continue
                }
                return true
            }
            val companion = try { cls.getField("Companion").get(null) } catch (e: Exception) { null }
                ?: return false
            for (m in companion.javaClass.declaredMethods) {
                if (m.name != "requestDefaultHome") continue
                m.isAccessible = true
                when (m.parameterTypes.size) {
                    0 -> m.invoke(companion)
                    1 -> m.invoke(companion, this)
                    else -> continue
                }
                return true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun filterRows(q: String) {
        val needle = q.trim().lowercase()
        for ((title, view) in rows) {
            view.visibility = if (needle.isEmpty() || title.lowercase().contains(needle)) android.view.View.VISIBLE else android.view.View.GONE
        }
        // Section headers hide when none of their rows match.
        var currentHeader: TextView? = null
        var anyVisible = false
        for (i in 0 until container.childCount) {
            val v = container.getChildAt(i)
            if (v is TextView && v.tag == "section") {
                if (currentHeader != null) currentHeader.visibility = if (anyVisible) android.view.View.VISIBLE else android.view.View.GONE
                currentHeader = v; anyVisible = false
            } else if (v.visibility == android.view.View.VISIBLE) {
                anyVisible = true
            }
        }
        if (currentHeader != null) currentHeader.visibility = if (anyVisible) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun section(title: String) {
        val t = tv(title, 15f, Themes.text(prefs, this), true)
        t.tag = "section"
        t.setPadding(dp(4), dp(18), 0, dp(6))
        container.addView(t)
    }

    private fun addRow(title: String, desc: String, control: android.view.View) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(dp(4), dp(8), dp(4), dp(8))
        row.addView(tv(title, 15f, Themes.text(prefs, this), true))
        if (desc.isNotBlank()) row.addView(tv(desc, 12f, Themes.sub(prefs, this)))
        val cp = LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, dp(6), 0, 0)
        row.addView(control, cp)
        container.addView(row)
        rows.add(title + " " + desc to row)
    }

    private fun switchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val sw = Switch(this)
        sw.text = "On / Off"
        sw.setTextColor(Themes.text(prefs, this))
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        addRow(title, desc, sw)
    }

    private fun chipsRow(title: String, options: List<String>, sel: Int, onPick: (Int) -> Unit) {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        val tx = Themes.text(prefs, this)
        val card = Themes.card(prefs, this)
        val accent = Themes.accent(prefs, this)
        options.chunked(3).forEachIndexed { r, chunk ->
            val h = LinearLayout(this)
            chunk.forEachIndexed { i, s ->
                val k = r * 3 + i
                val b = btn(s, if (k == sel) accent else card, if (k == sel) Color.WHITE else tx) { onPick(k) }
                val lp = LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(dp(3), dp(3), dp(3), 0)
                h.addView(b, lp)
            }
            wrap.addView(h)
        }
        addRow(title, "", wrap)
    }

    private fun stepperRow(title: String, value: Int, min: Int, max: Int, step: Int = 1, onSet: (Int) -> Unit) {
        var cur = value
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val card = Themes.card(prefs, this)
        val tx = Themes.text(prefs, this)
        val valueView = tv(if (step > 1) "$cur%" else cur.toString(), 16f, tx, true)
        valueView.gravity = Gravity.CENTER
        val minus = btn("−", card, tx) {
            cur = (cur - step).coerceAtLeast(min)
            onSet(cur); valueView.text = if (step > 1) "$cur%" else cur.toString()
        }
        val plus = btn("+", card, tx) {
            cur = (cur + step).coerceAtMost(max)
            onSet(cur); valueView.text = if (step > 1) "$cur%" else cur.toString()
        }
        row.addView(minus)
        row.addView(valueView, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(plus)
        addRow(title, "", row)
    }

    private fun buttonRow(title: String, desc: String, label: String, onClick: () -> Unit) {
        val b = btn(label, Themes.accent(prefs, this), Color.WHITE, onClick)
        addRow(title, desc, b)
    }

    private fun pickerRow(title: String, desc: String, options: List<String>, sel: Int, onPick: (Int) -> Unit) {
        val cur = if (sel in options.indices) options[sel] else "—"
        val b = btn("Current: $cur — tap to change", Themes.card(prefs, this), Themes.text(prefs, this)) {
            AlertDialog.Builder(this).setTitle(title)
                .setItems(options.toTypedArray()) { _, which -> onPick(which) }
                .show()
        }
        addRow(title, desc, b)
    }

    private fun infoRow(title: String, text: String) {
        addRow(title, text, LinearLayout(this))
    }

    // ---- Looks & Personalities (v1.2) ----
    // Name, one-line vibe, and (background, card, accent) preview colours.
    // These hex values are the same ones Personalities.kt uses; they are kept
    // here so the cards render even if that file is not present.
    private val PERSONALITIES = listOf(
        Triple("Minimal", "Clean, calm, nothing in the way.", Triple("#101014", "#1C1C22", "#EDEDF2")),
        Triple("Sunset Pop", "Warm and bold, like golden hour.", Triple("#1C0F0C", "#2B1914", "#FF7A59")),
        Triple("Neon Night", "Dark, electric, glowing accents.", Triple("#0D1117", "#171D27", "#7DD3FC")),
        Triple("Ocean Glass", "Cool blues, smooth and glassy.", Triple("#12163F", "#1C2160", "#7C84FF")),
        Triple("Paper Day", "Bright, light, easy on the eyes.", Triple("#F3F1FA", "#FFFFFF", "#3B30F0")),
        Triple("Playful", "Fresh green, fun and friendly.", Triple("#0C1A14", "#14281E", "#4ADE80"))
    )
    // Fallback theme for each personality: (Themes.PALETTES index, themeMode).
    // 0 Midnight, 1 Cobalt, 2 Ember, 3 Forest, 4 Paper, 5 Mono; mode 1 Light, 2 Dark.
    private val PERSONALITY_THEME = listOf(5 to 2, 2 to 2, 0 to 2, 1 to 2, 4 to 1, 3 to 2)

    /** Applies personality [index] via Personalities.apply (the full bundle).
     *  Falls back to palette/mode only if that call throws. Always records
     *  prefs.personality. */
    private fun applyPersonality(index: Int) {
        val i = index.coerceIn(0, PERSONALITIES.size - 1)
        try {
            Personalities.apply(prefs, i)
        } catch (e: Exception) {
            val (themeIdx, mode) = PERSONALITY_THEME[i]
            val e2 = prefs.p.edit()
            e2.putInt("theme", themeIdx); e2.putInt("themeMode", mode)
            e2.putInt("accent", 0); e2.putBoolean("dynamicColor", false)
            e2.apply()
        }
        prefs.personality = i
    }

    /** Display data for one personality card: (name, vibe, (bg, card, accent)).
     *  Read straight from Personalities.all() so the dots and vibes always
     *  match the personality definitions; falls back to the built-in table. */
    private fun personalityDisplay(index: Int): Triple<String, String, Triple<String, String, String>> {
        return try {
            val item = Personalities.all().getOrNull(index) ?: return PERSONALITIES[index]
            Triple(item.name, item.vibe, Triple(item.previewBg, item.previewCard, item.previewAccent))
        } catch (e: Exception) {
            PERSONALITIES[index]
        }
    }

    /** One personality card: name, vibe, three colour dots, accent outline
     *  + checkmark when it is the current personality. */
    private fun personalityCard(index: Int): LinearLayout {
        val (name, vibe, colors) = personalityDisplay(index)
        val selected = prefs.personality == index
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(12), dp(14), dp(12))
        card.background = if (selected) {
            Themes.roundedBackground(this, Themes.Radius.CARD, Themes.card(prefs, this), Themes.accent(prefs, this), 2)
        } else {
            Themes.cardBackground(this, Themes.card(prefs, this))
        }
        val nameRow = LinearLayout(this)
        nameRow.orientation = LinearLayout.HORIZONTAL
        nameRow.gravity = Gravity.CENTER_VERTICAL
        nameRow.addView(tv(name, 15f, Themes.text(prefs, this), true), LinearLayout.LayoutParams(0, -2, 1f))
        if (selected) nameRow.addView(tv("✓", 15f, Themes.accent(prefs, this), true))
        card.addView(nameRow)
        val vibeView = tv(vibe, 12f, Themes.sub(prefs, this))
        vibeView.setPadding(0, dp(2), 0, dp(8))
        card.addView(vibeView)
        val dots = LinearLayout(this)
        dots.orientation = LinearLayout.HORIZONTAL
        for (hex in listOf(colors.first, colors.second, colors.third)) {
            val dot = android.view.View(this)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(Color.parseColor(hex))
            dot.background = g
            val lp = LinearLayout.LayoutParams(dp(14), dp(14))
            lp.setMargins(0, 0, dp(6), 0)
            dots.addView(dot, lp)
        }
        card.addView(dots)
        card.setOnClickListener { applyPersonality(index); render() }
        return card
    }

    /** Two-column grid of the six personality cards. Registered as a
     *  searchable row so "personality" / a personality name finds it. */
    private fun personalityCardsRow() {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        for (r in 0 until 3) {
            val row = LinearLayout(this)
            for (c in 0 until 2) {
                val idx = r * 2 + c
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                lp.setMargins(dp(3), dp(3), dp(3), dp(3))
                row.addView(personalityCard(idx), lp)
            }
            wrap.addView(row)
        }
        container.addView(wrap)
        rows.add("Looks & Personalities Minimal Sunset Pop Neon Night Ocean Glass Paper Day Playful personality looks" to wrap)
    }
}
