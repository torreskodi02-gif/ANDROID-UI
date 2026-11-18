package app.merlinlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

/** Icon packs (ADW / Nova theme intents) + shape masking. Best-effort throughout:
 *  a malformed pack never crashes the launcher — it just falls back to the stock icon. */
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

    /** Mask an icon into the chosen shape on an accent-tinted background (themed icons). */
    fun shaped(ctx: Context, icon: Drawable?, shape: Int, accent: Int, sizePx: Int): Drawable? {
        if (icon == null) return null
        return try {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val path = Path()
            val f = sizePx.toFloat()
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
            canvas.save()
            canvas.clipPath(path)
            val bg = Paint()
            bg.color = blend(accent, Color.WHITE, 0.82f)
            canvas.drawRect(0f, 0f, f, f, bg)
            val inset = (sizePx * 0.12).toInt()
            val src = drawableToBitmap(icon)
            canvas.drawBitmap(Bitmap.createScaledBitmap(src, sizePx - inset * 2, sizePx - inset * 2, true),
                inset.toFloat(), inset.toFloat(), null)
            canvas.restore()
            BitmapDrawable(ctx.resources, bmp)
        } catch (e: Exception) {
            icon
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

    private fun blend(a: Int, b: Int, ratio: Float): Int {
        val r = (Color.red(a) * (1 - ratio) + Color.red(b) * ratio).toInt()
        val g = (Color.green(a) * (1 - ratio) + Color.green(b) * ratio).toInt()
        val bl = (Color.blue(a) * (1 - ratio) + Color.blue(b) * ratio).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), bl.coerceIn(0, 255))
    }
}
