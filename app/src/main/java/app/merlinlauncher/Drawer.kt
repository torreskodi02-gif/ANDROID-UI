package app.merlinlauncher

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** App drawer: live search, sort (A-Z / most-used / category), grid columns
 *  from prefs. Friendly rework (v1.1): pill search with a clear button,
 *  letter/category section headers, a polished alphabet fast-scroller on the
 *  right edge, rounded icon tiles with favourite stars, and a warm empty
 *  state when a search matches nothing. Behaviour is unchanged: live search,
 *  all three sorts, and the long-press menu. */
class DrawerActivity : Activity() {
    private lateinit var prefs: Prefs
    private var apps: List<AppInfo> = emptyList()
    private var query = ""

    private var bodyRef: LinearLayout? = null
    private var countRef: TextView? = null
    private var scrollRef: ScrollView? = null
    private var searchRef: EditText? = null
    private var clearRef: TextView? = null
    private var scrollerRef: LinearLayout? = null
    private var bubbleRef: TextView? = null
    private var letterY = HashMap<String, Int>()
    private var activeLetter: String? = null

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

        // ---- Pill search with a clear button ----
        val searchWrap = FrameLayout(this)
        val search = EditText(this)
        search.hint = "Search apps…"
        search.setText(query)
        search.setSelection(search.text.length)
        search.setTextColor(tx)
        search.setHintTextColor(sub)
        search.textSize = 15f
        search.background = Themes.pillBackground(this, card, Themes.withAlpha(sub, 0.25f))
        search.setPadding(dp(20), dp(13), dp(46), dp(13))
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString() ?: ""
                clearRef?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                renderList()
            }
        })
        searchWrap.addView(search, FrameLayout.LayoutParams(-1, -2))
        val clear = TextView(this)
        clear.text = "✕"
        clear.textSize = 15f
        clear.setTextColor(sub)
        clear.gravity = Gravity.CENTER
        clear.setPadding(dp(12), dp(10), dp(16), dp(10))
        clear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
        clear.setOnClickListener { search.setText("") }
        searchWrap.addView(clear, FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL))
        searchRef = search
        clearRef = clear
        main.addView(searchWrap, LinearLayout.LayoutParams(-1, -2))

        val countView = tv("", Themes.Type.CAPTION, sub)
        countView.setPadding(dp(4), dp(10), 0, dp(2))
        main.addView(countView)

        val sv = ScrollView(this)
        scrollRef = sv
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        sv.addView(body)
        main.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))

        // ---- Alphabet fast-scroller on the right edge ----
        val strip = LinearLayout(this)
        strip.orientation = LinearLayout.VERTICAL
        strip.background = Themes.pillBackground(this, Themes.withAlpha(card, 0.92f))
        strip.setPadding(dp(3), dp(8), dp(3), dp(8))
        for (ch in 'A'..'Z') {
            val t = tv(ch.toString(), Themes.Type.TINY, sub)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(1), 0, dp(1))
            strip.addView(t)
        }
        strip.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    handleScrollerTouch(ev.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hideBubble()
                    true
                }
                else -> false
            }
        }
        scrollerRef = strip
        val stripLp = FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL)
        stripLp.setMargins(0, 0, dp(4), 0)
        root.addView(strip, stripLp)

        // Big letter bubble shown while dragging the fast-scroller.
        val bubble = TextView(this)
        bubble.textSize = 28f
        bubble.setTextColor(Color.WHITE)
        bubble.gravity = Gravity.CENTER
        bubble.setPadding(dp(18), dp(10), dp(18), dp(10))
        bubble.background = Themes.pillBackground(this, accent)
        bubble.visibility = View.GONE
        bubbleRef = bubble
        root.addView(bubble, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))

        renderListInto(body, countView, tx, sub, accent)
    }

    private fun renderList() {
        val body = bodyRef ?: return
        val countView = countRef ?: return
        renderListInto(body, countView, Themes.text(prefs, this), Themes.sub(prefs, this), Themes.accent(prefs, this))
    }

    private fun renderListInto(body: LinearLayout, countView: TextView, tx: Int, sub: Int, accent: Int) {
        bodyRef = body; countRef = countView
        body.removeAllViews()
        letterY.clear()
        val q = query.trim().lowercase()
        val filtered = Apps.sortedForDrawer(apps, prefs).filter { q.isEmpty() || it.label.lowercase().contains(q) || it.pkg.lowercase().contains(q) }
        countView.text = if (filtered.isEmpty() && q.isNotEmpty()) "No apps found" else "All apps · ${filtered.size} of ${apps.size}"
        val iconPx = dp(listOf(48, 56, 64)[prefs.iconSize.coerceIn(0, 2)])
        val cols = prefs.drawerCols

        if (filtered.isEmpty()) {
            body.addView(emptyState(tx, sub, accent))
            updateScroller(false)
            return
        }

        when {
            q.isEmpty() && prefs.drawerSort == 2 -> {
                // Category sections with headers, one grid per category.
                val cats = listOf("Communication", "Media", "Social", "Games", "Tools", "Other")
                for (cat in cats) {
                    val catApps = filtered.filter { Apps.categoryOf(it) == cat }
                    if (catApps.isEmpty()) continue
                    body.addView(sectionHeader(cat, tx, accent))
                    val catGrid = GridLayout(this)
                    catGrid.columnCount = cols
                    addGridRows(catGrid, catApps, iconPx, cols)
                    body.addView(catGrid)
                }
                updateScroller(false)
            }
            q.isEmpty() && prefs.drawerSort == 0 -> {
                // A–Z: one section per starting letter, each with a header.
                var lastLetter = ""
                var grid: GridLayout? = null
                var idx = 0
                for (a in filtered) {
                    val raw = a.label.firstOrNull()?.uppercase() ?: "#"
                    val letter = if (raw.first().isLetter()) raw else "#"
                    if (letter != lastLetter) {
                        lastLetter = letter
                        body.addView(sectionHeader(letter, tx, accent, letterTag = letter))
                        grid = GridLayout(this)
                        grid.columnCount = cols
                        body.addView(grid)
                        idx = 0
                    }
                    val tile = appTile(a, iconPx, tx, accent)
                    val lp = GridLayout.LayoutParams(GridLayout.spec(idx / cols, 1f), GridLayout.spec(idx % cols, 1f))
                    lp.width = 0
                    lp.setMargins(0, dp(8), 0, 0)
                    grid!!.addView(tile, lp)
                    idx++
                }
                updateScroller(true)
            }
            q.isEmpty() && prefs.drawerSort == 1 -> {
                body.addView(sectionHeader("★  Most used", tx, accent))
                val grid = GridLayout(this)
                grid.columnCount = cols
                addGridRows(grid, filtered, iconPx, cols)
                body.addView(grid)
                updateScroller(false)
            }
            else -> {
                // Searching: flat grid, no section headers.
                val grid = GridLayout(this)
                grid.columnCount = cols
                addGridRows(grid, filtered, iconPx, cols)
                body.addView(grid)
                updateScroller(false)
            }
        }
        scrollRef?.post { computeLetterPositions() }
    }

    private fun addGridRows(grid: GridLayout, list: List<AppInfo>, iconPx: Int, cols: Int) {
        val tx = Themes.text(prefs, this)
        val accent = Themes.accent(prefs, this)
        var i = 0
        for (a in list) {
            val tile = appTile(a, iconPx, tx, accent)
            val lp = GridLayout.LayoutParams(GridLayout.spec(i / cols, 1f), GridLayout.spec(i % cols, 1f))
            lp.width = 0
            lp.setMargins(0, dp(8), 0, 0)
            grid.addView(tile, lp)
            i++
        }
    }

    /** Section header: accent dot, bold title, hairline divider. When
     *  [letterTag] is set the row is a fast-scroller anchor for that letter. */
    private fun sectionHeader(title: String, tx: Int, accent: Int, letterTag: String? = null): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(4), dp(16), dp(4), dp(6))
        if (letterTag != null) row.tag = "letter_$letterTag"
        val dot = View(this)
        val dotBg = GradientDrawable()
        dotBg.shape = GradientDrawable.OVAL
        dotBg.setColor(accent)
        dot.background = dotBg
        val dotLp = LinearLayout.LayoutParams(dp(8), dp(8))
        dotLp.setMargins(0, 0, dp(8), 0)
        row.addView(dot, dotLp)
        row.addView(tv(title, Themes.Type.CAPTION + 1f, tx, true))
        val line = View(this)
        line.setBackgroundColor(Themes.divider(prefs, this))
        val lineLp = LinearLayout.LayoutParams(0, dp(1), 1f)
        lineLp.setMargins(dp(10), 0, 0, 0)
        row.addView(line, lineLp)
        return row
    }

    /** Warm empty state shown when a search matches nothing. */
    private fun emptyState(tx: Int, sub: Int, accent: Int): LinearLayout {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.gravity = Gravity.CENTER_HORIZONTAL
        wrap.setPadding(dp(24), dp(48), dp(24), dp(16))
        val icon = tv("🔍", 40f, sub)
        icon.gravity = Gravity.CENTER
        wrap.addView(icon)
        val title = tv("No apps match", 18f, tx, true)
        title.gravity = Gravity.CENTER
        title.setPadding(0, dp(16), 0, 0)
        wrap.addView(title)
        val hint = tv("Try another search — check the spelling, or clear the search to see all your apps.", Themes.Type.BODY, sub)
        hint.gravity = Gravity.CENTER
        hint.setPadding(0, dp(8), 0, 0)
        wrap.addView(hint)
        val clear = TextView(this)
        clear.text = "Clear search"
        clear.textSize = Themes.Type.BODY
        clear.setTextColor(Color.WHITE)
        clear.gravity = Gravity.CENTER
        clear.setPadding(dp(20), dp(11), dp(20), dp(11))
        clear.background = Themes.pressable(this, accent, Themes.Radius.PILL,
            Themes.withAlpha(Color.WHITE, 0.25f))
        clear.setOnClickListener { searchRef?.setText("") }
        val clearLp = LinearLayout.LayoutParams(-2, -2)
        clearLp.setMargins(0, dp(20), 0, 0)
        wrap.addView(clear, clearLp)
        return wrap
    }

    private fun appTile(app: AppInfo, iconPx: Int, tx: Int, accent: Int): LinearLayout {
        val frame = FrameLayout(this)
        val iv = ImageView(this)
        val raw = IconPack.resolve(this, app, prefs)
        val drawable = when {
            raw == null -> IconPack.fallbackIcon(this, app.label, accent, iconPx)
            prefs.themedIcons -> IconPack.tile(this, raw, accent, iconPx)
            else -> raw
        }
        iv.setImageDrawable(drawable)
        iv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx, Gravity.CENTER_HORIZONTAL)
        frame.addView(iv)
        // Favourite star: a small gold star on a soft pill, top-right of the icon.
        if (prefs.favourites().contains(app.pkg)) {
            val star = TextView(this)
            star.text = "★"
            star.textSize = Themes.Type.TINY
            star.setTextColor(Color.parseColor("#F5A524"))
            star.gravity = Gravity.CENTER
            star.background = Themes.pillBackground(this, Themes.withAlpha(Themes.card(prefs, this), 0.95f))
            star.setPadding(dp(4), dp(1), dp(4), dp(1))
            frame.addView(star, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END))
        }
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
                if (prefs.badges == 2) ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                if (prefs.badges == 2) ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                Gravity.TOP or Gravity.END))
        }
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        // Larger touch target with a friendly press ripple.
        col.setPadding(dp(2), dp(8), dp(2), dp(8))
        col.minimumHeight = dp(84)
        col.background = Themes.pressable(this, Color.TRANSPARENT, Themes.Radius.TILE,
            Themes.withAlpha(accent, 0.16f))
        col.addView(frame)
        if (prefs.showLabels) {
            val label = tv(app.label, Themes.Type.CAPTION, tx)
            label.gravity = Gravity.CENTER
            label.maxLines = 1
            col.addView(label)
        }
        col.setOnClickListener { Apps.launch(this, app, prefs) }
        col.setOnLongClickListener { Apps.showAppMenu(this, app, prefs) { apps = Apps.loadApps(this, prefs); render() }; true }
        return col
    }

    // ---- Alphabet fast-scroller ----

    private fun updateScroller(show: Boolean) {
        scrollerRef?.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) hideBubble()
    }

    private fun computeLetterPositions() {
        val body = bodyRef ?: return
        letterY.clear()
        for (i in 0 until body.childCount) {
            val v = body.getChildAt(i)
            val tag = v.tag as? String ?: continue
            if (tag.startsWith("letter_")) letterY[tag.removePrefix("letter_")] = v.top
        }
    }

    private fun handleScrollerTouch(y: Float) {
        val strip = scrollerRef ?: return
        if (strip.height <= 0) return
        val idx = ((y / strip.height.toFloat()) * 26).toInt().coerceIn(0, 25)
        val letter = ('A' + idx).toString()
        scrollToLetter(letter)
        showBubble(letter)
        highlightLetter(letter)
    }

    private fun scrollToLetter(letter: String) {
        val sv = scrollRef ?: return
        val target = letterY[letter]
            ?: letterY.entries.filter { it.key < letter }.maxByOrNull { it.key }?.value
            ?: return
        sv.smoothScrollTo(0, target)
    }

    private fun showBubble(letter: String) {
        val b = bubbleRef ?: return
        b.text = letter
        b.visibility = View.VISIBLE
    }

    private fun hideBubble() {
        bubbleRef?.visibility = View.GONE
        activeLetter = null
        resetLetterColors()
    }

    private fun highlightLetter(letter: String) {
        val strip = scrollerRef ?: return
        activeLetter = letter
        val accent = Themes.accent(prefs, this)
        val sub = Themes.sub(prefs, this)
        for (i in 0 until strip.childCount) {
            val t = strip.getChildAt(i) as? TextView ?: continue
            val isActive = t.text.toString() == letter
            t.setTextColor(if (isActive) accent else sub)
            t.setTypeface(t.typeface, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun resetLetterColors() {
        val strip = scrollerRef ?: return
        val sub = Themes.sub(prefs, this)
        for (i in 0 until strip.childCount) {
            val t = strip.getChildAt(i) as? TextView ?: continue
            t.setTextColor(sub)
            t.setTypeface(t.typeface, Typeface.NORMAL)
        }
    }
}
