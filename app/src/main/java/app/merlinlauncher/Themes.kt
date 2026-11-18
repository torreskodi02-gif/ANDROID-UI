package app.merlinlauncher

import android.app.WallpaperManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable

data class Pal(val name: String, val bg: String, val card: String, val accent: String, val text: String, val sub: String)

/** Theme palettes + accent colours. No Palette dependency: dynamic colour
 *  samples the wallpaper bitmap directly (downscaled to 8x8, averaged). */
object Themes {
    val PALETTES = listOf(
        Pal("Midnight", "#0A0D12", "#141A22", "#7DD3FC", "#FFFFFF", "#9AA4B2"),
        Pal("Cobalt", "#0F1240", "#1A1F5E", "#7C84FF", "#F2F3FF", "#A9AEE8"),
        Pal("Ember", "#1C0F0C", "#2B1914", "#FF7A59", "#FFF2EC", "#D9A596"),
        Pal("Forest", "#0C1A14", "#14281E", "#4ADE80", "#EAFBF1", "#8FBFA2"),
        Pal("Paper", "#F3F1FA", "#FFFFFF", "#3B30F0", "#14123A", "#55527A"),
        Pal("Mono", "#000000", "#111111", "#FFFFFF", "#FFFFFF", "#8A8A8A")
    )
    val ACCENTS = listOf("#3B30F0", "#FF6B57", "#4ADE80", "#F5A524", "#F472B6", "#A78BFA", "#38BDF8", "#FACC15")
    private val LIGHT = Pal("Light", "#F5F3EE", "#FFFFFF", "#3B30F0", "#14123A", "#55527A")

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
}
