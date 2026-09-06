package ui.widget

import androidx.compose.runtime.Composable
import ui.component.preview.ThemePreviews
import ui.theme.AppTheme

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
