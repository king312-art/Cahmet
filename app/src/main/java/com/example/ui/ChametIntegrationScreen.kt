package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.ActivityLogEntity
import com.example.ui.theme.LocalJarvisPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChametIntegrationScreen(
  uiState: JarvisUiState,
  onRefreshPermissions: () -> Unit,
  onSaveTargetPackage: (String) -> Unit,
  onSimulateIncomingMessage: () -> Unit,
  onClearLogs: () -> Unit,
  onNavigateBackHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onNavigateBackHome() }

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val palette = LocalJarvisPalette.current

  var packageInput by rememberSaveable { mutableStateOf(uiState.settings.targetPackageName) }
  LaunchedEffect(uiState.settings.targetPackageName) {
    packageInput = uiState.settings.targetPackageName
  }

  // Re-check Android permission status automatically when returning from System Settings
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        onRefreshPermissions()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
      onRefreshPermissions()
    }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .testTag("chamet_setup_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = stringResource(R.string.integration_header),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.integration_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Step 1: Android Notification Listener Access
      item {
        PermissionStepCard(
          title = stringResource(R.string.perm_listener_title),
          statusText =
            if (uiState.permissions.isNotificationListenerEnabled) {
              stringResource(R.string.perm_listener_granted)
            } else {
              stringResource(R.string.perm_listener_missing)
            },
          isGranted = uiState.permissions.isNotificationListenerEnabled,
          buttonLabel = stringResource(R.string.btn_open_listener_settings),
          buttonTestTag = "open_listener_settings_button",
          onActionClick = {
            runCatching {
              val intent =
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
              context.startActivity(intent)
            }
          },
        )
      }

      // Step 2: POST_NOTIFICATIONS Permission for Quick-Copy Banner
      item {
        PermissionStepCard(
          title = stringResource(R.string.perm_post_notif_title),
          statusText =
            if (uiState.permissions.canPostHelperNotifications) {
              stringResource(R.string.perm_post_notif_granted)
            } else {
              stringResource(R.string.perm_post_notif_missing)
            },
          isGranted = uiState.permissions.canPostHelperNotifications,
          buttonLabel = stringResource(R.string.btn_allow_notifications),
          buttonTestTag = "grant_post_notifications_button",
          onActionClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              runCatching {
                val intent =
                  Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                  }
                context.startActivity(intent)
              }
            }
          },
        )
      }

      // Step 3: Strict Chamet Package Lock
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.VerifiedUser,
                contentDescription = "Chamet app filter",
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = stringResource(R.string.chamet_app_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Text(
              text =
                if (uiState.permissions.isChametAppInstalled) {
                  stringResource(R.string.chamet_installed)
                } else {
                  stringResource(R.string.chamet_not_installed)
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
              value = packageInput,
              onValueChange = { packageInput = it },
              label = { Text(stringResource(R.string.label_target_package)) },
              singleLine = true,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("target_package_input"),
            )

            OutlinedButton(
              onClick = { onSaveTargetPackage(packageInput) },
              modifier =
                Modifier
                  .height(48.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("save_package_button"),
            ) {
              Text(stringResource(R.string.btn_save_package))
            }
          }
        }
      }

      // Honest Transparency Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Technical explanation",
                tint = MaterialTheme.colorScheme.secondary,
              )
              Text(
                text = stringResource(R.string.honest_notice_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            Text(
              text = stringResource(R.string.honest_notice_body),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }

      // Safe Pipeline Simulator Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = stringResource(R.string.simulator_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.simulator_desc),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
              onClick = onSimulateIncomingMessage,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(50.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("simulate_chamet_event_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = stringResource(R.string.btn_simulate_chamet),
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(stringResource(R.string.btn_simulate_chamet))
            }
          }
        }
      }

      // Recent Activity Log Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.activity_log_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          if (uiState.recentLogs.isNotEmpty()) {
            TextButton(
              onClick = onClearLogs,
              modifier =
                Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("clear_logs_button"),
            ) {
              Text(stringResource(R.string.btn_clear_logs))
            }
          }
        }
      }

      if (uiState.recentLogs.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Empty activity log",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = stringResource(R.string.activity_log_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(items = uiState.recentLogs, key = { it.id }) { log ->
          ActivityLogItemCard(log = log)
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }
}

@Composable
private fun PermissionStepCard(
  title: String,
  statusText: String,
  isGranted: Boolean,
  buttonLabel: String,
  buttonTestTag: String,
  onActionClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Pending,
          contentDescription = if (isGranted) "Granted" else "Action needed",
          tint =
            if (isGranted) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.secondary
            },
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (isGranted) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        }
      }

      if (!isGranted) {
        Button(
          onClick = onActionClick,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(48.dp)
              .minimumInteractiveComponentSize()
              .testTag(buttonTestTag),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = buttonLabel,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = buttonLabel)
        }
      } else {
        OutlinedButton(
          onClick = onActionClick,
          modifier =
            Modifier
              .height(48.dp)
              .minimumInteractiveComponentSize()
              .testTag(buttonTestTag),
        ) {
          Text(text = "Review in Android Settings")
        }
      }
    }
  }
}

@Composable
private fun ActivityLogItemCard(log: ActivityLogEntity) {
  val palette = LocalJarvisPalette.current
  val timeFormatted =
    SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = log.senderTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = timeFormatted,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Text(
        text = log.messageSnippet,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = log.statusBadge,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }
  }
}
