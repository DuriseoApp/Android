package com.duriseo.android.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp

// 토큰 견본 Preview. 글자는 토큰 이름을 보여 주는 개발용이라 앱 문구(strings.xml)가 아니다.

private val TokenColors: List<Pair<String, Color>> = listOf(
    "brand-50" to Brand50,
    "brand-100" to Brand100,
    "brand-500" to Brand500,
    "brand-700" to Brand700,
    "on-brand" to OnBrand,
    "person-me" to PersonMe,
    "person-partner" to PersonPartner,
    "person-together" to PersonTogether,
    "ink-900" to Ink900,
    "ink-700" to Ink700,
    "ink-600" to Ink600,
    "ink-500" to Ink500,
    "ink-400" to Ink400,
    "ink-300" to Ink300,
    "line-300" to Line300,
    "line-200" to Line200,
    "line-100" to Line100,
    "surface-0" to Surface0,
    "surface-50" to Surface50,
    "surface-100" to Surface100,
    "scrim" to Scrim,
    "danger-600" to Danger600,
    "danger-700" to Danger700,
    "warning-50" to Warning50,
    "warning-700" to Warning700,
    "warning-800" to Warning800,
    "new-600" to New600,
    "info-700" to Info700,
    "favorite-600" to Favorite600,
)

/** 견본 한 칸의 크기. 앱바 높이(64) 토큰 하나로 모든 견본(색, 모서리, 그림자)을 같은 크기로 그린다. */
private val SampleSize = DuriseoSize.Appbar

@Composable
private fun SwatchGrid(swatches: List<Pair<String, Color>>, columns: Int = 4) {
    Column(
        modifier = Modifier
            .background(Surface0)
            .padding(DuriseoSpacing.Space4),
        verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2),
    ) {
        swatches.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space2)) {
                rowItems.forEach { (name, color) ->
                    Column(
                        modifier = Modifier.width(SampleSize),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(SampleSize)
                                .background(color, MaterialTheme.shapes.small)
                                .border(Dp.Hairline, Line200, MaterialTheme.shapes.small),
                        )
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink900,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 520)
@Composable
private fun ColorTokensPreview() {
    DuriseoTheme {
        SwatchGrid(TokenColors, columns = 6)
    }
}

@Preview(showBackground = true, widthDp = 520)
@Composable
private fun PickPalettePreview() {
    DuriseoTheme {
        SwatchGrid(
            swatches = DuriseoPickPalette.colors.map { stringResource(it.labelRes) to it.color },
            columns = 6,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TypographyTokensPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier
                .background(Surface0)
                .padding(DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space1),
        ) {
            // 이름 = M3 스타일 이름 (tokens 이름)
            Text("displayLarge (display) 40/46", style = MaterialTheme.typography.displayLarge, color = Ink900)
            Text("headlineMedium (headline) 26/35", style = MaterialTheme.typography.headlineMedium, color = Ink900)
            Text("titleLarge (title-lg) 22/30", style = MaterialTheme.typography.titleLarge, color = Ink900)
            Text("titleMedium (title-md) 17/24", style = MaterialTheme.typography.titleMedium, color = Ink900)
            Text("titleSmall (title-sm) 15/22", style = MaterialTheme.typography.titleSmall, color = Ink900)
            Text("bodyLarge (body-lg) 16/24", style = MaterialTheme.typography.bodyLarge, color = Ink900)
            Text("bodyMedium (body-md) 15/24", style = MaterialTheme.typography.bodyMedium, color = Ink900)
            Text("bodySmall (body-sm) 14/22", style = MaterialTheme.typography.bodySmall, color = Ink900)
            Text("labelLarge (label) 13/18", style = MaterialTheme.typography.labelLarge, color = Ink900)
            Text("labelMedium (caption) 12/16", style = MaterialTheme.typography.labelMedium, color = Ink900)
            Text("labelSmall (micro) 11/14", style = MaterialTheme.typography.labelSmall, color = Ink900)
            Text("CodeLg 26/32 mono", style = DuriseoTextStyles.CodeLg, color = Ink900)
            Text("CodeMd 24/30 mono", style = DuriseoTextStyles.CodeMd, color = Ink900)
        }
    }
}

@Composable
private fun ShapeSample(name: String, shape: Shape) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(SampleSize)
                .background(Brand100, shape),
        )
        Text(text = name, style = MaterialTheme.typography.labelSmall, color = Ink900)
    }
}

@Preview(showBackground = true, widthDp = 520)
@Composable
private fun ShapeTokensPreview() {
    DuriseoTheme {
        Column(
            modifier = Modifier
                .background(Surface0)
                .padding(DuriseoSpacing.Space4),
            verticalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
                ShapeSample("small 12", MaterialTheme.shapes.small)
                ShapeSample("medium 14", MaterialTheme.shapes.medium)
                ShapeSample("large 16", MaterialTheme.shapes.large)
                ShapeSample("extraLarge 24", MaterialTheme.shapes.extraLarge)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space3)) {
                ShapeSample("xs 6", DuriseoShapes.Xs)
                ShapeSample("fab 18", DuriseoShapes.Fab)
                ShapeSample("xl 20", DuriseoShapes.Xl)
                ShapeSample("full", DuriseoShapes.Full)
            }
        }
    }
}

@Composable
private fun ShadowSample(name: String, shadow: DuriseoShadow) {
    val shape = MaterialTheme.shapes.large
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(SampleSize)
                .duriseoShadow(shape, shadow)
                .background(Surface0, shape),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Ink900,
            modifier = Modifier.padding(top = DuriseoSpacing.Space2),
        )
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun ShadowTokensPreview() {
    DuriseoTheme {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DuriseoSize.Appbar * 3)
                .background(Surface50)
                .padding(DuriseoSpacing.Space6),
            horizontalArrangement = Arrangement.spacedBy(DuriseoSpacing.Space6),
        ) {
            ShadowSample("shadow-sm", DuriseoShadows.Sm)
            ShadowSample("shadow-fab", DuriseoShadows.Fab)
            ShadowSample("shadow-dialog", DuriseoShadows.Dialog)
        }
    }
}
