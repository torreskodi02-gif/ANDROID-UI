package app.merlinlauncher

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** First-run welcome wizard + the "onboarded" flag.
 *
 *  The flag lives in the same "ml" SharedPreferences file Prefs uses. Prefs
 *  has no onboarded property, so Onboarding reads/writes the key directly
 *  (same file name, same key everywhere — "onboarded"). */
object Onboarding {
    private const val KEY = "onboarded"

    fun isOnboarded(ctx: Context): Boolean =
        ctx.getSharedPreferences("ml", Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setOnboarded(ctx: Context) {
        ctx.getSharedPreferences("ml", Context.MODE_PRIVATE).edit().putBoolean(KEY, true).apply()
    }
}

/** Three friendly pages: Welcome -> Pick your style -> Make Merlin your Home.
 *  Shown as the content view until the user finishes (or taps "Maybe later").
 *  Style choices save immediately via Prefs. The default-home request goes
 *  through DefaultHome; MainActivity handles its onActivityResult.
 *  Programmatic Views only, no XML, no Compose, no new dependencies. */
class OnboardingView(
    private val activity: Activity,
    private val prefs: Prefs,
    private val onFinish: () -> Unit
) : LinearLayout(activity) {

    private var page = 0
    private lateinit var content: LinearLayout
    private lateinit var dotsRow: LinearLayout
    private lateinit var navRow: LinearLayout

    init {
        orientation = VERTICAL
        setPadding(dp(24), dp(32), dp(24), dp(24))
        val sv = ScrollView(activity)
        content = LinearLayout(activity).apply { orientation = VERTICAL }
        sv.addView(content, LinearLayout.LayoutParams(-1, -2))
        addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        dotsRow = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(12))
        }
        addView(dotsRow, LinearLayout.LayoutParams(-1, -2))
        navRow = LinearLayout(activity).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(navRow, LinearLayout.LayoutParams(-1, -2))
        // Swipe left/right to move between pages (buttons do the same).
        val gd = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                if (e1.x - e2.x > 120 && Math.abs(vx) > 200) { nextPage(); return true }
                if (e2.x - e1.x > 120 && Math.abs(vx) > 200) { prevPage(); return true }
                return false
            }
        })
        setOnTouchListener { _, ev -> gd.onTouchEvent(ev) }
        showPage()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun tv(s: String, sz: Float, col: Int, bold: Boolean = false) = TextView(activity).apply {
        text = s; textSize = sz; setTextColor(col)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun bigButton(s: String, accent: Int, card: Int, tx: Int, filled: Boolean, onClick: () -> Unit): TextView {
        val b = TextView(activity)
        b.text = s
        b.textSize = 17f
        b.setTypeface(b.typeface, Typeface.BOLD)
        b.gravity = Gravity.CENTER
        b.setPadding(dp(20), dp(16), dp(20), dp(16))
        val g = GradientDrawable()
        if (filled) {
            g.setColor(accent)
            b.setTextColor(Color.WHITE)
        } else {
            g.setColor(card)
            b.setTextColor(tx)
        }
        g.cornerRadius = dp(28).toFloat()
        b.background = g
        b.setOnClickListener { onClick() }
        return b
    }

    /** Tappable option card; selected cards get an accent outline. */
    private fun optionCard(title: String, desc: String, selected: Boolean, accent: Int, card: Int, tx: Int, sub: Int, onClick: () -> Unit): View {
        val col = LinearLayout(activity)
        col.orientation = VERTICAL
        col.setPadding(dp(18), dp(16), dp(18), dp(16))
        val g = GradientDrawable()
        g.setColor(card)
        g.cornerRadius = dp(20).toFloat()
        if (selected) g.setStroke(dp(2), accent)
        col.background = g
        col.addView(tv(title, 18f, tx, true))
        val d = tv(desc, 14f, sub)
        d.setPadding(0, dp(4), 0, 0)
        col.addView(d)
        col.setOnClickListener { onClick() }
        return col
    }

    private fun showPage() {
        val bg = Themes.bg(prefs, activity)
        val card = Themes.card(prefs, activity)
        val tx = Themes.text(prefs, activity)
        val sub = Themes.sub(prefs, activity)
        val accent = Themes.accent(prefs, activity)
        setBackgroundColor(bg)
        content.removeAllViews()
        when (page) {
            0 -> pageWelcome(tx, sub, accent)
            1 -> pageStyle(card, tx, sub, accent)
            else -> pageHome(card, tx, sub, accent)
        }
        buildDots(accent, sub)
        buildNav(card, tx, accent)
    }

    private fun pageWelcome(tx: Int, sub: Int, accent: Int) {
        val mark = tv("✦", 64f, accent, true)
        mark.gravity = Gravity.CENTER
        mark.setPadding(0, dp(40), 0, dp(8))
        content.addView(mark)
        val title = tv("Welcome to Merlin", 30f, tx, true)
        title.gravity = Gravity.CENTER
        content.addView(title)
        val body = tv("Your new home screen — fast, friendly, and yours to make your own.", 16f, sub)
        body.gravity = Gravity.CENTER
        body.setPadding(dp(16), dp(16), dp(16), 0)
        content.addView(body)
        val hint = tv("It only takes a minute to set up.", 14f, sub)
        hint.gravity = Gravity.CENTER
        hint.setPadding(0, dp(24), 0, 0)
        content.addView(hint)
    }

    private fun pageStyle(card: Int, tx: Int, sub: Int, accent: Int) {
        content.addView(tv("Pick your style", 26f, tx, true))
        val hint = tv("Tap to try it — it saves right away. You can change everything later in Settings.", 14f, sub)
        hint.setPadding(0, dp(8), 0, dp(20))
        content.addView(hint)

        val grid = optionCard("▦  Grid", "Your favourite apps in tidy rows.", prefs.homeStyle == 0, accent, card, tx, sub) {
            prefs.homeStyle = 0; showPage()
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2))
        val list = optionCard("☰  List", "Every app, A to Z, in one smooth scroll.", prefs.homeStyle == 1, accent, card, tx, sub) {
            prefs.homeStyle = 1; showPage()
        }
        val llp = LinearLayout.LayoutParams(-1, -2); llp.setMargins(0, dp(12), 0, 0)
        content.addView(list, llp)

        val presetLabel = tv("PRESET", 12f, sub, true)
        presetLabel.setPadding(0, dp(24), 0, dp(8))
        content.addView(presetLabel)
        for ((name, desc) in listOf(
            "Minimal" to "Calm and clean — list home, no labels.",
            "Classic" to "The familiar grid, labels on.",
            "Dense" to "Fit as much as possible on screen.",
            "One-handed" to "Big targets, easy to reach."
        )) {
            val row = optionCard(name, desc, prefs.preset == name, accent, card, tx, sub) {
                prefs.applyPreset(name); showPage()
            }
            val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, 0, 0, dp(10))
            content.addView(row, lp)
        }
    }

    private fun pageHome(card: Int, tx: Int, sub: Int, accent: Int) {
        content.addView(tv("Make Merlin your Home", 26f, tx, true))
        val body = tv("So your Home button always brings you back here — to your apps, your way.", 15f, sub)
        body.setPadding(0, dp(10), 0, dp(24))
        content.addView(body)

        val go = bigButton("Make Merlin my Home", accent, card, tx, true) {
            onFinish()
            DefaultHome.requestDefaultHome(activity)
        }
        content.addView(go, LinearLayout.LayoutParams(-1, -2))

        val perms = LinearLayout(activity).apply { orientation = VERTICAL }
        val pg = GradientDrawable(); pg.setColor(card); pg.cornerRadius = dp(20).toFloat()
        perms.background = pg
        perms.setPadding(dp(18), dp(16), dp(18), dp(16))
        perms.addView(tv("Totally optional — later in Settings:", 13f, tx, true))
        for (line in listOf(
            "Notifications — for app badges. Merlin only counts them, it never reads them.",
            "Accessibility — swipe down for your notifications.",
            "Device admin — double-tap to lock your screen."
        )) {
            val t = tv("•  $line", 13f, sub)
            t.setPadding(0, dp(8), 0, 0)
            perms.addView(t)
        }
        val plp = LinearLayout.LayoutParams(-1, -2); plp.setMargins(0, dp(20), 0, 0)
        content.addView(perms, plp)

        val later = tv("Maybe later", 15f, accent, true)
        later.gravity = Gravity.CENTER
        later.setPadding(0, dp(24), 0, 0)
        later.setOnClickListener { onFinish() }
        content.addView(later)
    }

    private fun buildDots(accent: Int, sub: Int) {
        dotsRow.removeAllViews()
        for (i in 0..2) {
            val d = View(activity)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setColor(if (i == page) accent else sub)
            d.background = g
            val size = dp(if (i == page) 10 else 8)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.setMargins(dp(4), 0, dp(4), 0)
            dotsRow.addView(d, lp)
        }
    }

    private fun buildNav(card: Int, tx: Int, accent: Int) {
        navRow.removeAllViews()
        if (page > 0) {
            val back = tv("‹ Back", 16f, tx, true)
            back.setPadding(dp(8), dp(14), dp(8), dp(14))
            back.setOnClickListener { prevPage() }
            navRow.addView(back)
        }
        val spacer = View(activity)
        navRow.addView(spacer, LinearLayout.LayoutParams(0, -2, 1f))
        if (page < 2) {
            val next = bigButton(if (page == 0) "Let's go  →" else "Continue  →", accent, card, tx, true) { nextPage() }
            navRow.addView(next, LinearLayout.LayoutParams(-2, -2))
        }
    }

    private fun nextPage() { if (page < 2) { page++; showPage() } }
    private fun prevPage() { if (page > 0) { page--; showPage() } }

    /** Returns true if Back moved to a previous page. False on page 1. */
    fun goBack(): Boolean {
        if (page > 0) { page--; showPage(); return true }
        return false
    }
}
