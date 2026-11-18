package app.merlinlauncher

import android.app.WallpaperManager
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.view.View

data class Pal(val name: String, val bg: String, val card: String, val accent: String, val text: String, val sub: String)

/** Theme palettes + accent colours, plus the friendly visual system used by
 *  Home, Drawer and Settings: rounded surfaces, pills, press ripples and a
 *  small type/spacing scale. No Palette dependency: dynamic colour samples
 *  the wallpaper bitmap directly (downscaled to 8x8, averaged).
 *
 *  Colours are deliberately soft — no pure black or pure white surfaces, so
 *  the launcher feels warm and friendly rather than harsh in either mode. */
object Themes {
    val PALETTES = listOf(
        Pal("Midnight", "#0D1117", "#171D27", "#7DD3FC", "#F4F6FA", "#9AA4B2"),
        Pal("Cobalt", "#12163F", "#1C2160", "#7C84FF", "#F2F3FF", "#A9AEE8"),
        Pal("Ember", "#1C0F0C", "#2B1914", "#FF7A59", "#FFF2EC", "#D9A596"),
        Pal("Forest", "#0C1A14", "#14281E", "#4ADE80", "#EAFBF1", "#8FBFA2"),
        Pal("Paper", "#F3F1FA", "#FFFFFF", "#3B30F0", "#14123A", "#55527A"),
        Pal("Mono", "#101014", "#1C1C22", "#EDEDF2", "#F4F4F6", "#9C9CA6")
    )
    val ACCENTS = listOf("#3B30F0", "#FF6B57", "#4ADE80", "#F5A524", "#F472B6", "#A78BFA", "#38BDF8", "#FACC15")
    private val LIGHT = Pal("Light", "#F5F3EE", "#FFFFFF", "#3B30F0", "#14123A", "#55527A")

    /** Typography scale (sp). */
    object Type {
        const val DISPLAY = 30f
        const val TITLE = 22f
        const val SUBTITLE = 16f
        const val BODY = 14f
        const val CAPTION = 12f
        const val TINY = 11f
    }

    /** Spacing scale (dp). */
    object Space {
        const val XS = 4
        const val S = 8
        const val M = 12
        const val L = 16
        const val XL = 24
        const val XXL = 32
    }

    /** Corner radii (dp). Cards sit in the 16–24dp band; pills are fully round. */
    object Radius {
        const val CARD = 20
        const val TILE = 16
        const val CHIP = 12
        const val PILL = 100
    }

    fun palette(prefs: Prefs): Pal = PALETTES[prefs.theme.coerceIn(0, PALETTES.size - 1)]

    fun isNight(ctx: Context): Boolean =
        (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    /** Background: Light mode forces the light palette; Auto follows the system night flag. */
    fun bg(prefs: Prefs, ctx: Context): Int {
        val pal = palette(prefs)
        return when (prefs.themeMode) {
            1 -> Color.parseColor(LIGHT.bg)
            0 -> if (isNight(ctx)) Color.parseColor(pal.bg) else Color.parseColor(LIGHT.bg)
            else -> Color.parseColor(pal.bg)
        }
    }

    fun card(prefs: Prefs, ctx: Context): Int {
        val pal = palette(prefs)
        return when (prefs.themeMode) {
            1 -> Color.parseColor(LIGHT.card)
            0 -> if (isNight(ctx)) Color.parseColor(pal.card) else Color.parseColor(LIGHT.card)
            else -> Color.parseColor(pal.card)
        }
    }

    fun text(prefs: Prefs, ctx: Context): Int {
        val pal = palette(prefs)
        return when (prefs.themeMode) {
            1 -> Color.parseColor(LIGHT.text)
            0 -> if (isNight(ctx)) Color.parseColor(pal.text) else Color.parseColor(LIGHT.text)
            else -> Color.parseColor(pal.text)
        }
    }

    fun sub(prefs: Prefs, ctx: Context): Int {
        val pal = palette(prefs)
        return when (prefs.themeMode) {
            1 -> Color.parseColor(LIGHT.sub)
            0 -> if (isNight(ctx)) Color.parseColor(pal.sub) else Color.parseColor(LIGHT.sub)
            else -> Color.parseColor(pal.sub)
        }
    }

    /** Accent: dynamic colour (wallpaper average) wins, then the picked accent, then the palette accent. */
    fun accent(prefs: Prefs, ctx: Context): Int {
        if (prefs.dynamicColor) {
            wallpaperAverage(ctx)?.let { return it }
        }
        val pal = palette(prefs)
        val i = prefs.accent
        return if (i > 0 && i <= ACCENTS.size) Color.parseColor(ACCENTS[i - 1]) else Color.parseColor(pal.accent)
    }

    /** Average colour of the current wallpaper, sampled at 8x8. Null if unavailable. */
    fun wallpaperAverage(ctx: Context): Int? {
        return try {
            val d = WallpaperManager.getInstance(ctx).drawable ?: return null
            val bmp = (d as? BitmapDrawable)?.bitmap ?: return null
            val small = Bitmap.createScaledBitmap(bmp, 8, 8, true)
            var r = 0L; var g = 0L; var b = 0L; var n = 0L
            for (x in 0 until 8) for (y in 0 until 8) {
                val px = small.getPixel(x, y)
                r += Color.red(px); g += Color.green(px); b += Color.blue(px); n++
            }
            if (n == 0L) return null
            var cr = (r / n).toInt(); var cg = (g / n).toInt(); var cb = (b / n).toInt()
            // Lift very dark / very pale averages so the accent stays visible on both themes.
            val lum = 0.299 * cr + 0.587 * cg + 0.114 * cb
            if (lum < 70) { cr += 60; cg += 60; cb += 60 }
            if (lum > 200) { cr -= 50; cg -= 50; cb -= 50 }
            Color.rgb(cr.coerceIn(0, 255), cg.coerceIn(0, 255), cb.coerceIn(0, 255))
        } catch (e: Exception) {
            null
        }
    }

    // ---- Friendly visual system: surfaces, ripples, colour helpers ----

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    /** [color] with its alpha scaled by [alpha] (0f..1f). */
    fun withAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    /** Linear blend of two colours. [ratio] is the fraction of [b]. */
    fun blend(a: Int, b: Int, ratio: Float): Int {
        val r = (Color.red(a) * (1 - ratio) + Color.red(b) * ratio).toInt()
        val g = (Color.green(a) * (1 - ratio) + Color.green(b) * ratio).toInt()
        val bl = (Color.blue(a) * (1 - ratio) + Color.blue(b) * ratio).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), bl.coerceIn(0, 255))
    }

    /** Rounded rectangle surface, optionally with a thin outline stroke. */
    fun roundedBackground(ctx: Context, radiusDp: Int, color: Int, strokeColor: Int? = null, strokeWidthDp: Int = 1): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(ctx, radiusDp).toFloat()
        if (strokeColor != null) g.setStroke(dp(ctx, strokeWidthDp), strokeColor)
        return g
    }

    /** Fully round pill surface (buttons, search field, chips). */
    fun pillBackground(ctx: Context, color: Int, strokeColor: Int? = null): GradientDrawable =
        roundedBackground(ctx, Radius.PILL, color, strokeColor)

    /** Card surface: 20dp corners, in the 16–24dp band. */
    fun cardBackground(ctx: Context, color: Int): GradientDrawable =
        roundedBackground(ctx, Radius.CARD, color)

    /** Card surface with a soft native shadow. Sets the view's background and elevation. */
    fun applyCard(view: View, ctx: Context, color: Int, elevationDp: Float = 6f) {
        view.background = cardBackground(ctx, color)
        view.elevation = elevationDp * ctx.resources.displayMetrics.density
    }

    /** Press ripple over [content], clipped to a rounded mask. On API 21+ this
     *  is a real RippleDrawable; below that it falls back to a state-list
     *  drawable that tints the surface while pressed. */
    fun rippleDrawable(ctx: Context, rippleColor: Int, content: Drawable?, radiusDp: Int): Drawable {
        val mask = roundedBackground(ctx, radiusDp, Color.WHITE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable(ColorStateList.valueOf(rippleColor), content, mask)
        } else {
            val pressed = roundedBackground(ctx, radiusDp, rippleColor)
            val normal = content ?: roundedBackground(ctx, radiusDp, Color.TRANSPARENT)
            StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), pressed)
                addState(intArrayOf(), normal)
            }
        }
    }

    /** Rounded surface that ripples when pressed. [baseColor] may be
     *  Color.TRANSPARENT for a ripple over whatever is underneath. */
    fun pressable(ctx: Context, baseColor: Int, radiusDp: Int, rippleColor: Int): Drawable =
        rippleDrawable(ctx, rippleColor, roundedBackground(ctx, radiusDp, baseColor), radiusDp)

    /** Subtle accent-tinted surface for chips and the search field. */
    fun chip(prefs: Prefs, ctx: Context): Int = blend(card(prefs, ctx), accent(prefs, ctx), 0.08f)

    /** Soft hairline divider colour, derived from the text colour. */
    fun divider(prefs: Prefs, ctx: Context): Int = withAlpha(text(prefs, ctx), 0.14f)
}
