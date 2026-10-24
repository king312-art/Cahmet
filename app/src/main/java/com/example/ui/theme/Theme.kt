package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class JarvisExtendedPalette(
  val heroOverlayBrush: Brush,
  val activeBannerBrush: Brush,
  val inactiveBannerBrush: Brush,
  val startButtonBrush: Brush,
  val stopButtonBrush: Brush,
  val diamondCardBrush: Brush,
  val diamondGlowAccent: Color,
  val emeraldBright: Color,
  val silverBorder: Color,
  val heroForeground: Color,
)

val LocalJarvisPalette = staticCompositionLocalOf {
  JarvisExtendedPalette(
    heroOverlayBrush = Brush.linearGradient(listOf(EmeraldDarkHeader, EmeraldDeep, DiamondBluePrimary)),
    activeBannerBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, DiamondBluePrimary)),
    inactiveBannerBrush = Brush.horizontalGradient(listOf(SilverSlateText, PlatinumBorder)),
    startButtonBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, EmeraldGlow)),
    stopButtonBrush = Brush.horizontalGradient(listOf(StopCrimsonLight, StopCrimsonDark)),
    diamondCardBrush = Brush.linearGradient(listOf(PureDiamondWhite, DiamondIceContainer)),
    diamondGlowAccent = DiamondBlueGlow,
    emeraldBright = EmeraldGlow,
    silverBorder = PlatinumBorder,
    heroForeground = PureDiamondWhite,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimaryLight,
  onPrimary = PureDiamondWhite,
  primaryContainer = EmeraldMintContainer,
  onPrimaryContainer = OnEmeraldMintContainer,
  secondary = DiamondBluePrimary,
  onSecondary = PureDiamondWhite,
  secondaryContainer = DiamondIceContainer,
  onSecondaryContainer = OnDiamondIceContainer,
  tertiary = EmeraldDeep,
  onTertiary = PureDiamondWhite,
  tertiaryContainer = BrushedSilverSurface,
  onTertiaryContainer = DeepSlateInk,
  background = FrostedWhiteBg,
  onBackground = DeepSlateInk,
  surface = PureDiamondWhite,
  onSurface = DeepSlateInk,
  surfaceVariant = BrushedSilverSurface,
  onSurfaceVariant = SilverSlateText,
  outline = PlatinumBorder,
  error = StopCrimsonLight,
  onError = PureDiamondWhite,
  errorContainer = StopContainerLight,
  onErrorContainer = OnStopContainerLight,
)

private val DarkColorScheme = darkColorScheme(
  primary = EmeraldBrightDark,
  onPrimary = OnEmeraldMintContainer,
  primaryContainer = EmeraldDeep,
  onPrimaryContainer = EmeraldMintContainer,
  secondary = DiamondBlueGlow,
  onSecondary = OnDiamondIceContainer,
  secondaryContainer = OnDiamondIceContainer,
  onSecondaryContainer = DiamondIceContainer,
  tertiary = DiamondCyanSpark,
  onTertiary = OnDiamondIceContainer,
  tertiaryContainer = ObsidianElevatedSurface,
  onTertiaryContainer = SilverMistText,
  background = ObsidianEmeraldBg,
  onBackground = SilverMistText,
  surface = ObsidianCardSurface,
  onSurface = PureDiamondWhite,
  surfaceVariant = ObsidianElevatedSurface,
  onSurfaceVariant = MutedSilverDark,
  outline = SilverSlateText,
  error = StopCrimsonDark,
  onError = OnStopContainerLight,
  errorContainer = OnStopContainerLight,
  onErrorContainer = StopContainerLight,
)

val JarvisShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val extendedPalette = if (darkTheme) {
    JarvisExtendedPalette(
      heroOverlayBrush = Brush.linearGradient(
        listOf(
          EmeraldDarkHeader.copy(alpha = 0.90f),
          EmeraldDeep.copy(alpha = 0.82f),
          OnDiamondIceContainer.copy(alpha = 0.85f),
        )
      ),
      activeBannerBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, DiamondBluePrimary)),
      inactiveBannerBrush = Brush.horizontalGradient(listOf(ObsidianElevatedSurface, SilverSlateText)),
      startButtonBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, EmeraldGlow, DiamondBluePrimary)),
      stopButtonBrush = Brush.horizontalGradient(listOf(OnStopContainerLight, StopCrimsonLight)),
      diamondCardBrush = Brush.linearGradient(
        listOf(ObsidianCardSurface, ObsidianElevatedSurface)
      ),
      diamondGlowAccent = DiamondCyanSpark,
      emeraldBright = EmeraldBrightDark,
      silverBorder = SilverSlateText,
      heroForeground = PureDiamondWhite,
    )
  } else {
    JarvisExtendedPalette(
      heroOverlayBrush = Brush.linearGradient(
        listOf(
          EmeraldDarkHeader.copy(alpha = 0.88f),
          EmeraldDeep.copy(alpha = 0.78f),
          DiamondBluePrimary.copy(alpha = 0.76f),
        )
      ),
      activeBannerBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, DiamondBluePrimary)),
      inactiveBannerBrush = Brush.horizontalGradient(listOf(SilverSlateText, SilverSlateText.copy(alpha = 0.85f))),
      startButtonBrush = Brush.horizontalGradient(listOf(EmeraldPrimaryLight, EmeraldGlow, DiamondBluePrimary)),
      stopButtonBrush = Brush.horizontalGradient(listOf(StopCrimsonLight, OnStopContainerLight)),
      diamondCardBrush = Brush.linearGradient(
        listOf(PureDiamondWhite, FrostedWhiteBg, DiamondIceContainer.copy(alpha = 0.55f))
      ),
      diamondGlowAccent = DiamondBlueGlow,
      emeraldBright = EmeraldGlow,
      silverBorder = PlatinumBorder,
      heroForeground = PureDiamondWhite,
    )
  }

  CompositionLocalProvider(LocalJarvisPalette provides extendedPalette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = JarvisShapes,
      content = content,
    )
  }
}
