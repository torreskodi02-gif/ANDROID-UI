package app.merlinlauncher

/** Six one-tap Personalities — the heart of v1.2.0.
 *
 *  Every user has their own personality, so one tap restyles the WHOLE
 *  launcher: palette, accent, icon shape, home style, grid, labels,
 *  dock, clock, search, tiles, drawer and animation. Nothing here is
 *  a preview-only mock: apply() writes the full bundle into Prefs, so
 *  the launcher actually becomes that look.
 *
 *  New-pref keys (personality, clockStyle, searchStyle, fontScale,
 *  tileStyle, drawerStyle, homeDim) are written with raw
 *  prefs.p.edit() puts on purpose: the Prefs agent is adding typed
 *  accessors for them concurrently, and raw puts against the same
 *  "ml" SharedPreferences file and the same key names stay correct
 *  whether or not those accessors exist yet. Existing prefs are
 *  written through their typed accessors so their range coercion
 *  (gridCols 3..6, gridRows 3..7, iconSize 0..2, ...) still applies.
 *  Every bundle value below is inside the valid range of its setter.
 */
object Personalities {

    data class Personality(
        val index: Int,
        val name: String,
        val vibe: String,
        // Preview colours (hex) for the picker card: wallpaper bg, card, effective accent.
        val previewBg: String,
        val previewCard: String,
        val previewAccent: String,
        // ---- The bundle: every value apply() writes into Prefs ----
        val theme: Int,        // palette index into Themes.PALETTES
        val themeMode: Int,    // 0 Auto, 1 Light, 2 Dark
        val accent: Int,       // 0 = palette accent, else 1..8 into Themes.ACCENTS
        val iconShape: Int,    // 0 Circle, 1 Squircle, 2 Rounded, 3 Square, 4 Teardrop, 5 Hexagon
        val homeStyle: Int,    // 0 Grid, 1 List
        val gridCols: Int,     // 3..6
        val gridRows: Int,     // 3..7
        val iconSize: Int,     // 0 Small, 1 Medium, 2 Large
        val showLabels: Boolean,
        val labelSize: Int,    // 0 Small, 1 Medium, 2 Large
        val dockStyle: Int,    // 0 Floating rounded, 1 Full-width bar, 2 Transparent
        val dockShow: Boolean,
        val dockCount: Int,    // 3..7
        val animSpeed: Int,    // 0 Off, 1 Normal, 2 Fast
        val showAtAGlance: Boolean,
        val showDateOnHome: Boolean,
        val clockFormat24: Boolean,
        val clockStyle: Int,   // 0 Big, 1 Compact, 2 Date-first
        val searchStyle: Int,  // 0 Pill, 1 Bar, 2 Minimal
        val fontScale: Int,    // 0 Small, 1 Normal, 2 Large
        val tileStyle: Int,    // 0 Flat, 1 Glassy card, 2 Bold outline
        val drawerStyle: Int,  // 0 Grid, 1 List rows
        val homeDim: Int,      // 0..100 percent dim over the wallpaper
        val themedIcons: Boolean,
        val feedShow: Boolean
    )

    /** Effective accent hex for a palette + accent-pref pair. */
    private fun accentHex(theme: Int, accentPref: Int): String {
        if (accentPref <= 0) return Themes.PALETTES[theme.coerceIn(0, Themes.PALETTES.size - 1)].accent
        return Themes.ACCENTS[(accentPref - 1).coerceIn(0, Themes.ACCENTS.size - 1)]
    }

    private fun palBg(theme: Int): String =
        Themes.PALETTES[theme.coerceIn(0, Themes.PALETTES.size - 1)].bg

    private fun palCard(theme: Int): String =
        Themes.PALETTES[theme.coerceIn(0, Themes.PALETTES.size - 1)].card

    fun all(): List<Personality> = listOf(
        Personality(
            index = 0, name = "Minimal",
            vibe = "Calm, quiet, nothing in the way.",
            previewBg = palBg(5), previewCard = palCard(5), previewAccent = accentHex(5, 0),
            theme = 5, themeMode = 2, accent = 0,
            iconShape = 0, homeStyle = 0,
            gridCols = 4, gridRows = 5, iconSize = 0,
            showLabels = false, labelSize = 0,
            dockStyle = 0, dockShow = true, dockCount = 5,
            animSpeed = 1, showAtAGlance = true, showDateOnHome = true,
            clockFormat24 = false, clockStyle = 1, searchStyle = 2,
            fontScale = 0, tileStyle = 0, drawerStyle = 0, homeDim = 0,
            themedIcons = true, feedShow = true
        ),
        Personality(
            index = 1, name = "Sunset Pop",
            vibe = "Warm, bubbly and loud in the best way.",
            previewBg = palBg(2), previewCard = palCard(2), previewAccent = accentHex(2, 0),
            theme = 2, themeMode = 2, accent = 0,
            iconShape = 1, homeStyle = 0,
            gridCols = 4, gridRows = 5, iconSize = 2,
            showLabels = true, labelSize = 2,
            dockStyle = 1, dockShow = true, dockCount = 5,
            animSpeed = 2, showAtAGlance = true, showDateOnHome = true,
            clockFormat24 = false, clockStyle = 0, searchStyle = 0,
            fontScale = 2, tileStyle = 2, drawerStyle = 0, homeDim = 0,
            themedIcons = true, feedShow = true
        ),
        Personality(
            index = 2, name = "Neon Night",
            vibe = "Late-night city glow — dark, dense, electric.",
            previewBg = palBg(0), previewCard = palCard(0), previewAccent = accentHex(0, 5),
            theme = 0, themeMode = 2, accent = 5, // hot neon pink (#F472B6)
            iconShape = 5, homeStyle = 0,
            gridCols = 6, gridRows = 6, iconSize = 1,
            showLabels = false, labelSize = 0,
            dockStyle = 2, dockShow = true, dockCount = 5,
            animSpeed = 2, showAtAGlance = true, showDateOnHome = false,
            clockFormat24 = true, clockStyle = 0, searchStyle = 1,
            fontScale = 1, tileStyle = 2, drawerStyle = 0, homeDim = 10,
            themedIcons = true, feedShow = false
        ),
        Personality(
            index = 3, name = "Ocean Glass",
            vibe = "Cool, glassy and deep like still water.",
            previewBg = palBg(1), previewCard = palCard(1), previewAccent = accentHex(1, 7),
            theme = 1, themeMode = 2, accent = 7, // sky blue (#38BDF8)
            iconShape = 2, homeStyle = 0,
            gridCols = 4, gridRows = 5, iconSize = 1,
            showLabels = true, labelSize = 1,
            dockStyle = 0, dockShow = true, dockCount = 5,
            animSpeed = 1, showAtAGlance = true, showDateOnHome = true,
            clockFormat24 = false, clockStyle = 0, searchStyle = 0,
            fontScale = 1, tileStyle = 1, drawerStyle = 0, homeDim = 0,
            themedIcons = true, feedShow = true
        ),
        Personality(
            index = 4, name = "Paper Day",
            vibe = "Bright, classic and easy to read in daylight.",
            previewBg = palBg(4), previewCard = palCard(4), previewAccent = accentHex(4, 0),
            theme = 4, themeMode = 1, accent = 0,
            iconShape = 2, homeStyle = 0,
            gridCols = 4, gridRows = 6, iconSize = 1,
            showLabels = true, labelSize = 1,
            dockStyle = 1, dockShow = true, dockCount = 5,
            animSpeed = 1, showAtAGlance = true, showDateOnHome = true,
            clockFormat24 = false, clockStyle = 2, searchStyle = 1,
            fontScale = 1, tileStyle = 0, drawerStyle = 1, homeDim = 0,
            themedIcons = false, feedShow = true
        ),
        Personality(
            index = 5, name = "Playful",
            vibe = "Big, bouncy and impossible to ignore.",
            previewBg = palBg(3), previewCard = palCard(3), previewAccent = accentHex(3, 8),
            theme = 3, themeMode = 2, accent = 8, // bright yellow (#FACC15)
            iconShape = 4, homeStyle = 0,
            gridCols = 3, gridRows = 5, iconSize = 2,
            showLabels = true, labelSize = 2,
            dockStyle = 0, dockShow = true, dockCount = 4,
            animSpeed = 2, showAtAGlance = true, showDateOnHome = true,
            clockFormat24 = false, clockStyle = 0, searchStyle = 0,
            fontScale = 2, tileStyle = 2, drawerStyle = 0, homeDim = 0,
            themedIcons = true, feedShow = true
        )
    )

    /** The personality currently applied (0 = Minimal). Reads the raw key so
     *  it works even before the typed accessor lands in Prefs. */
    fun current(prefs: Prefs): Int =
        prefs.p.getInt("personality", 0).coerceIn(0, all().size - 1)

    /** Apply a whole personality: writes the full bundle into Prefs. */
    fun apply(prefs: Prefs, index: Int) {
        val per = all().getOrNull(index.coerceIn(0, all().size - 1)) ?: return
        // Existing prefs via typed accessors (range coercion applies).
        prefs.theme = per.theme
        prefs.themeMode = per.themeMode
        prefs.accent = per.accent
        prefs.iconShape = per.iconShape
        prefs.homeStyle = per.homeStyle
        prefs.gridCols = per.gridCols
        prefs.gridRows = per.gridRows
        prefs.iconSize = per.iconSize
        prefs.showLabels = per.showLabels
        prefs.labelSize = per.labelSize
        prefs.dockStyle = per.dockStyle
        prefs.dockShow = per.dockShow
        prefs.dockCount = per.dockCount
        prefs.animSpeed = per.animSpeed
        prefs.showAtAGlance = per.showAtAGlance
        prefs.showDateOnHome = per.showDateOnHome
        prefs.clockFormat24 = per.clockFormat24
        prefs.themedIcons = per.themedIcons
        prefs.feedShow = per.feedShow
        // New v1.2 prefs via raw keys (same "ml" file, same key names as
        // the typed accessors being added — values already in range).
        prefs.p.edit()
            .putInt("personality", per.index)
            .putInt("clockStyle", per.clockStyle)
            .putInt("searchStyle", per.searchStyle)
            .putInt("fontScale", per.fontScale)
            .putInt("tileStyle", per.tileStyle)
            .putInt("drawerStyle", per.drawerStyle)
            .putInt("homeDim", per.homeDim)
            .apply()
    }
}
