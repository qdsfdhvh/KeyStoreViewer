package ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun LargeTitle(
  title: String,
  modifier: Modifier = Modifier,
  actions: @Composable RowScope.() -> Unit = {},
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Column(Modifier.weight(1f)) {
      Text(
        "KeyStoreViewer",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        title,
        style = MaterialTheme.typography.headlineLarge,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
      )
    }
    actions()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
  title: String,
  onBack: () -> Unit,
  actions: @Composable RowScope.() -> Unit = {},
) {
  CenterAlignedTopAppBar(
    title = { Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back")
      }
    },
    actions = actions,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background,
      navigationIconContentColor = MaterialTheme.colorScheme.primary,
      actionIconContentColor = MaterialTheme.colorScheme.primary,
    ),
  )
}

@Composable
fun GroupedCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    content = content,
  )
}

@Composable
fun PrimaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable RowScope.() -> Unit,
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.heightIn(min = 48.dp),
    shape = MaterialTheme.shapes.medium,
    content = content,
  )
}

@Composable
fun SecondaryButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  content: @Composable RowScope.() -> Unit,
) {
  TextButton(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.heightIn(min = 48.dp),
    shape = MaterialTheme.shapes.medium,
    colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surface),
    content = content,
  )
}

/** Individual lazy rows form one rounded group without eagerly composing the whole list. */
fun groupedRowShape(first: Boolean, last: Boolean): Shape = RoundedCornerShape(
  topStart = if (first) 12.dp else 0.dp,
  topEnd = if (first) 12.dp else 0.dp,
  bottomStart = if (last) 12.dp else 0.dp,
  bottomEnd = if (last) 12.dp else 0.dp,
)
