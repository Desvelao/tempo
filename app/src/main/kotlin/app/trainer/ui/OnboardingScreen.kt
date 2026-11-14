package app.trainer.ui

import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val top = stringResource(R.string.onboarding_headline_top)
    val accent = stringResource(R.string.onboarding_headline_accent)
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LogoMark()
            Text(
                buildAnnotatedString {
                    append(top + "\n")
                    withStyle(SpanStyle(color = colors.primary)) { append(accent) }
                },
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
                modifier = Modifier.padding(top = 28.dp).semantics { heading() },
            )
            Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Feature(AppIcons.Target, stringResource(R.string.onboarding_skills_title), stringResource(R.string.onboarding_skills_body))
                Feature(AppIcons.Progress, stringResource(R.string.onboarding_adapts_title), stringResource(R.string.onboarding_adapts_body))
                Feature(AppIcons.Lock, stringResource(R.string.onboarding_private_title), stringResource(R.string.onboarding_private_body))
            }
        }
        PrimaryButton(stringResource(R.string.onboarding_get_started), onDone, Modifier.padding(top = 16.dp), height = 68.dp)
    }
}

/** The launcher icon's cadence ring (track, open arc, centre dot), drawn with theme colours. */
@Composable
private fun LogoMark() {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp), color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.size(72.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val radius = 17.dp.toPx()
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            drawCircle(colors.outlineVariant, radius, center, style = Stroke(stroke))
            drawArc(colors.primary, -90f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(colors.secondary, 4.5.dp.toPx(), center)
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
        Surface(
            shape = RoundedCornerShape(14.dp), color = colors.surfaceContainer,
            border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.size(48.dp),
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = colors.primary) } }
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
