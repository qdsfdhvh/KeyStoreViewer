package ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.component.preview.ThemePreviews
import ui.theme.AppTheme
import ui.widget.icon.rememberIcALowerCase
import ui.widget.icon.rememberIcAUpperCase
import ui.widget.icon.rememberIcColon
import ui.widget.icon.rememberIcContentCopy

@Composable
fun HexText(
  title: String,
  text: String,
  onCopyContentClick: () -> Unit,
  modifier: Modifier = Modifier,
  isShowToggleUpperOrLowCase: Boolean = false,
  isUpperCase: Boolean = false,
  onToggleUpperOrLowCaseClick: () -> Unit = {},
  isShowColonButton: Boolean = false,
  isColonSplit: Boolean = false,
  onToggleColonSplitClick: () -> Unit = {},
) {
  SelectionContainer {
    Surface(
      modifier = modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
      Column(
        modifier = Modifier
          .padding(start = 8.dp, bottom = 8.dp)
          .fillMaxWidth(),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              title,
              style = MaterialTheme.typography.titleSmall,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Spacer(Modifier.weight(1f))

          if (isShowColonButton) {
            SmallIcon(
              onClick = onToggleColonSplitClick,
              imageVector = rememberIcColon(),
              contentDescription = "show colon",
              color = if (isColonSplit) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
            )
          }

          if (isShowToggleUpperOrLowCase) {
            SmallIcon(
              onClick = onToggleUpperOrLowCaseClick,
              imageVector = if (isUpperCase) {
                rememberIcAUpperCase()
              } else {
                rememberIcALowerCase()
              },
              contentDescription = if (isUpperCase) {
                "show lower case"
              } else {
                "show upper case"
              },
              color = if (isUpperCase) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
            )
          }

          SmallIcon(
            onClick = onCopyContentClick,
            imageVector = rememberIcContentCopy(),
            contentDescription = "copy content",
          )
        }
        Text(
          text,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.padding(end = 8.dp),
        )
      }
    }
  }
}

@Composable
private fun SmallIcon(
  onClick: () -> Unit,
  imageVector: ImageVector,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentColor: Color = contentColorFor(color),
) {
  Surface(
    onClick = onClick,
    color = color,
    contentColor = contentColor,
    shape = CircleShape,
    modifier = modifier,
  ) {
    Icon(
      imageVector,
      contentDescription = contentDescription,
      modifier = Modifier
        .padding(6.dp)
        .size(16.dp),
    )
  }
}

@ThemePreviews
@Composable
fun MD5HexTextPreview() {
  AppTheme {
    HexText(
      title = "MD5",
      text = "0e:a3:7f:37:86:c8:27:32:87:60:0b:ee:47:18:6c:32",
      onCopyContentClick = {},
      isShowToggleUpperOrLowCase = true,
      isUpperCase = true,
      isShowColonButton = true,
      isColonSplit = false,
    )
  }
}

@ThemePreviews
@Composable
fun PublicKeyHexTextPreview() {
  AppTheme {
    HexText(
      title = "Public Key (16)",
      text = "cc04a241c80cbabcf4339c41269471be6a6818f283768a9e63d0f636edc910dae028920a2a773d2e96f4055e168f053045070e9c62fa90241e81495bb3e82d80abfc9b294354c34120954e3283868bf14a60ae3ff4cbc1c2984bb08cc344e151703e4d356338a42d0474f4010b66e1133bebb8105762dcc82145e3a953d22aeeac1f973c2e1df64114843f1b37405faad27fe255fa58d15ea2948351546ca68e1947b2fb7bd9ff84e6e50dfe601053a4728036097474732b77d3ab909d087559504bbd72c0c1ad67b4735397dc161df2fd4007590daa70a1cdf93c7f6774d4a095efdb36c8ed56734829e153200d75209679da47834c67f8d7e7948376ba1b96097502a9c85247ae90758ccf136b142470a5fdbd391bc3b2ecf369d23a128260930fabaf0b99fd5371d6559fad3ab5b225223351d810e67f4b578c287d0ef6661ade5c6e8fe37136e2c2d6b0c9bc223ca2cf05133c6d6180fb8d2529ce72aae11a023a2a9240cda969d21efe279f66ac84b3defa60da94635508c13aecbe23393af00fa1b5c17a9c34f0ef00b4d9d5e45bac54284f9e91e16e2064623de943cafec87ce6941c8c5a6b3b78ba6a9930739dfb964d0875af9956f9ed84e7b7a957ec07b16b2be74e43f41f8f80cbc0a4c84ce08cd1bc7eb1ab6eea81b2320facaf78162bd57ca8195b4e8d314585a2068f3b06e911bb7f30a2c2a917f88da080f7",
      onCopyContentClick = {},
      onToggleUpperOrLowCaseClick = {},
    )
  }
}
