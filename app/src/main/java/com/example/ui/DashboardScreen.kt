package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalJarvisPalette
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  uiState: JarvisUiState,
  onStartClick: () -> Unit,
  onStopClick: () -> Unit,
  onAutoReplyToggle: (Boolean) -> Unit,
  onAlertSoundToggle: (Boolean) -> Unit,
  onTestAlertClick: () -> Unit,
  onPreviewReplyClick: () -> Unit,
  onCopyReplyClick: () -> Unit,
  onDismissPreviewSheet: () -> Unit,
  onNavigateToReplyEditor: () -> Unit,
  onNavigateToSetup: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var justCopied by remember { mutableStateOf(false) }

  LaunchedEffect(justCopied) {
    if (justCopied) {
      delay(1_800L)
      justCopied = false
    }
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .testTag("dashboard_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        DiamondHeroStatusCard(
          isRunning = uiState.settings.isAssistantRunning,
          isSoundPlaying = uiState.isAlertSoundPlaying,
        )
      }

      item {
        PrimaryStartStopCard(
          isRunning = uiState.settings.isAssistantRunning,
          isSoundPlaying = uiState.isAlertSoundPlaying,
          onStartClick = onStartClick,
          onStopClick = onStopClick,
        )
      }

      item {
        AutomationSwitchesCard(
          isAutoReplyEnabled = uiState.settings.isAutoReplyEnabled,
          isAlertSoundEnabled = uiState.settings.isAlertSoundEnabled,
          onAutoReplyToggle = onAutoReplyToggle,
          onAlertSoundToggle = onAlertSoundToggle,
        )
      }

      item {
        QuickActionsCard(
          isAlertPlaying = uiState.isAlertSoundPlaying,
          justCopied = justCopied,
          onTestAlertClick = onTestAlertClick,
          onPreviewReplyClick = onPreviewReplyClick,
          onCopyReplyClick = {
            justCopied = true
            onCopyReplyClick()
          },
        )
      }

      item {
        SellerReadinessCard(
          packageCount = uiState.pricePackages.size,
          isListenerConnected = uiState.permissions.isNotificationListenerEnabled,
          onEditPricesClick = onNavigateToReplyEditor,
          onOpenSetupClick = onNavigateToSetup,
        )
      }

      item { Spacer(modifier = Modifier.height(8.dp)) }
    }
  }

  if (uiState.showPreviewSheet) {
    ModalBottomSheet(
      onDismissRequest = onDismissPreviewSheet,
      containerColor = MaterialTheme.colorScheme.surface,
    ) {
      ReplyPreviewSheetContent(
        formattedReply = uiState.formattedReply,
        onCopyClick = {
          justCopied = true
          onCopyReplyClick()
        },
        onEditClick = {
          onDismissPreviewSheet()
          onNavigateToReplyEditor()
        },
        onCloseClick = onDismissPreviewSheet,
      )
    }
  }
}

@Composable
private fun DiamondHeroStatusCard(
  isRunning: Boolean,
  isSoundPlaying: Boolean,
) {
  val palette = LocalJarvisPalette.current
  val pulseTransition = rememberInfiniteTransition(label = "assistant_pulse")
  val pulseAlpha by
    pulseTransition.animateFloat(
      initialValue = 0.65f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
      label = "pulse_alpha",
    )

  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .testTag("assistant_status_card"),
    shape = MaterialTheme.shapes.extraLarge,
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Image(
        painter = painterResource(id = R.drawable.img_diamond_hero),
        contentDescription = stringResource(R.string.cd_hero_banner),
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize(),
      )

      Box(
        modifier =
          Modifier
            .matchParentSize()
            .background(palette.heroOverlayBrush)
      )

      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Faceted Diamond Logo Emblem
          Surface(
            modifier =
              Modifier
                .size(56.dp)
                .border(1.5.dp, palette.diamondGlowAccent, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Diamond,
                contentDescription = stringResource(R.string.cd_diamond_logo),
                tint = palette.diamondGlowAccent,
                modifier = Modifier.size(32.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              color = palette.heroForeground,
            )
            Text(
              text = stringResource(R.string.brand_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = palette.heroForeground.copy(alpha = 0.85f),
            )
          }
        }

        // Clear Status Indicator Pill (ON / OFF)
        val statusText =
          if (isRunning) {
            stringResource(R.string.status_assistant_on)
          } else {
            stringResource(R.string.status_assistant_off)
          }

        Surface(
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("assistant_status_indicator")
              .semantics { contentDescription = statusText },
          shape = MaterialTheme.shapes.medium,
          color =
            if (isRunning) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
            } else {
              MaterialTheme.colorScheme.surface.copy(alpha = 0.16f)
            },
          border =
            BorderStroke(
              width = 1.dp,
              color =
                if (isRunning) {
                  palette.diamondGlowAccent
                } else {
                  palette.heroForeground.copy(alpha = 0.35f)
                },
            ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .size(12.dp)
                  .alpha(if (isRunning) pulseAlpha else 0.7f)
                  .clip(CircleShape)
                  .background(
                    if (isRunning) palette.emeraldBright else palette.silverBorder
                  )
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                color = palette.heroForeground,
              )
              Text(
                text =
                  if (isRunning) {
                    stringResource(R.string.status_on_desc)
                  } else {
                    stringResource(R.string.status_off_desc)
                  },
                style = MaterialTheme.typography.bodySmall,
                color = palette.heroForeground.copy(alpha = 0.88f),
              )
            }

            AnimatedVisibility(visible = isSoundPlaying) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.NotificationsActive,
                    contentDescription = "Alert sound active",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(14.dp),
                  )
                  Text(
                    text = "CHIME",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PrimaryStartStopCard(
  isRunning: Boolean,
  isSoundPlaying: Boolean,
  onStartClick: () -> Unit,
  onStopClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Master Assistant Controls",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Large START Button
        Box(
          modifier =
            Modifier
              .weight(1f)
              .height(64.dp)
              .clip(MaterialTheme.shapes.medium)
              .background(palette.startButtonBrush)
              .border(
                width = if (isRunning) 2.dp else 1.dp,
                color = if (isRunning) palette.diamondGlowAccent else palette.silverBorder,
                shape = MaterialTheme.shapes.medium,
              )
              .clickable(role = Role.Button, onClick = onStartClick)
              .minimumInteractiveComponentSize()
              .testTag("start_assistant_button"),
          contentAlignment = Alignment.Center,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.PlayArrow,
              contentDescription = stringResource(R.string.btn_start),
              tint = palette.heroForeground,
              modifier = Modifier.size(26.dp),
            )
            Text(
              text = stringResource(R.string.btn_start),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = palette.heroForeground,
            )
          }
        }

        // Large STOP Button (stops assistant AND any active sound immediately)
        val stopHighlight = !isRunning && !isSoundPlaying
        Button(
          onClick = onStopClick,
          modifier =
            Modifier
              .weight(1f)
              .height(64.dp)
              .minimumInteractiveComponentSize()
              .testTag("stop_assistant_button"),
          shape = MaterialTheme.shapes.medium,
          colors =
            ButtonDefaults.buttonColors(
              containerColor =
                if (stopHighlight) {
                  MaterialTheme.colorScheme.errorContainer
                } else {
                  MaterialTheme.colorScheme.error
                },
              contentColor =
                if (stopHighlight) {
                  MaterialTheme.colorScheme.onErrorContainer
                } else {
                  MaterialTheme.colorScheme.onError
                },
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        ) {
          Icon(
            imageVector = Icons.Filled.Stop,
            contentDescription = stringResource(R.string.btn_stop),
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.btn_stop),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun AutomationSwitchesCard(
  isAutoReplyEnabled: Boolean,
  isAlertSoundEnabled: Boolean,
  onAutoReplyToggle: (Boolean) -> Unit,
  onAlertSoundToggle: (Boolean) -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Auto Reply ON/OFF Switch
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { onAutoReplyToggle(!isAutoReplyEnabled) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondaryContainer,
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = stringResource(R.string.label_auto_reply),
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              "${stringResource(R.string.label_auto_reply)}: ${if (isAutoReplyEnabled) "ON" else "OFF"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.desc_auto_reply),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = isAutoReplyEnabled,
          onCheckedChange = onAutoReplyToggle,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("auto_reply_switch"),
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
      }

      HorizontalDivider(
        modifier = Modifier.padding(vertical = 10.dp),
        color = palette.silverBorder.copy(alpha = 0.45f),
      )

      // Alert Sound ON/OFF Switch
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { onAlertSoundToggle(!isAlertSoundEnabled) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(42.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector =
                if (isAlertSoundEnabled) {
                  Icons.AutoMirrored.Filled.VolumeUp
                } else {
                  Icons.AutoMirrored.Outlined.VolumeOff
                },
              contentDescription = stringResource(R.string.label_alert_sound),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              "${stringResource(R.string.label_alert_sound)}: ${if (isAlertSoundEnabled) "ON" else "OFF"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.desc_alert_sound),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = isAlertSoundEnabled,
          onCheckedChange = onAlertSoundToggle,
          modifier =
            Modifier
              .minimumInteractiveComponentSize()
              .testTag("alert_sound_switch"),
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
              checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
      }
    }
  }
}

@Composable
private fun QuickActionsCard(
  isAlertPlaying: Boolean,
  justCopied: Boolean,
  onTestAlertClick: () -> Unit,
  onPreviewReplyClick: () -> Unit,
  onCopyReplyClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current
  val copyContainerColor by
    animateColorAsState(
      targetValue =
        if (justCopied) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.secondary
        },
      label = "copy_btn_color",
    )

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Reply & Sound Tools",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      // Test Alert Sound Button
      OutlinedButton(
        onClick = onTestAlertClick,
        modifier =
          Modifier
            .fillMaxWidth()
            .height(52.dp)
            .minimumInteractiveComponentSize()
            .testTag("test_alert_button"),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
      ) {
        Icon(
          imageVector = Icons.Filled.NotificationsActive,
          contentDescription = stringResource(R.string.btn_test_alert),
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (isAlertPlaying) {
              stringResource(R.string.btn_alert_playing)
            } else {
              stringResource(R.string.btn_test_alert)
            },
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Preview Reply Button
        OutlinedButton(
          onClick = onPreviewReplyClick,
          modifier =
            Modifier
              .weight(1f)
              .height(54.dp)
              .minimumInteractiveComponentSize()
              .testTag("preview_reply_button"),
          shape = MaterialTheme.shapes.medium,
          border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary),
        ) {
          Icon(
            imageVector = Icons.Filled.Visibility,
            contentDescription = stringResource(R.string.btn_preview_reply),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.btn_preview_reply),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
          )
        }

        // Copy Reply Button
        Button(
          onClick = onCopyReplyClick,
          modifier =
            Modifier
              .weight(1f)
              .height(54.dp)
              .minimumInteractiveComponentSize()
              .testTag("copy_reply_button"),
          shape = MaterialTheme.shapes.medium,
          colors =
            ButtonDefaults.buttonColors(
              containerColor = copyContainerColor,
              contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
          Icon(
            imageVector =
              if (justCopied) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
            contentDescription = stringResource(R.string.btn_copy_reply),
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text =
              if (justCopied) {
                stringResource(R.string.btn_copied_reply)
              } else {
                stringResource(R.string.btn_copy_reply)
              },
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun SellerReadinessCard(
  packageCount: Int,
  isListenerConnected: Boolean,
  onEditPricesClick: () -> Unit,
  onOpenSetupClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onEditPricesClick)
            .padding(vertical = 4.dp)
            .testTag("shortcut_edit_prices"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.EditNote,
          contentDescription = "Edit diamond price list",
          tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "$packageCount Diamond Packages Saved",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tap to customize PKR prices, greeting, or payment numbers",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      HorizontalDivider(color = palette.silverBorder.copy(alpha = 0.45f))

      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onOpenSetupClick)
            .padding(vertical = 4.dp)
            .testTag("shortcut_chamet_setup"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Outlined.Shield,
          contentDescription = "Chamet notification setup",
          tint =
            if (isListenerConnected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.secondary
            },
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text =
              if (isListenerConnected) {
                "Chamet Notification Detector: Connected"
              } else {
                "Enable Chamet Notification Detection"
              },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Tap to view Android permission status & test incoming Chamet alert",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ReplyPreviewSheetContent(
  formattedReply: String,
  onCopyClick: () -> Unit,
  onEditClick: () -> Unit,
  onCloseClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag("reply_preview_sheet"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Text(
      text = stringResource(R.string.preview_sheet_title),
      style = MaterialTheme.typography.headlineSmall,
      color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
      text = stringResource(R.string.preview_sheet_sub),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
      border = BorderStroke(1.dp, palette.silverBorder),
    ) {
      Text(
        text = formattedReply,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
          Modifier
            .padding(16.dp)
            .testTag("preview_reply_text"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      OutlinedButton(
        onClick = onEditClick,
        modifier =
          Modifier
            .weight(1f)
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag("sheet_edit_reply_button"),
      ) {
        Text(text = "Edit Reply")
      }
      Button(
        onClick = onCopyClick,
        modifier =
          Modifier
            .weight(1f)
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag("sheet_copy_reply_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.ContentCopy,
          contentDescription = "Copy reply",
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.btn_copy_reply))
      }
    }

    OutlinedButton(
      onClick = onCloseClick,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(48.dp)
          .minimumInteractiveComponentSize()
          .testTag("sheet_close_button"),
    ) {
      Text(text = stringResource(R.string.btn_close))
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
