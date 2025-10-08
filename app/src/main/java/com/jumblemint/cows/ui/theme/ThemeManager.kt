package com.jumblemint.cows.ui.theme; // Added package declaration

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.jumblemint.cows.data.model.Settings
import com.jumblemint.cows.data.model.SettingsKeys
import com.jumblemint.cows.data.repository.CattleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Helper to determine a contrasting color (dark for light bg, light for dark bg)
private fun determineOnColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) {
        Color(0xFF000000) // Pure black for light backgrounds
    } else {
        Color(0xFFFFFFFF) // Pure white for dark backgrounds
    }
}

// Public utility function for app-wide luminance-based text color
fun getContrastingTextColor(backgroundColor: Color): Color {
    return determineOnColor(backgroundColor)
}

// Extension function for easier usage
fun Color.contrastingTextColor(): Color {
    return getContrastingTextColor(this)
}

// "On" colors are dynamically determined by default using the base colors.
data class CustomColors(
    // Light Theme Base Colors
    val primaryLight: Color = Color(0xFF0EA5E9),
    val secondaryLight: Color = Color(0xFF64748B),
    val tertiaryLight: Color = Color(0xFF22D3EE),
    val backgroundLight: Color = Color(0xFFF5F5F5),
    val surfaceLight: Color = Color(0xFFF3F2F7),
    val cardBackgroundLight: Color = Color(0xFFE8E8E8),
    val maleColorLight: Color = Color(0xFF1E40AF),
    val femaleColorLight: Color = Color(0xFFBE185D),
    val tbdColorLight: Color = Color(0xFF4B5563),

    // Light Theme "On" Colors
    val onPrimaryLight: Color = determineOnColor(primaryLight),
    val onSecondaryLight: Color = determineOnColor(secondaryLight),
    val onTertiaryLight: Color = determineOnColor(tertiaryLight),
    val onBackgroundLight: Color = determineOnColor(backgroundLight),
    val onSurfaceLight: Color = determineOnColor(surfaceLight),
    // onCardBackgroundLight often defaults to onSurfaceLight or determineOnColor(cardBackgroundLight)

    // Dark Theme Base Colors
    val primaryDark: Color = Color(0xFF7DD3FC),
    val secondaryDark: Color = Color(0xFF94A3B8),
    val tertiaryDark: Color = Color(0xFF67E8F9),
    val backgroundDark: Color = Color(0xFF1C1B1F),
    val surfaceDark: Color = Color(0xFF211F26),
    val cardBackgroundDark: Color = Color(0xFF2C2B2F),
    val maleColorDark: Color = Color(0xFF60A5FA),
    val femaleColorDark: Color = Color(0xFFF472B6),
    val tbdColorDark: Color = Color(0xFF9CA3AF),

    // Dark Theme "On" Colors
    val onPrimaryDark: Color = determineOnColor(primaryDark),
    val onSecondaryDark: Color = determineOnColor(secondaryDark),
    val onTertiaryDark: Color = determineOnColor(tertiaryDark),
    val onBackgroundDark: Color = determineOnColor(backgroundDark),
    val onSurfaceDark: Color = determineOnColor(surfaceDark)
    // onCardBackgroundDark often defaults to onSurfaceDark or determineOnColor(cardBackgroundDark)
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class PresetTheme(val displayName: String) {
    DEFAULT("Default"),
    BLUE("Blue Lagoon"),
    GREEN("Forest Canopy"),
    PURPLE("Royal Amethyst"),
    WARM("Warm Autumn"),
    COOL("Cool Ocean"),
    BRIGHT("Vibrant Citrus"),
    RAINBOW("Chromatic Burst"),
    BOLD_BLUE("Cobalt Depths"),
    BOLD_GREEN("Emerald Isle"),
    BOLD_ORANGE("Sunset Blaze"),
    BOLD_PINK("Magenta Pop"),
    MONOCHROME("Monochrome"),
    SLATE_GRAY("Industrial Slate"),
    SUNSET_GLOW("Sunset Glow"),
    HIGH_CONTRAST("High Contrast"),
    PASTEL_GARDEN("Pastel Garden"),
    EARTHY_COMFORT("Earthy Comfort"),
    CUSTOM("Custom")
}

fun PresetTheme.getColors(): CustomColors {
    when (this) {
        PresetTheme.DEFAULT -> {
            val pl = Color(0xFF0EA5E9); val sl = Color(0xFF64748B); val tl = Color(0xFF22D3EE); val bl = Color(0xFFF5F5F5); val sul = Color(0xFFF3F2F7); val cbl = Color(0xFFE8E8E8); val mcl = Color(0xFF1E40AF); val fcl = Color(0xFFBE185D); val tcl = Color(0xFF4B5563)
            val pd = Color(0xFF7DD3FC); val sd = Color(0xFF94A3B8); val td = Color(0xFF67E8F9); val bd = Color(0xFF1C1B1F); val sud = Color(0xFF211F26); val cbd = Color(0xFF2C2B2F); val mcd = Color(0xFF60A5FA); val fcd = Color(0xFFF472B6); val tcd = Color(0xFF9CA3AF)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.BLUE -> {
            val pl=Color(0xFF1565C0); val sl=Color(0xFF1976D2); val tl=Color(0xFF0277BD); val bl=Color(0xFFE3F2FD); val sul=Color(0xFFBBDEFB); val cbl=Color(0xFFF3F8FF); val mcl=Color(0xFF0D47A1); val fcl=Color(0xFFAD1457); val tcl=Color(0xFF424242)
            val pd=Color(0xFF42A5F5); val sd=Color(0xFF64B5F6); val td=Color(0xFF29B6F6); val bd=Color(0xFF0D1421); val sud=Color(0xFF1A252F); val cbd=Color(0xFF1E2A35); val mcd=Color(0xFF2196F3); val fcd=Color(0xFFE91E63); val tcd=Color(0xFF757575)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.GREEN -> {
            val pl=Color(0xFF2E7D32); val sl=Color(0xFF388E3C); val tl=Color(0xFF00695C); val bl=Color(0xFFE8F5E8); val sul=Color(0xFFC8E6C9); val cbl=Color(0xFFF1F8F1); val mcl=Color(0xFF1565C0); val fcl=Color(0xFFC2185B); val tcl=Color(0xFF5D4037)
            val pd=Color(0xFF66BB6A); val sd=Color(0xFF81C784); val td=Color(0xFF4DB6AC); val bd=Color(0xFF0D1B0D); val sud=Color(0xFF1B2E1B); val cbd=Color(0xFF243028); val mcd=Color(0xFF42A5F5); val fcd=Color(0xFFE91E63); val tcd=Color(0xFF8D6E63)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.PURPLE -> {
            val pl=Color(0xFF7B1FA2); val sl=Color(0xFF8E24AA); val tl=Color(0xFF5E35B1); val bl=Color(0xFFF3E5F5); val sul=Color(0xFFE1BEE7); val cbl=Color(0xFFFAF5FB); val mcl=Color(0xFF3F51B5); val fcl=Color(0xFFE91E63); val tcl=Color(0xFF616161)
            val pd=Color(0xFFBA68C8); val sd=Color(0xFFCE93D8); val td=Color(0xFF9575CD); val bd=Color(0xFF1A0D1F); val sud=Color(0xFF2D1B33); val cbd=Color(0xFF2F2135); val mcd=Color(0xFF7986CB); val fcd=Color(0xFFF06292); val tcd=Color(0xFF9E9E9E)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.WARM -> {
            val pl=Color(0xFFD84315); val sl=Color(0xFFE64A19); val tl=Color(0xFFFF5722); val bl=Color(0xFFFBE9E7); val sul=Color(0xFFFFCCBC); val cbl=Color(0xFFFFF5F3); val mcl=Color(0xFF1565C0); val fcl=Color(0xFFD81B60); val tcl=Color(0xFF6D4C41)
            val pd=Color(0xFFFF7043); val sd=Color(0xFFFF8A65); val td=Color(0xFFFFAB91); val bd=Color(0xFF1F0D0A); val sud=Color(0xFF331A14); val cbd=Color(0xFF35211C); val mcd=Color(0xFF42A5F5); val fcd=Color(0xFFF06292); val tcd=Color(0xFFA1887F)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.COOL -> {
            val pl=Color(0xFF00838F); val sl=Color(0xFF0097A7); val tl=Color(0xFF00ACC1); val bl=Color(0xFFE0F2F1); val sul=Color(0xFFB2DFDB); val cbl=Color(0xFFF0F9F9); val mcl=Color(0xFF1976D2); val fcl=Color(0xFFAD1457); val tcl=Color(0xFF455A64)
            val pd=Color(0xFF4DD0E1); val sd=Color(0xFF80DEEA); val td=Color(0xFFB2EBF2); val bd=Color(0xFF0A1F1F); val sud=Color(0xFF143333); val cbd=Color(0xFF1B3535); val mcd=Color(0xFF64B5F6); val fcd=Color(0xFFE91E63); val tcd=Color(0xFF78909C)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }

        PresetTheme.BRIGHT -> { // Vibrant Citrus
            val pl=Color(0xFFFF6F00); val sl=Color(0xFFFF8F00); val tl=Color(0xFFFFA000); val bl=Color(0xFFFFF8E1); val sul=Color(0xFFFFF3C4); val cbl=Color(0xFFFFFDF5); val mcl=Color(0xFF3F51B5); val fcl=Color(0xFFE91E63); val tcl=Color(0xFF795548)
            val pd=Color(0xFFFFB74D); val sd=Color(0xFFFFCC02); val td=Color(0xFFFDD835); val bd=Color(0xFF2A210A); val sud=Color(0xFF3D321F); val cbd=Color(0xFF3F3521); val mcd=Color(0xFF7986CB); val fcd=Color(0xFFF06292); val tcd=Color(0xFFBCAAA4)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.RAINBOW -> { // Chromatic Burst
            val pl=Color(0xFFE91E63); val sl=Color(0xFF9C27B0); val tl=Color(0xFF673AB7); val bl=Color(0xFFFDF0F3); val sul=Color(0xFFFBE4F0); val cbl=Color(0xFFFFF7F9); val mcl=Color(0xFF2196F3); val fcl=Color(0xFF4CAF50); val tcl=Color(0xFFFFC107)
            val pd=Color(0xFFF48FB1); val sd=Color(0xFFCE93D8); val td=Color(0xFFB39DDB); val bd=Color(0xFF191214); val sud=Color(0xFF2A1E25); val cbd=Color(0xFF2F232A); val mcd=Color(0xFF90CAF9); val fcd=Color(0xFFA5D6A7); val tcd=Color(0xFFFFE082)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.BOLD_BLUE -> { // Cobalt Depths
            val pl=Color(0xFF0D47A1); val sl=Color(0xFF1565C0); val tl=Color(0xFF1976D2); val bl=Color(0xFFE3F2FD); val sul=Color(0xFFBBDEFB); val cbl=Color(0xFFD6EBFC); val mcl=Color(0xFF0D47A1); val fcl=Color(0xFFC2185B); val tcl=Color(0xFF212121)
            val pd=Color(0xFF64B5F6); val sd=Color(0xFF42A5F5); val td=Color(0xFF2196F3); val bd=Color(0xFF0A1017); val sud=Color(0xFF14202B); val cbd=Color(0xFF1A2A3A); val mcd=Color(0xFF90CAF9); val fcd=Color(0xFFF48FB1); val tcd=Color(0xFF616161)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.BOLD_GREEN -> { // Emerald Isle
            val pl=Color(0xFF1B5E20); val sl=Color(0xFF2E7D32); val tl=Color(0xFF388E3C); val bl=Color(0xFFE8F5E9); val sul=Color(0xFFC8E6C9); val cbl=Color(0xFFDEF0DF); val mcl=Color(0xFF0D47A1); val fcl=Color(0xFFC2185B); val tcl=Color(0xFF4E342E)
            val pd=Color(0xFFA5D6A7); val sd=Color(0xFF81C784); val td=Color(0xFF66BB6A); val bd=Color(0xFF0A130A); val sud=Color(0xFF152615); val cbd=Color(0xFF1F301F); val mcd=Color(0xFF90CAF9); val fcd=Color(0xFFF48FB1); val tcd=Color(0xFF8D6E63)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.BOLD_ORANGE -> { // Sunset Blaze
            val pl=Color(0xFFE65100); val sl=Color(0xFFF57C00); val tl=Color(0xFFFF9800); val bl=Color(0xFFFFF3E0); val sul=Color(0xFFFFE0B2); val cbl=Color(0xFFFFF8EF); val mcl=Color(0xFF0D47A1); val fcl=Color(0xFFC2185B); val tcl=Color(0xFF4E342E)
            val pd=Color(0xFFFFB74D); val sd=Color(0xFFFFA726); val td=Color(0xFFFF9800); val bd=Color(0xFF1A1207); val sud=Color(0xFF2B1F0E); val cbd=Color(0xFF352A1A); val mcd=Color(0xFF90CAF9); val fcd=Color(0xFFF48FB1); val tcd=Color(0xFF8D6E63)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.BOLD_PINK -> { // Magenta Pop
            val pl=Color(0xFF880E4F); val sl=Color(0xFFAD1457); val tl=Color(0xFFC2185B); val bl=Color(0xFFFCE4EC); val sul=Color(0xFFF8BBD0); val cbl=Color(0xFFFDF0F5); val mcl=Color(0xFF0D47A1); val fcl=Color(0xFF880E4F); val tcl=Color(0xFF424242)
            val pd=Color(0xFFF48FB1); val sd=Color(0xFFEC407A); val td=Color(0xFFE91E63); val bd=Color(0xFF170A10); val sud=Color(0xFF29141D); val cbd=Color(0xFF331C26); val mcd=Color(0xFF90CAF9); val fcd=Color(0xFFF8BBD0); val tcd=Color(0xFF9E9E9E)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.MONOCHROME -> {
            val pl=Color(0xFF525252); val sl=Color(0xFF737373); val tl=Color(0xFF8A8A8A); val bl=Color(0xFFFFFFFF); val sul=Color(0xFFF3F3F3); val cbl=Color(0xFFFAFAFA); val mcl=Color(0xFF4A4A4A); val fcl=Color(0xFF6E6E6E); val tcl=Color(0xFF7D7D7D)
            val pd=Color(0xFFA3A3A3); val sd=Color(0xFFBDBDBD); val td=Color(0xFFD4D4D4); val bd=Color(0xFF121212); val sud=Color(0xFF1E1E1E); val cbd=Color(0xFF2A2A2A); val mcd=Color(0xFF8C8C8C); val fcd=Color(0xFFAFAFAF); val tcd=Color(0xFFC2C2C2)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.SLATE_GRAY -> { // Industrial Slate
            val pl=Color(0xFF4A5568); val sl=Color(0xFF718096); val tl=Color(0xFFA0AEC0); val bl=Color(0xFFF7FAFC); val sul=Color(0xFFEDF2F7); val cbl=Color(0xFFFFFFFF); val mcl=Color(0xFF2C5282); val fcl=Color(0xFF742A2A); val tcl=Color(0xFF4A5568)
            val pd=Color(0xFFCBD5E0); val sd=Color(0xFFA0AEC0); val td=Color(0xFF718096); val bd=Color(0xFF1A202C); val sud=Color(0xFF2D3748); val cbd=Color(0xFF252E3D); val mcd=Color(0xFF63B3ED); val fcd=Color(0xFFF56565); val tcd=Color(0xFFA0AEC0)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.SUNSET_GLOW -> {
            val pl=Color(0xFFF97316); val sl=Color(0xFFF59E0B); val tl=Color(0xFFEF4444); val bl=Color(0xFFFFF7ED); val sul=Color(0xFFFFEDB3); val cbl=Color(0xFFFFFBEB); val mcl=Color(0xFFB45309); val fcl=Color(0xFFC2410C); val tcl=Color(0xFF7F1D1D)
            val pd=Color(0xFFFB923C); val sd=Color(0xFFFBBF24); val td=Color(0xFFF87171); val bd=Color(0xFF26190D); val sud=Color(0xFF3F2A1A); val cbd=Color(0xFF452E1D); val mcd=Color(0xFFFDBA74); val fcd=Color(0xFFFDA4AF); val tcd=Color(0xFFFCA5A5)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.HIGH_CONTRAST -> {
            val pl=Color(0xFF0000FF); val sl=Color(0xFF008000); val tl=Color(0xFF800080); val bl=Color(0xFFFFFFFF); val sul=Color(0xFFF0F0F0); val cbl=Color(0xFFFAFAFA); val mcl=Color(0xFF00008B); val fcl=Color(0xFF8B0000); val tcl=Color(0xFF2F4F4F)
            val pd=Color(0xFFFFFF00); val sd=Color(0xFF00FF00); val td=Color(0xFFFF00FF); val bd=Color(0xFF000000); val sud=Color(0xFF0D0D0D); val cbd=Color(0xFF1A1A1A); val mcd=Color(0xFFADD8E6); val fcd=Color(0xFF90EE90); val tcd=Color(0xFFDDA0DD)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.PASTEL_GARDEN -> {
            val pl=Color(0xFFFBB4AE); val sl=Color(0xFFB3CDE3); val tl=Color(0xFFCCEBC5); val bl=Color(0xFFFFFDFD); val sul=Color(0xFFFAF0E6); val cbl=Color(0xFFFFFFFF); val mcl=Color(0xFFAFDCEB); val fcl=Color(0xFFFADADD); val tcl=Color(0xFFFFF5BA)
            val pd=Color(0xFFFFD8D3); val sd=Color(0xFFD7E8F7); val td=Color(0xFFE6F5E1); val bd=Color(0xFF262325); val sud=Color(0xFF3D383C); val cbd=Color(0xFF423D40); val mcd=Color(0xFFD3EFF8); val fcd=Color(0xFFFFEBF0); val tcd=Color(0xFFFFFBCF)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }
        PresetTheme.EARTHY_COMFORT -> {
            val pl=Color(0xFF8C6B5C); val sl=Color(0xFF795548); val tl=Color(0xFFA1887F); val bl=Color(0xFFF5F5F0); val sul=Color(0xFFEFEBE9); val cbl=Color(0xFFFFFFFF); val mcl=Color(0xFF5D4037); val fcl=Color(0xFFBF360C); val tcl=Color(0xFF4E342E)
            val pd=Color(0xFFBCAAA4); val sd=Color(0xFFA1887F); val td=Color(0xFF8D6E63); val bd=Color(0xFF201D1C); val sud=Color(0xFF302B29); val cbd=Color(0xFF3A3432); val mcd=Color(0xFF8D6E63); val fcd=Color(0xFFFF5722); val tcd=Color(0xFF795548)
            return CustomColors(primaryLight=pl,secondaryLight=sl,tertiaryLight=tl,backgroundLight=bl,surfaceLight=sul,cardBackgroundLight=cbl,maleColorLight=mcl,femaleColorLight=fcl,tbdColorLight=tcl,onPrimaryLight=determineOnColor(pl),onSecondaryLight=determineOnColor(sl),onTertiaryLight=determineOnColor(tl),onBackgroundLight=determineOnColor(bl),onSurfaceLight=determineOnColor(sul),primaryDark=pd,secondaryDark=sd,tertiaryDark=td,backgroundDark=bd,surfaceDark=sud,cardBackgroundDark=cbd,maleColorDark=mcd,femaleColorDark=fcd,tbdColorDark=tcd,onPrimaryDark=determineOnColor(pd),onSecondaryDark=determineOnColor(sd),onTertiaryDark=determineOnColor(td),onBackgroundDark=determineOnColor(bd),onSurfaceDark=determineOnColor(sud))
        }

        PresetTheme.CUSTOM -> return CustomColors() // Loaded from settings by ThemeManager
    }
}

class ThemeManager(private val repository: CattleRepository) {

    suspend fun getThemeMode(): ThemeMode {
        val mode = repository.getSettingByKey(SettingsKeys.THEME_MODE)?.value
        return try {
            mode?.let { ThemeMode.valueOf(it.uppercase()) } ?: ThemeMode.SYSTEM
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        repository.insertOrUpdateSetting(Settings(SettingsKeys.THEME_MODE, mode.name))
    }

    fun getThemeModeFlow(): Flow<ThemeMode> {
        return repository.getAllSettings().map { settings ->
            val mode = settings.find { it.key == SettingsKeys.THEME_MODE }?.value
            try {
                mode?.let { ThemeMode.valueOf(it.uppercase()) } ?: ThemeMode.SYSTEM
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }
    }

    fun getCustomColors(): Flow<CustomColors> {
        return repository.getAllSettings().map { settings ->
            val settingsMap = settings.associate { it.key to it.value }
            val defaultInitial = CustomColors() // For fallback if absolutely nothing is set

            fun getColor(key: String, default: Color): Color {
                return settingsMap[key]?.let { Color(it.toInt()) } ?: default
            }

            // Load base colors or use defaults
            val primaryLight = getColor(SettingsKeys.THEME_PRIMARY_LIGHT, defaultInitial.primaryLight)
            val secondaryLight = getColor(SettingsKeys.THEME_SECONDARY_LIGHT, defaultInitial.secondaryLight)
            val tertiaryLight = getColor(SettingsKeys.THEME_TERTIARY_LIGHT, defaultInitial.tertiaryLight)
            val backgroundLight = getColor(SettingsKeys.THEME_BACKGROUND_LIGHT, defaultInitial.backgroundLight)
            val surfaceLight = getColor(SettingsKeys.THEME_SURFACE_LIGHT, defaultInitial.surfaceLight)
            val cardBackgroundLight = getColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, defaultInitial.cardBackgroundLight)
            val maleColorLight = getColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, defaultInitial.maleColorLight)
            val femaleColorLight = getColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, defaultInitial.femaleColorLight)
            val tbdColorLight = getColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, defaultInitial.tbdColorLight)

            val primaryDark = getColor(SettingsKeys.THEME_PRIMARY_DARK, defaultInitial.primaryDark)
            val secondaryDark = getColor(SettingsKeys.THEME_SECONDARY_DARK, defaultInitial.secondaryDark)
            val tertiaryDark = getColor(SettingsKeys.THEME_TERTIARY_DARK, defaultInitial.tertiaryDark)
            val backgroundDark = getColor(SettingsKeys.THEME_BACKGROUND_DARK, defaultInitial.backgroundDark)
            val surfaceDark = getColor(SettingsKeys.THEME_SURFACE_DARK, defaultInitial.surfaceDark)
            val cardBackgroundDark = getColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, defaultInitial.cardBackgroundDark)
            val maleColorDark = getColor(SettingsKeys.THEME_MALE_COLOR_DARK, defaultInitial.maleColorDark)
            val femaleColorDark = getColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, defaultInitial.femaleColorDark)
            val tbdColorDark = getColor(SettingsKeys.THEME_TBD_COLOR_DARK, defaultInitial.tbdColorDark)

            // "On" colors are derived directly
            CustomColors(
                primaryLight = primaryLight, onPrimaryLight = determineOnColor(primaryLight),
                secondaryLight = secondaryLight, onSecondaryLight = determineOnColor(secondaryLight),
                tertiaryLight = tertiaryLight, onTertiaryLight = determineOnColor(tertiaryLight),
                backgroundLight = backgroundLight, onBackgroundLight = determineOnColor(backgroundLight),
                surfaceLight = surfaceLight, onSurfaceLight = determineOnColor(surfaceLight),
                cardBackgroundLight = cardBackgroundLight,
                maleColorLight = maleColorLight, femaleColorLight = femaleColorLight, tbdColorLight = tbdColorLight,

                primaryDark = primaryDark, onPrimaryDark = determineOnColor(primaryDark),
                secondaryDark = secondaryDark, onSecondaryDark = determineOnColor(secondaryDark),
                tertiaryDark = tertiaryDark, onTertiaryDark = determineOnColor(tertiaryDark),
                backgroundDark = backgroundDark, onBackgroundDark = determineOnColor(backgroundDark),
                surfaceDark = surfaceDark, onSurfaceDark = determineOnColor(surfaceDark),
                cardBackgroundDark = cardBackgroundDark,
                maleColorDark = maleColorDark, femaleColorDark = femaleColorDark, tbdColorDark = tbdColorDark
            )
        }
    }

    suspend fun updateColor(key: String, color: Color) {
        repository.insertOrUpdateSetting(Settings(key, color.toArgb().toString()))
        // "On" colors are now dynamically determined by CustomColors,
        // so no need to update them separately in settings.
    }

    suspend fun applyPresetTheme(theme: PresetTheme) {
        val presetColors = theme.getColors()

        // Update settings with the colors from the preset
        // Light theme colors
        updateColor(SettingsKeys.THEME_PRIMARY_LIGHT, presetColors.primaryLight)
        updateColor(SettingsKeys.THEME_SECONDARY_LIGHT, presetColors.secondaryLight)
        updateColor(SettingsKeys.THEME_TERTIARY_LIGHT, presetColors.tertiaryLight)
        updateColor(SettingsKeys.THEME_BACKGROUND_LIGHT, presetColors.backgroundLight)
        updateColor(SettingsKeys.THEME_SURFACE_LIGHT, presetColors.surfaceLight)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_LIGHT, presetColors.cardBackgroundLight)
        updateColor(SettingsKeys.THEME_MALE_COLOR_LIGHT, presetColors.maleColorLight)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_LIGHT, presetColors.femaleColorLight)
        updateColor(SettingsKeys.THEME_TBD_COLOR_LIGHT, presetColors.tbdColorLight)

        // Dark theme colors
        updateColor(SettingsKeys.THEME_PRIMARY_DARK, presetColors.primaryDark)
        updateColor(SettingsKeys.THEME_SECONDARY_DARK, presetColors.secondaryDark)
        updateColor(SettingsKeys.THEME_TERTIARY_DARK, presetColors.tertiaryDark)
        updateColor(SettingsKeys.THEME_BACKGROUND_DARK, presetColors.backgroundDark)
        updateColor(SettingsKeys.THEME_SURFACE_DARK, presetColors.surfaceDark)
        updateColor(SettingsKeys.THEME_CARD_BACKGROUND_DARK, presetColors.cardBackgroundDark)
        updateColor(SettingsKeys.THEME_MALE_COLOR_DARK, presetColors.maleColorDark)
        updateColor(SettingsKeys.THEME_FEMALE_COLOR_DARK, presetColors.femaleColorDark)
        updateColor(SettingsKeys.THEME_TBD_COLOR_DARK, presetColors.tbdColorDark)

        // Also update the current theme name setting
        repository.insertOrUpdateSetting(Settings(SettingsKeys.CURRENT_THEME_NAME, theme.name))
    }
    suspend fun getCurrentPreset(): PresetTheme {
        val currentThemeName = repository.getSettingByKey(SettingsKeys.CURRENT_THEME_NAME)?.value
        return try {
            currentThemeName?.let { PresetTheme.valueOf(it) } ?: PresetTheme.DEFAULT
        } catch (e: IllegalArgumentException) {
            PresetTheme.DEFAULT // Fallback if the stored name is invalid
        }
    }

    suspend fun resetToDefaults() {
        applyPresetTheme(PresetTheme.DEFAULT)
    }
}
