package app.trainer.ui

import app.trainer.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.trainer.core.ChartMath

/** Line chart drawn with Canvas. Renders the empty grid plus a message when [values] is empty. */
@Composable
fun TrendChart(values: List<Double>, maxValue: Double, description: String, modifier: Modifier = Modifier, height: Dp = 120.dp, emptyMessage: String? = stringResource(R.string.progress_no_results)) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(height).semantics { contentDescription = description }) {
            val inset = 8.dp.toPx()
            val w = size.width - 2 * inset
            val h = size.height - 2 * inset
            for (f in listOf(0f, 0.5f, 1f)) {
                val y = inset + h * (1 - f)
                drawLine(grid, Offset(inset, y), Offset(inset + w, y), strokeWidth = 1.dp.toPx())
            }
            if (values.isEmpty()) return@Canvas
            val xs = ChartMath.xFractions(values.size)
            val ys = ChartMath.yFractions(values, 0.0, maxValue)
            val points = xs.zip(ys).map { (x, y) -> Offset(inset + w * x, inset + h * (1 - y)) }
            val path = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            }
            drawPath(path, line, style = Stroke(width = 3.dp.toPx()))
            points.forEach { drawCircle(line, radius = 4.dp.toPx(), center = it) }
        }
        if (values.isEmpty() && emptyMessage != null) Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
    }
}
