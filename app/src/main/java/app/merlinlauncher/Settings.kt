package app.merlinlauncher

import android.app.Activity
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

        // ---------- (1) Look & Feel ----------
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

        // ---------- (2) Home ----------
        section("Home")
        chipsRow("Home style", listOf("Grid", "List"), prefs.homeStyle) { prefs.homeStyle = it; render() }
        pickerRow("Preset", "Applies a full bundle of settings at once.", listOf("Minimal", "Classic", "Dense", "One-handed"), listOf("Minimal", "Classic", "Dense", "One-handed").indexOf(prefs.preset)) {
            prefs.applyPreset(listOf("Minimal", "Classic", "Dense", "One-handed")[it])
            Toast.makeText(this, "Preset applied", Toast.LENGTH_SHORT).show()
            render()
        }
        stepperRow("Grid rows", prefs.gridRows, 3, 8) { prefs.gridRows = it }
        stepperRow("Grid columns", prefs.gridCols, 3, 6) { prefs.gridCols = it }
        chipsRow("Icon size", listOf("Small", "Medium", "Large"), prefs.iconSize) { prefs.iconSize = it }
        switchRow("Show app labels", "Labels under Home and drawer icons.", prefs.showLabels) { prefs.showLabels = it }
        switchRow("Lock layout", "Blocks hiding apps and moving Home items until unlocked.", prefs.lockLayout) { prefs.lockLayout = it }
        chipsRow("Animation speed", listOf("Off", "Normal", "Fast"), prefs.animSpeed) { prefs.animSpeed = it }

        // ---------- (3) Drawer ----------
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

        // ---------- (4) Dock ----------
        section("Dock")
        switchRow("Show dock", "App row at the bottom of Home.", prefs.dockShow) { prefs.dockShow = it }
        stepperRow("Dock apps", prefs.dockCount, 3, 7) { prefs.dockCount = it }

        // ---------- (5) Icons & Folders ----------
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
        chipsRow("Icon shape", listOf("Circle", "Squircle", "Rounded", "Square"), prefs.iconShape) { prefs.iconShape = it }
        switchRow("Themed icons", "Mask unthemed icons into the chosen shape on an accent-tinted background.", prefs.themedIcons) { prefs.themedIcons = it }
        buttonRow("Folders", "Cover folders arrive in v1.1. For now, long-press any app to add it to Home or Favourites.", "OK") { }

        // ---------- (6) Gestures ----------
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

        // ---------- (7) Notifications ----------
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

        // ---------- (8) Feed ----------
        section("Feed")
        switchRow("Show Feed", "Swipe left on Home for notes, recent apps and favourites.", prefs.feedShow) { prefs.feedShow = it }

        // ---------- (9) Backup ----------
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

        // ---------- (10) About ----------
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

    private fun stepperRow(title: String, value: Int, min: Int, max: Int, onSet: (Int) -> Unit) {
        var cur = value
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val card = Themes.card(prefs, this)
        val tx = Themes.text(prefs, this)
        val valueView = tv(cur.toString(), 16f, tx, true)
        valueView.gravity = Gravity.CENTER
        val minus = btn("−", card, tx) {
            cur = (cur - 1).coerceAtLeast(min)
            onSet(cur); valueView.text = cur.toString()
        }
        val plus = btn("+", card, tx) {
            cur = (cur + 1).coerceAtMost(max)
            onSet(cur); valueView.text = cur.toString()
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
}
