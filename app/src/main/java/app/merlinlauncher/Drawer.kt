package app.merlinlauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** App drawer: live search, sort (A-Z / most-used / category), grid columns from prefs. */
class DrawerActivity : Activity() {
    private lateinit var prefs: Prefs
    private var apps: List<AppInfo> = emptyList()
    private var query = ""

    internal fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    internal fun tv(s: String, sz: Float, col: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sz; setTextColor(col)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
    }

    override fun onResume() {
        super.onResume()
        apps = Apps.loadApps(this, prefs)
        render()
    }

    private fun render() {
        val bg = Themes.bg(prefs, this)
        val card = Themes.card(prefs, this)
        val tx = Themes.text(prefs, this)
        val sub = Themes.sub(prefs, this)
        val accent = Themes.accent(prefs, this)
        val root = FrameLayout(this)
        root.setBackgroundColor(bg)
        setContentView(root)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(16), dp(12), dp(16), dp(8))
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        val search = EditText(this)
        search.hint = "Search apps…"
        search.setText(query)
        search.setSelection(search.text.length)
        search.setTextColor(tx)
        search.setHintTextColor(sub)
        val sg = GradientDrawable(); sg.setColor(card); sg.cornerRadius = dp(24).toFloat()
        search.background = sg
        search.setPadding(dp(18), dp(12), dp(18), dp(12))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { query = s?.toString() ?: ""; renderList() }
        })
        main.addView(search, LinearLayout.LayoutParams(-1, -2))

        val countView = tv("", 13f, sub)
        countView.setPadding(dp(4), dp(10), 0, dp(2))
        main.addView(countView)

        val sv = ScrollView(this)
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        sv.addView(body)
        main.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        renderListInto(body, countView, tx, sub, accent)
    }

    private var bodyRef: LinearLayout? = null
    private var countRef: TextView? = null

    private fun renderList() {
        val body = bodyRef ?: return
        val countView = countRef ?: return
        renderListInto(body, countView, Themes.text(prefs, this), Themes.sub(prefs, this), Themes.accent(prefs, this))
    }

    private fun renderListInto(body: LinearLayout, countView: TextView, tx: Int, sub: Int, accent: Int) {
        bodyRef = body; countRef = countView
        body.removeAllViews()
        val q = query.trim().lowercase()
        val filtered = Apps.sortedForDrawer(apps, prefs).filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
        countView.text = "All apps · ${filtered.size} of ${apps.size}"
        val iconPx = dp(listOf(48, 56, 64)[prefs.iconSize.coerceIn(0, 2)])
        val cols = prefs.drawerCols

        if (prefs.drawerSort == 2 && q.isEmpty()) {
            // Category sections with headers, one grid per category.
            val cats = listOf("Communication", "Media", "Social", "Games", "Tools", "Other")
            for (cat in cats) {
                val catApps = filtered.filter { Apps.categoryOf(it) == cat }
                if (catApps.isEmpty()) continue
                body.addView(sectionHeader(cat, sub))
                val catGrid = GridLayout(this)
                catGrid.columnCount = cols
                addGridRows(catGrid, catApps, iconPx, cols)
                body.addView(catGrid)
            }
            return
        }

        val grid = GridLayout(this)
        grid.columnCount = cols
        addGridRows(grid, filtered, iconPx, cols)
        body.addView(grid)
    }

    private fun addGridRows(grid: GridLayout, list: List<AppInfo>, iconPx: Int, cols: Int) {
        val tx = Themes.text(prefs, this)
        val accent = Themes.accent(prefs, this)
        var i = 0
        for (a in list) {
            val tile = appTile(a, iconPx, tx, accent)
            val lp = GridLayout.LayoutParams(GridLayout.spec(i / cols, 1f), GridLayout.spec(i % cols, 1f))
            lp.width = 0
            lp.setMargins(0, dp(10), 0, 0)
            grid.addView(tile, lp)
            i++
        }
    }

    private fun sectionHeader(s: String, col: Int): TextView {
        val t = tv(s, 13f, col, true)
        t.setPadding(dp(4), dp(14), 0, dp(4))
        return t
    }

    private fun appTile(app: AppInfo, iconPx: Int, tx: Int, accent: Int): LinearLayout {
        val frame = FrameLayout(this)
        val iv = ImageView(this)
        val raw = IconPack.resolve(this, app, prefs)
        iv.setImageDrawable(if (prefs.themedIcons) IconPack.shaped(this, raw, prefs.iconShape, accent, iconPx) else raw)
        iv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER_HORIZONTAL)
        frame.addView(iv)
        val count = BadgeService.countFor(app.pkg)
        if (prefs.badges > 0 && count > 0) {
            val badge = TextView(this)
            badge.text = if (prefs.badges == 2) count.toString() else ""
            badge.textSize = 10f
            badge.setTextColor(Color.WHITE)
            badge.gravity = Gravity.CENTER
            val g = GradientDrawable()
            g.setColor(Color.parseColor("#E5484D"))
            if (prefs.badges == 2) {
                g.cornerRadius = dp(10).toFloat()
                badge.setPadding(dp(6), dp(2), dp(6), dp(2))
            } else {
                g.shape = GradientDrawable.OVAL
            }
            badge.background = g
            frame.addView(badge, FrameLayout.LayoutParams(
                if (prefs.badges == 2) android.view.ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                if (prefs.badges == 2) android.view.ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                Gravity.TOP or Gravity.END))
        }
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(frame)
        if (prefs.showLabels) {
            val label = tv(app.label, 12f, tx)
            label.gravity = Gravity.CENTER
            label.maxLines = 1
            col.addView(label)
        }
        col.setOnClickListener { Apps.launch(this, app, prefs) }
        col.setOnLongClickListener { Apps.showAppMenu(this, app, prefs) { apps = Apps.loadApps(this, prefs); render() }; true }
        return col
    }
}
