import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Composable
fun rememberApkDocument(): ImageVector = remember {
  ImageVector.Builder(
    name = "IcApkDocument",
    defaultWidth = 40.dp,
    defaultHeight = 40.dp,
    viewportWidth = 960f,
    viewportHeight = 960f
  ).apply {
    path(
      fill = SolidColor(Color.Black),
      fillAlpha = 1.0f,
      stroke = null,
      strokeAlpha = 1.0f,
      strokeLineWidth = 1.0f,
      strokeLineCap = StrokeCap.Butt,
      strokeLineJoin = StrokeJoin.Miter,
      strokeLineMiter = 1.0f,
      pathFillType = PathFillType.NonZero
    ) {
      moveTo(280f, 760f)
      horizontalLineToRelative(400f)
      quadToRelative(-4f, -49f, -30f, -90f)
      reflectiveQuadToRelative(-68f, -65f)
      lineToRelative(38f, -68f)
      quadToRelative(2f, -4f, 1f, -9f)
      reflectiveQuadToRelative(-6f, -7f)
      quadToRelative(-4f, -2f, -8.5f, -1f)
      reflectiveQuadToRelative(-6.5f, 5f)
      lineToRelative(-39f, 70f)
      quadToRelative(-20f, -8f, -40f, -12.5f)
      reflectiveQuadToRelative(-41f, -4.5f)
      reflectiveQuadToRelative(-41f, 4.5f)
      reflectiveQuadToRelative(-40f, 12.5f)
      lineToRelative(-39f, -70f)
      quadToRelative(-2f, -5f, -6.5f, -5f)
      reflectiveQuadToRelative(-9.5f, 2f)
      lineToRelative(-4f, 15f)
      lineToRelative(38f, 68f)
      quadToRelative(-42f, 24f, -68f, 65f)
      reflectiveQuadToRelative(-30f, 90f)
      moveToRelative(110f, -60f)
      quadToRelative(-8f, 0f, -14f, -6f)
      reflectiveQuadToRelative(-6f, -14f)
      reflectiveQuadToRelative(6f, -14f)
      reflectiveQuadToRelative(14f, -6f)
      reflectiveQuadToRelative(14f, 6f)
      reflectiveQuadToRelative(6f, 14f)
      reflectiveQuadToRelative(-6f, 14f)
      reflectiveQuadToRelative(-14f, 6f)
      moveToRelative(180f, 0f)
      quadToRelative(-8f, 0f, -14f, -6f)
      reflectiveQuadToRelative(-6f, -14f)
      reflectiveQuadToRelative(6f, -14f)
      reflectiveQuadToRelative(14f, -6f)
      reflectiveQuadToRelative(14f, 6f)
      reflectiveQuadToRelative(6f, 14f)
      reflectiveQuadToRelative(-6f, 14f)
      reflectiveQuadToRelative(-14f, 6f)
      moveTo(240f, 880f)
      quadToRelative(-33f, 0f, -56.5f, -23.5f)
      reflectiveQuadTo(160f, 800f)
      verticalLineToRelative(-640f)
      quadToRelative(0f, -33f, 23.5f, -56.5f)
      reflectiveQuadTo(240f, 80f)
      horizontalLineToRelative(320f)
      lineToRelative(240f, 240f)
      verticalLineToRelative(480f)
      quadToRelative(0f, 33f, -23.5f, 56.5f)
      reflectiveQuadTo(720f, 880f)
      close()
      moveToRelative(280f, -520f)
      verticalLineToRelative(-200f)
      horizontalLineTo(240f)
      verticalLineToRelative(640f)
      horizontalLineToRelative(480f)
      verticalLineToRelative(-440f)
      close()
      moveTo(240f, 160f)
      verticalLineToRelative(200f)
      close()
      verticalLineToRelative(640f)
      close()
    }
  }.build()
}
