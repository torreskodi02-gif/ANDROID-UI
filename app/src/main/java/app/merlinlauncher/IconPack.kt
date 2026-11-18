package app.merlinlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import org.xmlpull.v1.XmlPullParser

/** Icon packs (ADW / Nova theme intents) + shape masking + friendly tile
 *  treatment. Best-effort throughout: a malformed pack never crashes the
 *  launcher — it just falls back to the stock icon. */
object IconPack {

    data class Pack(val label: String, val pkg: String)

    fun listIconPacks(ctx: Context): List<Pack> {
        val pm = ctx.packageManager
        val out = ArrayList<Pack>()
        val seen = HashSet<String>()
        for (action in listOf("org.adw.launcher.THEMES", "com.novalauncher.THEME")) {
            try {
                val infos = pm.queryIntentActivities(Intent(action), 0)
                for (ri in infos) {
                    val ai = ri.activityInfo ?: continue
                    if (seen.add(ai.packageName)) {
                        out.add(Pack(ri.loadLabel(pm)?.toString() ?: ai.packageName, ai.packageName))
                    }
                }
            } catch (e: Exception) {
            }
        }
        out.sortBy { it.label.lowercase() }
        return out
    }

    /** Themed icon for one app from the chosen pack, or null to use the stock icon. */
    fun loadIconFor(ctx: Context, app: AppInfo, packPkg: String): Drawable? {
        if (packPkg.isBlank()) return null
        return try {
            val pm = ctx.packageManager
            val res = pm.getResourcesForApplication(packPkg)
            val xmlId = res.getIdentifier("appfilter", "xml", packPkg)
            if (xmlId == 0) return null
            val parser = res.getXml(xmlId)
            val component = "ComponentInfo{${app.pkg}/${app.cls}}"
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = parser.getAttributeValue(null, "component") ?: ""
                    if (comp == component) {
                        val drawableName = parser.getAttributeValue(null, "drawable") ?: return null
                        val drawId = res.getIdentifier(drawableName, "drawable", packPkg)
                        if (drawId != 0) return res.getDrawable(drawId, null)
                        return null
                    }
                }
                event = parser.next()
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Resolve the icon to draw: pack icon if set and found, else the stock icon. */
    fun resolve(ctx: Context, app: AppInfo, prefs: Prefs): Drawable? {
        val packed = loadIconFor(ctx, app, prefs.iconPack)
        return packed ?: app.icon
    }

    /** Friendly tile treatment: the icon on a rounded squircle tile with a
     *  soft drop shadow and an accent-tinted gradient background, so icons
     *  from different apps look coherent with the palette. */
    fun tile(ctx: Context, icon: Drawable?, accent: Int, sizePx: Int): Drawable? {
        if (icon == null) return null
        return try {
            drawOnTile(ctx, icon, 1, accent, sizePx)
        } catch (e: Exception) {
            icon
        }
    }

    /** Mask an icon into the chosen shape on an accent-tinted background (themed icons). */
    fun shaped(ctx: Context, icon: Drawable?, shape: Int, accent: Int, sizePx: Int): Drawable? {
        if (icon == null) return null
        return try {
            drawOnTile(ctx, icon, shape, accent, sizePx)
        } catch (e: Exception) {
            icon
        }
    }

    /** Shared tile painter: soft shadow, accent-tinted gradient background
     *  clipped to the shape, a thin accent edge, and the icon inset so it
     *  breathes inside the tile. */
    private fun drawOnTile(ctx: Context, icon: Drawable, shape: Int, accent: Int, sizePx: Int): Drawable {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val f = sizePx.toFloat()
        val path = Path()
        when (shape) {
            0 -> path.addCircle(f / 2, f / 2, f / 2, Path.Direction.CW) // Circle
            1 -> { // Squircle (approx: rounded rect, 30% radius)
                val r = f * 0.30f
                path.addRoundRect(0f, 0f, f, f, r, r, Path.Direction.CW)
            }
            2 -> { // Rounded square (15% radius)
                val r = f * 0.15f
                path.addRoundRect(0f, 0f, f, f, r, r, Path.Direction.CW)
            }
            else -> path.addRect(0f, 0f, f, f, Path.Direction.CW) // Square
        }
        // Soft drop shadow: two translucent layers under the tile.
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
        shadow.color = Color.argb(26, 0, 0, 0)
        canvas.save()
        canvas.translate(0f, f * 0.045f)
        canvas.drawPath(path, shadow)
        canvas.restore()
        shadow.color = Color.argb(40, 0, 0, 0)
        canvas.save()
        canvas.translate(0f, f * 0.02f)
        canvas.drawPath(path, shadow)
        canvas.restore()
        // Accent-tinted gradient background, clipped to the shape.
        canvas.save()
        canvas.clipPath(path)
        val top = Themes.blend(accent, Color.WHITE, 0.92f)
        val bottom = Themes.blend(accent, Color.WHITE, 0.78f)
        val bgPaint = Paint()
        bgPaint.shader = LinearGradient(0f, 0f, 0f, f, top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, f, f, bgPaint)
        // Thin accent edge so the tile reads against any wallpaper.
        val edge = Paint(Paint.ANTI_ALIAS_FLAG)
        edge.style = Paint.Style.STROKE
        edge.strokeWidth = (f * 0.02f).coerceAtLeast(1f)
        edge.color = Themes.blend(accent, Color.WHITE, 0.55f)
        canvas.drawPath(path, edge)
        // Icon, inset so it breathes inside the tile.
        val inset = (sizePx * 0.12).toInt()
        val src = drawableToBitmap(icon)
        canvas.drawBitmap(Bitmap.createScaledBitmap(src, sizePx - inset * 2, sizePx - inset * 2, true),
            inset.toFloat(), inset.toFloat(), null)
        canvas.restore()
        return BitmapDrawable(ctx.resources, bmp)
    }

    /** Friendly fallback when an app has no icon: a gradient squircle tile
     *  in the accent colour with the app's first letter in bold white. */
    fun fallbackIcon(ctx: Context, label: String, accent: Int, sizePx: Int): Drawable {
        return try {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val f = sizePx.toFloat()
            val r = f * 0.30f
            val path = Path()
            path.addRoundRect(0f, 0f, f, f, r, r, Path.Direction.CW)
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG)
            shadow.color = Color.argb(40, 0, 0, 0)
            canvas.save()
            canvas.translate(0f, f * 0.03f)
            canvas.drawPath(path, shadow)
            canvas.restore()
            canvas.save()
            canvas.clipPath(path)
            val paint = Paint()
            paint.shader = LinearGradient(0f, 0f, f, f, accent,
                Themes.blend(accent, Color.BLACK, 0.35f), Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, f, f, paint)
            val letter = label.trim().firstOrNull()?.uppercase() ?: "?"
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            textPaint.color = Color.WHITE
            textPaint.textSize = f * 0.44f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.textAlign = Paint.Align.CENTER
            val y = f / 2 - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(letter, f / 2, y, textPaint)
            canvas.restore()
            BitmapDrawable(ctx.resources, bmp)
        } catch (e: Exception) {
            val g = GradientDrawable()
            g.setColor(accent)
            g.cornerRadius = sizePx * 0.30f
            g
        }
    }

    fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null) return d.bitmap
        val w = if (d.intrinsicWidth > 0) d.intrinsicWidth else 96
        val h = if (d.intrinsicHeight > 0) d.intrinsicHeight else 96
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(canvas)
        return bmp
    }
}
