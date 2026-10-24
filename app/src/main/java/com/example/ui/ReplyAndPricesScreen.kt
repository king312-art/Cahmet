package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PricePackageEntity
import com.example.ui.theme.LocalJarvisPalette

@Composable
fun ReplyAndPricesScreen(
  uiState: JarvisUiState,
  onSaveTemplate: (greeting: String, paymentNumbers: String, closing: String) -> Unit,
  onSavePackage: (id: Int, priceText: String, diamondsText: String, sortOrder: Int) -> Unit,
  onDeletePackage: (id: Int) -> Unit,
  onResetDefaultPrices: () -> Unit,
  onNavigateBackHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onNavigateBackHome() }

  val palette = LocalJarvisPalette.current

  var greetingInput by rememberSaveable { mutableStateOf(uiState.settings.greeting) }
  var paymentInput by rememberSaveable { mutableStateOf(uiState.settings.paymentNumbers) }
  var closingInput by rememberSaveable { mutableStateOf(uiState.settings.closing) }

  LaunchedEffect(
    uiState.settings.greeting,
    uiState.settings.paymentNumbers,
    uiState.settings.closing,
  ) {
    greetingInput = uiState.settings.greeting
    paymentInput = uiState.settings.paymentNumbers
    closingInput = uiState.settings.closing
  }

  var editingPackage by remember { mutableStateOf<PricePackageEntity?>(null) }
  var isAddingNewPackage by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .testTag("reply_prices_list"),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Price List Header & Actions
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Text(
              text = stringResource(R.string.section_price_list),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = stringResource(R.string.section_price_list_sub),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { isAddingNewPackage = true },
                modifier =
                  Modifier
                    .weight(1f)
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("add_price_package_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Add,
                  contentDescription = stringResource(R.string.btn_add_package),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.btn_add_package))
              }

              OutlinedButton(
                onClick = onResetDefaultPrices,
                modifier =
                  Modifier
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("reset_prices_button"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Restore,
                  contentDescription = stringResource(R.string.btn_reset_prices),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.btn_reset_prices))
              }
            }
          }
        }
      }

      // Editable Diamond Price Items
      items(items = uiState.pricePackages, key = { it.id }) { pkg ->
        PriceTierCard(
          pkg = pkg,
          onEditClick = { editingPackage = pkg },
          onDeleteClick = { onDeletePackage(pkg.id) },
        )
      }

      // Editable Greeting, Payment Numbers & Closing Form
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = MaterialTheme.shapes.large,
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.65f)),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = stringResource(R.string.section_reply_template),
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
              value = greetingInput,
              onValueChange = { greetingInput = it },
              label = { Text(stringResource(R.string.label_greeting)) },
              minLines = 2,
              maxLines = 4,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("greeting_input"),
            )

            OutlinedTextField(
              value = paymentInput,
              onValueChange = { paymentInput = it },
              label = { Text(stringResource(R.string.label_payment_numbers)) },
              placeholder = { Text(stringResource(R.string.hint_payment_numbers)) },
              minLines = 3,
              maxLines = 6,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("payment_numbers_input"),
            )

            OutlinedTextField(
              value = closingInput,
              onValueChange = { closingInput = it },
              label = { Text(stringResource(R.string.label_closing)) },
              singleLine = true,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("closing_input"),
            )

            Button(
              onClick = { onSaveTemplate(greetingInput, paymentInput, closingInput) },
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(52.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("save_template_button"),
              shape = MaterialTheme.shapes.medium,
            ) {
              Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = stringResource(R.string.btn_save_template),
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.btn_save_template),
                style = MaterialTheme.typography.labelLarge,
              )
            }

            HorizontalDivider(color = palette.silverBorder.copy(alpha = 0.45f))

            Text(
              text = "Live Saved Reply Preview",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
              border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.6f)),
            ) {
              Text(
                text = uiState.formattedReply,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                  Modifier
                    .padding(14.dp)
                    .testTag("live_template_preview_text"),
              )
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(12.dp)) }
    }
  }

  if (isAddingNewPackage) {
    PricePackageDialog(
      title = stringResource(R.string.dialog_add_tier_title),
      initialPrice = "PKR ",
      initialDiamonds = " diamonds",
      onDismiss = { isAddingNewPackage = false },
      onConfirm = { price, diamonds ->
        onSavePackage(0, price, diamonds, uiState.pricePackages.size + 1)
        isAddingNewPackage = false
      },
    )
  }

  editingPackage?.let { current ->
    PricePackageDialog(
      title = stringResource(R.string.dialog_edit_tier_title),
      initialPrice = current.priceText,
      initialDiamonds = current.diamondsText,
      onDismiss = { editingPackage = null },
      onConfirm = { price, diamonds ->
        onSavePackage(current.id, price, diamonds, current.sortOrder)
        editingPackage = null
      },
    )
  }
}

@Composable
private fun PriceTierCard(
  pkg: PricePackageEntity,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val palette = LocalJarvisPalette.current

  Card(
    modifier =
      Modifier
        .fillMaxWidth()
        .testTag("price_tier_card_${pkg.id}"),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, palette.silverBorder.copy(alpha = 0.7f)),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.Diamond,
            contentDescription = "Diamond tier",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp),
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = pkg.priceText,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = pkg.diamondsText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      IconButton(
        onClick = onEditClick,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("edit_price_button_${pkg.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.Edit,
          contentDescription = "Edit ${pkg.priceText}",
          tint = MaterialTheme.colorScheme.secondary,
        )
      }

      IconButton(
        onClick = onDeleteClick,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("delete_price_button_${pkg.id}"),
      ) {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = "Delete ${pkg.priceText}",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun PricePackageDialog(
  title: String,
  initialPrice: String,
  initialDiamonds: String,
  onDismiss: () -> Unit,
  onConfirm: (price: String, diamonds: String) -> Unit,
) {
  var price by rememberSaveable { mutableStateOf(initialPrice) }
  var diamonds by rememberSaveable { mutableStateOf(initialDiamonds) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = title) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = price,
          onValueChange = { price = it },
          label = { Text(stringResource(R.string.label_price_input)) },
          singleLine = true,
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("dialog_price_input"),
        )
        OutlinedTextField(
          value = diamonds,
          onValueChange = { diamonds = it },
          label = { Text(stringResource(R.string.label_diamonds_input)) },
          singleLine = true,
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("dialog_diamonds_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(price, diamonds) },
        enabled = price.isNotBlank() && diamonds.isNotBlank(),
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("dialog_save_price_button"),
      ) {
        Text(stringResource(R.string.btn_save))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier =
          Modifier
            .minimumInteractiveComponentSize()
            .testTag("dialog_cancel_price_button"),
      ) {
        Text(stringResource(R.string.btn_cancel))
      }
    },
  )
}
