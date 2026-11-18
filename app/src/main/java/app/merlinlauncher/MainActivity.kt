package app.merlinlauncher

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Merlin Launcher Home. Grid mode AND List mode, switchable on Home and in
 *  Settings. Programmatic Views only, zero dependencies. */
class MainActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var root: FrameLayout
    private var apps: List<AppInfo> = emptyList()
    private var feedMode = false
    private var letterY = HashMap<String, Int>()
    private var scrollView: ScrollView? = null
    private var showingOnboarding = false
    private var onboardingView: OnboardingView? = null
    private var defaultPillShown = false
    private var defaultPillDismissed = false

    internal fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    internal fun tv(s: String, sz: Float, col: Int, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = sz; setTextColor(col)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    /** Light tap feedback, gated by the Haptics setting. */
    private fun tapHaptic() {
        if (prefs.haptics) root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Label text size from the labelSize pref (0 small / 1 medium / 2 large). */
    private fun tileLabelSize() = listOf(10f, 12f, 14f)[prefs.labelSize.coerceIn(0, 2)]
    private fun rowLabelSize() = listOf(14f, 16f, 18f)[prefs.labelSize.coerceIn(0, 2)]

    // ---- v1.2 prefs, read via raw keys so this file compiles even while the
    // Prefs agent is still adding the typed accessors (same keys/defaults). ----
    private fun prefInt(key: String, def: Int) = prefs.p.getInt(key, def)
    private fun prefClockStyle() = prefInt("clockStyle", 0) // 0 big, 1 compact, 2 date-first
    private fun prefSearchStyle() = prefInt("searchStyle", 0) // 0 pill, 1 bar, 2 minimal
    private fun prefFontScale() = prefInt("fontScale", 1) // 0 small, 1 normal, 2 large
    private fun prefTileStyle() = prefInt("tileStyle", 0) // 0 flat, 1 glassy card, 2 bold outline
    private fun prefHomeDim() = prefInt("homeDim", 0) // 0..100

    /** Font scale: small = -2sp, normal = base, large = +3sp. */
    private fun fs(base: Float) = base + when (prefFontScale()) { 0 -> -2f; 2 -> 3f; else -> 0f }

    /** Tile surface for the current tileStyle: flat = transparent ripple,
     *  glassy = rounded card surface, bold outline = 2dp accent-tinted stroke. */
    private fun tileSurface(card: Int, accent: Int): android.graphics.drawable.Drawable {
        val ripple = Themes.withAlpha(accent, 0.16f)
        return when (prefTileStyle()) {
            1 -> Themes.pressable(this, card, Themes.Radius.TILE, ripple)
            2 -> Themes.rippleDrawable(this, ripple,
                Themes.roundedBackground(this, Themes.Radius.TILE, Color.TRANSPARENT,
                    Themes.withAlpha(accent, 0.55f), 2), Themes.Radius.TILE)
            else -> Themes.pressable(this, Color.TRANSPARENT, Themes.Radius.TILE, ripple)
        }
    }

    internal fun pill(s: String, bg: Int, fg: Int, f: () -> Unit) = TextView(this).apply {
        text = s; textSize = Themes.Type.BODY; setTextColor(fg); gravity = Gravity.CENTER
        setPadding(dp(18), dp(10), dp(18), dp(10))
        background = Themes.pressable(this@MainActivity, bg, Themes.Radius.PILL, Themes.withAlpha(fg, 0.14f))
        setOnClickListener { tapHaptic(); f() }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        prefs = Prefs(this)
        root = FrameLayout(this)
        setContentView(root)
        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (showingOnboarding) return true
                if (prefs.doubleTapLock) lockScreen()
                return true
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (showingOnboarding) return false
                if (e1 == null) return false
                val dx = e2.x - e1.x; val dy = e2.y - e1.y
                if (dy > 120 && Math.abs(dy) > Math.abs(dx) * 1.5f) {
                    // Swipe down
                    if (prefs.gestureSwipeDown == 0) openDrawer() else openDrawer()
                    return true
                }
                if (dy < -120 && Math.abs(dy) > Math.abs(dx) * 1.5f) {
                    // Swipe up: open the app drawer (unless turned off in Settings).
                    if (prefs.swipeUpForDrawer) { openDrawer(); return true }
                }
                if (dx < -120 && Math.abs(dx) > Math.abs(dy) * 1.5f && prefs.feedShow && !feedMode) {
                    feedMode = true; render(); return true
                }
                if (dx > 120 && Math.abs(dx) > Math.abs(dy) * 1.5f && feedMode) {
                    feedMode = false; render(); return true
                }
                return false
            }
            override fun onLongPress(e: MotionEvent) {
                if (showingOnboarding) return
                if (!feedMode) openSettings()
            }
        })
        root.setOnTouchListener { _, ev -> gd.onTouchEvent(ev) }

        // First run: friendly welcome wizard before the home screen.
        if (!Onboarding.isOnboarded(this)) {
            showOnboarding()
        }
    }

    override fun onResume() {
        super.onResume()
        if (showingOnboarding) return
        apps = Apps.loadApps(this, prefs)
        render()
        maybeHandleHomeRequest()
    }

    /** Settings can ask for the default-home flow via the "requestDefaultHome" extra. */
    private fun maybeHandleHomeRequest() {
        if (intent.getBooleanExtra("requestDefaultHome", false)) {
            intent.removeExtra("requestDefaultHome")
            requestDefaultHome()
        }
    }

    override fun onBackPressed() {
        if (showingOnboarding) {
            // Step back a wizard page; on page 1, Back leaves the wizard (back to the old Home).
            if (onboardingView?.goBack() == true) return
            super.onBackPressed()
            return
        }
        if (feedMode) { feedMode = false; render() } else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DefaultHome.REQUEST_DEFAULT_HOME) {
            if (DefaultHome.isDefaultLauncher(this)) {
                Toast.makeText(this, "Merlin is your Home now — welcome home!", Toast.LENGTH_LONG).show()
            }
            // Not granted: say nothing. No nagging — the home pill offers again next launch.
            if (!showingOnboarding) render()
        }
    }

    /** Ask the system to make Merlin the default Home app. Settings calls this
     *  via MainActivity: startActivity(Intent(this, MainActivity::class.java)
     *  .putExtra("requestDefaultHome", true)). */
    fun requestDefaultHome() {
        DefaultHome.requestDefaultHome(this)
    }

    private fun showOnboarding() {
        showingOnboarding = true
        val view = OnboardingView(this, prefs) { finishOnboarding() }
        onboardingView = view
        root.addView(view, FrameLayout.LayoutParams(-1, -1))
    }

    private fun finishOnboarding() {
        Onboarding.setOnboarded(this)
        showingOnboarding = false
        onboardingView?.let { root.removeView(it) }
        onboardingView = null
        apps = Apps.loadApps(this, prefs)
        render()
        maybeHandleHomeRequest()
    }

    private fun openDrawer() {
        startActivity(Intent(this, DrawerActivity::class.java))
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    private fun lockScreen() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, LockAdmin::class.java)
        if (!dpm.isAdminActive(admin)) {
            Toast.makeText(this, "Enable Merlin Launcher as a device admin first (Settings > Gestures)", Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Lets double-tap lock the screen.")
                })
            } catch (e: Exception) { }
            return
        }
        try { dpm.lockNow() } catch (e: Exception) {
            Toast.makeText(this, "Could not lock the screen", Toast.LENGTH_SHORT).show()
        }
    }

    /** One app tile: icon (+ badge) with optional label. */
    private fun appTile(app: AppInfo, iconPx: Int): View {
        val accent = Themes.accent(prefs, this)
        val frame = FrameLayout(this)
        val iv = ImageView(this)
        val raw = IconPack.resolve(this, app, prefs)
        // Themed icons get the friendly tile treatment (shape from the Icon shape
        // setting); an app with no icon gets a gradient letter tile, never a blank.
        val drawable = if (raw == null) IconPack.fallbackIcon(this, app.label, accent, iconPx)
            else if (prefs.themedIcons) IconPack.shaped(this, raw, prefs.iconShape, accent, iconPx)
            else raw
        iv.setImageDrawable(drawable)
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
            val lp = FrameLayout.LayoutParams(
                if (prefs.badges == 2) ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                if (prefs.badges == 2) ViewGroup.LayoutParams.WRAP_CONTENT else dp(12),
                Gravity.TOP or Gravity.END)
            frame.addView(badge, lp)
        }
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.minimumHeight = dp(84)
        col.background = tileSurface(Themes.card(prefs, this), accent)
        if (prefTileStyle() == 1) col.elevation = dp(4).toFloat()
        col.addView(frame)
        if (prefs.showLabels) {
            val label = tv(app.label, fs(tileLabelSize()), Themes.text(prefs, this))
            label.gravity = Gravity.CENTER
            label.maxLines = 1
            col.addView(label)
        }
        col.setOnClickListener { tapHaptic(); Apps.launch(this, app, prefs) }
        col.setOnLongClickListener { Apps.showAppMenu(this, app, prefs) { apps = Apps.loadApps(this, prefs); render() }; true }
        return col
    }

    private fun render() {
        val bg = Themes.bg(prefs, this)
        val card = Themes.card(prefs, this)
        val tx = Themes.text(prefs, this)
        val sub = Themes.sub(prefs, this)
        val accent = Themes.accent(prefs, this)
        // Home dim: darken the background toward black by the chosen percent.
        val dimBg = Themes.blend(bg, Color.BLACK, prefHomeDim().coerceIn(0, 100) / 100f)
        root.setBackgroundColor(dimBg)
        root.removeAllViews()
        scrollView = null

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setPadding(dp(16), dp(12), dp(16), dp(8))
        root.addView(main, FrameLayout.LayoutParams(-1, -1))

        // ---- Hero header: friendly greeting + clock in three styles ----
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val dateFmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        val timeFmt = SimpleDateFormat(if (prefs.clockFormat24) "HH:mm" else "h:mm a", Locale.getDefault())
        val now = Date()
        if (prefs.showAtAGlance) {
            val hero = LinearLayout(this)
            hero.orientation = LinearLayout.VERTICAL
            // Soft rounded card behind the hero when tiles are glassy.
            if (prefTileStyle() == 1) {
                hero.background = Themes.cardBackground(this, card)
                hero.setPadding(dp(16), dp(12), dp(16), dp(12))
            }
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when {
                hour < 12 -> "Good morning"
                hour < 18 -> "Good afternoon"
                else -> "Good evening"
            }
            hero.addView(tv(greeting, fs(Themes.Type.TITLE), accent, true))
            when (prefClockStyle()) {
                1 -> { // Compact: time + date on one line.
                    val line = if (prefs.showDateOnHome) "${timeFmt.format(now)}  ·  ${dateFmt.format(now)}"
                        else timeFmt.format(now)
                    hero.addView(tv(line, fs(Themes.Type.SUBTITLE), tx, true))
                }
                2 -> { // Date-first: big date, smaller time below.
                    if (prefs.showDateOnHome) hero.addView(tv(dateFmt.format(now), fs(26f), tx, true))
                    hero.addView(tv(timeFmt.format(now), fs(Themes.Type.SUBTITLE), sub))
                }
                else -> { // Big: huge time with the date underneath.
                    hero.addView(tv(timeFmt.format(now), fs(48f), tx, true))
                    if (prefs.showDateOnHome) hero.addView(tv(dateFmt.format(now), fs(Themes.Type.SUBTITLE), sub))
                }
            }
            // Honest placeholder: no weather API, no fake forecast.
            hero.addView(tv("Weather: add widget  ·  v1.2", fs(Themes.Type.CAPTION), sub))
            val heroLp = LinearLayout.LayoutParams(0, -2, 1f)
            heroLp.setMargins(0, 0, dp(8), 0)
            header.addView(hero, heroLp)
        } else {
            header.addView(View(this), LinearLayout.LayoutParams(0, -2, 1f))
        }
        header.addView(pill(if (prefs.homeStyle == 0) "List" else "Grid", card, tx) {
            prefs.homeStyle = if (prefs.homeStyle == 0) 1 else 0
            render()
        })
        val gear = pill("⚙", card, tx) { openSettings() }
        val gearLp = LinearLayout.LayoutParams(-2, -2); gearLp.setMargins(dp(8), 0, 0, 0)
        header.addView(gear, gearLp)
        main.addView(header)

        // ---- Search: three styles (pill / bar / minimal), hidden if off ----
        if (prefs.searchBarOnHome) {
            val searchLp = LinearLayout.LayoutParams(-1, -2); searchLp.setMargins(0, dp(12), 0, dp(4))
            when (prefSearchStyle()) {
                1 -> { // Bar: full-width rounded-rectangle bar with a subtle outline.
                    val search = TextView(this).apply {
                        text = "🔍  Search apps, settings…"; textSize = fs(Themes.Type.BODY); setTextColor(sub)
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                        setPadding(dp(18), dp(13), dp(18), dp(13))
                        background = Themes.roundedBackground(this@MainActivity, 12, card, Themes.withAlpha(sub, 0.25f))
                        setOnClickListener { tapHaptic(); openDrawer() }
                    }
                    main.addView(search, searchLp)
                }
                2 -> { // Minimal: plain text row with a thin underline, no background.
                    val wrap = LinearLayout(this)
                    wrap.orientation = LinearLayout.VERTICAL
                    val search = TextView(this).apply {
                        text = "🔍  Search apps, settings…"; textSize = fs(Themes.Type.BODY); setTextColor(sub)
                        setPadding(dp(4), dp(8), dp(4), dp(8))
                        setOnClickListener { tapHaptic(); openDrawer() }
                    }
                    wrap.addView(search)
                    val line = View(this)
                    line.setBackgroundColor(Themes.withAlpha(sub, 0.35f))
                    wrap.addView(line, LinearLayout.LayoutParams(-1, dp(1)))
                    main.addView(wrap, searchLp)
                }
                else -> { // Pill: soft outline pill (the v1.1 look).
                    val search = TextView(this).apply {
                        text = "🔍  Search apps, settings…"; textSize = fs(Themes.Type.BODY); setTextColor(sub); gravity = Gravity.CENTER
                        setPadding(dp(18), dp(10), dp(18), dp(10))
                        background = Themes.pillBackground(this@MainActivity, card, Themes.withAlpha(sub, 0.25f))
                        setOnClickListener { tapHaptic(); openDrawer() }
                    }
                    main.addView(search, searchLp)
                }
            }
        }

        // ---- Gentle "make me default" nudge: once per app start, dismissible,
        // never while onboarding is showing (render() is not called then). ----
        if (!defaultPillDismissed && !defaultPillShown && !DefaultHome.isDefaultLauncher(this)) {
            defaultPillShown = true
            val nudge = LinearLayout(this)
            nudge.orientation = LinearLayout.HORIZONTAL
            nudge.gravity = Gravity.CENTER_VERTICAL
            nudge.background = Themes.cardBackground(this, card)
            nudge.setPadding(dp(16), dp(12), dp(8), dp(12))
            val label = tv("Make Merlin your default Home?", 14f, tx, true)
            nudge.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
            nudge.addView(pill("Set it up", accent, Color.WHITE) { requestDefaultHome() })
            val x = tv("✕", 16f, sub)
            x.setPadding(dp(12), 0, dp(4), 0)
            x.setOnClickListener { defaultPillDismissed = true; render() }
            nudge.addView(x)
            val nlp = LinearLayout.LayoutParams(-1, -2); nlp.setMargins(0, dp(8), 0, 0)
            main.addView(nudge, nlp)
        }

        if (feedMode) {
            renderFeed(main, card, tx, sub, accent)
        } else if (prefs.homeStyle == 0) {
            renderGrid(main)
        } else {
            renderList(main, tx, sub, accent)
        }

        // ---- Dock ----
        if (prefs.dockShow) {
            val dock = LinearLayout(this)
            dock.orientation = LinearLayout.HORIZONTAL
            dock.gravity = Gravity.CENTER
            // Dock style: 0 floating rounded card, 1 full-width bar, 2 transparent.
            when (prefs.dockStyle) {
                1 -> {
                    dock.background = Themes.roundedBackground(this, 4, card)
                    dock.setPadding(dp(8), dp(8), dp(8), dp(8))
                }
                2 -> dock.setPadding(dp(8), dp(8), dp(8), dp(8))
                else -> {
                    Themes.applyCard(dock, this, card, 8f)
                    dock.setPadding(dp(8), dp(8), dp(8), dp(8))
                }
            }
            val favs = prefs.favourites()
            val dockApps = ArrayList<AppInfo>()
            for (a in apps) if (favs.contains(a.pkg)) dockApps.add(a)
            if (dockApps.size < prefs.dockCount) {
                for (a in apps.sortedByDescending { prefs.usage(it.pkg) }) {
                    if (dockApps.size >= prefs.dockCount) break
                    if (!dockApps.contains(a)) dockApps.add(a)
                }
            }
            val iconPx = dp(listOf(48, 56, 64)[prefs.iconSize.coerceIn(0, 2)])
            for (a in dockApps.take(prefs.dockCount)) {
                val tile = appTile(a, iconPx)
                val lp = LinearLayout.LayoutParams(0, -2, 1f)
                dock.addView(tile, lp)
            }
            val dockLp = LinearLayout.LayoutParams(-1, -2); dockLp.setMargins(0, dp(8), 0, 0)
            main.addView(dock, dockLp)
            // Swipe up on the dock opens the app drawer (unless turned off in Settings).
            // (Cover folders are a v1.1 feature — see README.)
            val dockGd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                    if (prefs.swipeUpForDrawer && e1 != null && e1.y - e2.y > 100) { openDrawer(); return true }
                    return false
                }
            })
            dock.setOnTouchListener { _, ev -> dockGd.onTouchEvent(ev) }
        }

        // ---- Feed hint ----
        if (prefs.feedShow && !feedMode) {
            val hint = tv("‹ Swipe left for Feed", 12f, sub)
            hint.gravity = Gravity.CENTER
            hint.setPadding(0, dp(6), 0, 0)
            hint.setOnClickListener { feedMode = true; render() }
            main.addView(hint)
        }

        // Entrance: slide up + fade in (off when animations are off).
        if (prefs.animSpeed != 0) {
            val dur = if (prefs.animSpeed == 2) 120L else 250L
            main.alpha = 0f
            main.translationY = dp(24).toFloat()
            main.animate().alpha(1f).translationY(0f).setDuration(dur).start()
        }
    }

    private fun renderGrid(main: LinearLayout) {
        val cols = prefs.gridCols
        val rows = prefs.gridRows
        val max = cols * rows
        val favs = prefs.favourites()
        val home = prefs.homeApps()
        val shown = ArrayList<AppInfo>()
        for (a in apps) if (favs.contains(a.pkg) || home.contains(a.pkg)) shown.add(a)
        if (shown.isEmpty()) shown.addAll(apps.take(max))
        val grid = GridLayout(this)
        grid.columnCount = cols
        grid.rowCount = rows
        val iconPx = dp(listOf(48, 56, 64)[prefs.iconSize.coerceIn(0, 2)])
        var i = 0
        for (a in shown.take(max)) {
            val tile = appTile(a, iconPx)
            val lp = GridLayout.LayoutParams(GridLayout.spec(i / cols, 1f), GridLayout.spec(i % cols, 1f))
            lp.width = 0
            lp.setMargins(0, dp(10), 0, 0)
            grid.addView(tile, lp)
            i++
        }
        val glp = LinearLayout.LayoutParams(-1, 0, 1f)
        main.addView(grid, glp)
    }

    private fun renderList(main: LinearLayout, tx: Int, sub: Int, accent: Int) {
        val wrap = FrameLayout(this)
        val sv = ScrollView(this)
        scrollView = sv
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        val iconPx = dp(listOf(44, 52, 60)[prefs.iconSize.coerceIn(0, 2)])
        val favs = prefs.favourites()
        val favApps = apps.filter { favs.contains(it.pkg) }
        letterY.clear()
        if (favApps.isNotEmpty()) {
            list.addView(sectionHeader("★ Favourites", sub))
            for (a in favApps) list.addView(listRow(a, iconPx, tx))
        }
        list.addView(sectionHeader("All apps", sub))
        var lastLetter = ""
        for (a in apps) {
            val letter = a.label.firstOrNull()?.uppercase() ?: "#"
            if (letter != lastLetter) {
                lastLetter = letter
                val mark = TextView(this)
                mark.text = letter
                mark.setTag("letter_$letter")
                list.addView(mark)
            }
            list.addView(listRow(a, iconPx, tx))
        }
        sv.addView(list)
        wrap.addView(sv, FrameLayout.LayoutParams(-1, -1))
        // Alphabet wave on the right edge for one-handed jumping.
        val wave = LinearLayout(this)
        wave.orientation = LinearLayout.VERTICAL
        for (ch in 'A'..'Z') {
            val t = tv(ch.toString(), 11f, sub)
            t.gravity = Gravity.CENTER
            t.setPadding(0, dp(1), 0, dp(1))
            t.setOnClickListener {
                val target = list.findViewWithTag<View>("letter_$ch") ?: return@setOnClickListener
                sv.post { sv.scrollTo(0, target.top) }
            }
            wave.addView(t)
        }
        val waveLp = FrameLayout.LayoutParams(-2, -2, Gravity.END or Gravity.CENTER_VERTICAL)
        waveLp.setMargins(0, 0, dp(2), 0)
        wrap.addView(wave, waveLp)
        val wlp = LinearLayout.LayoutParams(-1, 0, 1f)
        main.addView(wrap, wlp)
    }

    private fun sectionHeader(s: String, col: Int): TextView {
        val t = tv(s, fs(13f), col, true)
        t.setPadding(dp(4), dp(14), 0, dp(4))
        return t
    }

    private fun listRow(app: AppInfo, iconPx: Int, tx: Int): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(4), dp(7), dp(4), dp(7))
        row.minimumHeight = dp(56)
        val accent = Themes.accent(prefs, this)
        row.background = tileSurface(Themes.card(prefs, this), accent)
        if (prefTileStyle() == 1) row.elevation = dp(4).toFloat()
        val iv = ImageView(this)
        val raw = IconPack.resolve(this, app, prefs)
        val drawable = if (raw == null) IconPack.fallbackIcon(this, app.label, accent, iconPx)
            else if (prefs.themedIcons) IconPack.shaped(this, raw, prefs.iconShape, accent, iconPx)
            else raw
        iv.setImageDrawable(drawable)
        row.addView(iv, LinearLayout.LayoutParams(iconPx, iconPx))
        val label = tv(app.label, fs(rowLabelSize()), tx)
        val llp = LinearLayout.LayoutParams(0, -2, 1f); llp.setMargins(dp(14), 0, 0, 0)
        row.addView(label, llp)
        val count = BadgeService.countFor(app.pkg)
        if (prefs.badges > 0 && count > 0) {
            row.addView(tv(if (prefs.badges == 2) count.toString() else "●", 13f, Color.parseColor("#E5484D"), true))
        }
        row.setOnClickListener { tapHaptic(); Apps.launch(this, app, prefs) }
        row.setOnLongClickListener { Apps.showAppMenu(this, app, prefs) { apps = Apps.loadApps(this, prefs); render() }; true }
        return row
    }

    /** Feed page: quick notes + recent apps + favourites. */
    private fun renderFeed(main: LinearLayout, card: Int, tx: Int, sub: Int, accent: Int) {
        val title = tv("Feed", Themes.Type.TITLE, tx, true)
        main.addView(title)
        val notesLabel = tv("Quick notes", Themes.Type.CAPTION, sub, true)
        notesLabel.setPadding(0, dp(12), 0, dp(4))
        main.addView(notesLabel)
        val et = EditText(this)
        et.setText(prefs.notes)
        et.setTextColor(tx)
        et.setHintTextColor(sub)
        et.hint = "Type a note… saved automatically"
        et.minLines = 4
        et.background = Themes.cardBackground(this, card)
        et.setPadding(dp(14), dp(12), dp(14), dp(12))
        et.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { prefs.notes = s?.toString() ?: "" }
        })
        main.addView(et, LinearLayout.LayoutParams(-1, -2))
        val recentLabel = tv("Recent apps", Themes.Type.CAPTION, sub, true)
        recentLabel.setPadding(0, dp(16), 0, dp(4))
        main.addView(recentLabel)
        val iconPx = dp(52)
        val recent = apps.sortedByDescending { prefs.usage(it.pkg) }.take(6)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        for (a in recent) {
            val tile = appTile(a, iconPx)
            row.addView(tile, LinearLayout.LayoutParams(0, -2, 1f))
        }
        main.addView(row)
        val back = pill("‹ Back to Home", card, tx) { feedMode = false; render() }
        val blp = LinearLayout.LayoutParams(-2, -2); blp.setMargins(0, dp(16), 0, 0)
        main.addView(back, blp)
    }
}
