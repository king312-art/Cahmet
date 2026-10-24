package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.PhonelinkSetup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R

private data class NavDestination(
  val tab: JarvisTab,
  val labelRes: Int,
  val filledIcon: ImageVector,
  val outlinedIcon: ImageVector,
  val testTag: String,
)

@Composable
fun JarvisApp(
  viewModel: JarvisViewModel =
    viewModel(factory = JarvisViewModel.Factory(LocalContext.current)),
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.refreshSystemStatus(context)
  }

  LaunchedEffect(uiState.snackbarMessage) {
    val msg = uiState.snackbarMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.consumeSnackbar()
    }
  }

  val destinations = remember {
    listOf(
      NavDestination(
        tab = JarvisTab.DASHBOARD,
        labelRes = R.string.nav_dashboard,
        filledIcon = Icons.Filled.Dashboard,
        outlinedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_tab_dashboard",
      ),
      NavDestination(
        tab = JarvisTab.REPLY_PRICES,
        labelRes = R.string.nav_reply_prices,
        filledIcon = Icons.Filled.Diamond,
        outlinedIcon = Icons.Outlined.Diamond,
        testTag = "nav_tab_reply_prices",
      ),
      NavDestination(
        tab = JarvisTab.CHAMET_SETUP,
        labelRes = R.string.nav_chamet_setup,
        filledIcon = Icons.Filled.PhonelinkSetup,
        outlinedIcon = Icons.Outlined.PhonelinkSetup,
        testTag = "nav_tab_chamet_setup",
      ),
    )
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpandedScreen = maxWidth >= 720.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (!isExpandedScreen) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
          ) {
            destinations.forEach { dest ->
              val selected = uiState.selectedTab == dest.tab
              val label = stringResource(dest.labelRes)
              NavigationBarItem(
                selected = selected,
                onClick = { viewModel.selectTab(dest.tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier =
          Modifier
            .fillMaxSize()
            .padding(innerPadding)
      ) {
        if (isExpandedScreen) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            destinations.forEach { dest ->
              val selected = uiState.selectedTab == dest.tab
              val label = stringResource(dest.labelRes)
              NavigationRailItem(
                selected = selected,
                onClick = { viewModel.selectTab(dest.tab) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.filledIcon else dest.outlinedIcon,
                    contentDescription = label,
                  )
                },
                label = { Text(label) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
          when (uiState.selectedTab) {
            JarvisTab.DASHBOARD -> {
              DashboardScreen(
                uiState = uiState,
                onStartClick = { viewModel.startAssistant(context) },
                onStopClick = { viewModel.stopAssistant(context) },
                onAutoReplyToggle = { viewModel.setAutoReplyEnabled(it) },
                onAlertSoundToggle = { viewModel.setAlertSoundEnabled(context, it) },
                onTestAlertClick = { viewModel.triggerTestAlertSound(context) },
                onPreviewReplyClick = { viewModel.setPreviewSheetVisible(true) },
                onCopyReplyClick = { viewModel.copyFormattedReply(context) },
                onDismissPreviewSheet = { viewModel.setPreviewSheetVisible(false) },
                onNavigateToReplyEditor = { viewModel.selectTab(JarvisTab.REPLY_PRICES) },
                onNavigateToSetup = { viewModel.selectTab(JarvisTab.CHAMET_SETUP) },
              )
            }

            JarvisTab.REPLY_PRICES -> {
              ReplyAndPricesScreen(
                uiState = uiState,
                onSaveTemplate = { greeting, payment, closing ->
                  viewModel.saveReplyTemplate(context, greeting, payment, closing)
                },
                onSavePackage = { id, price, diamonds, order ->
                  viewModel.savePricePackage(context, id, price, diamonds, order)
                },
                onDeletePackage = { id -> viewModel.deletePricePackage(id) },
                onResetDefaultPrices = { viewModel.resetDefaultPrices(context) },
                onNavigateBackHome = { viewModel.selectTab(JarvisTab.DASHBOARD) },
              )
            }

            JarvisTab.CHAMET_SETUP -> {
              ChametIntegrationScreen(
                uiState = uiState,
                onRefreshPermissions = { viewModel.refreshSystemStatus(context) },
                onSaveTargetPackage = { pkg -> viewModel.saveTargetPackage(context, pkg) },
                onSimulateIncomingMessage = { viewModel.simulateChametIncomingMessage(context) },
                onClearLogs = { viewModel.clearActivityLogs() },
                onNavigateBackHome = { viewModel.selectTab(JarvisTab.DASHBOARD) },
              )
            }
          }
        }
      }
    }
  }
}
