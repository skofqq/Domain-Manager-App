package com.skofqq.domainmanager.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import kotlin.math.abs
import kotlin.math.hypot

/*
 * Pixel-style ("Wi-Fi share / Quick Share") QR rendering: an organic blob plate,
 * concentric-circle finder patterns and per-module shape variation, drawn by hand
 * on a Compose Canvas over the RAW ZXing module matrix. This is purely visual —
 * scannability rules are non-negotiable:
 *  - ECC level H (~30% recovery) pays for the decorative liberties;
 *  - finder/alignment rings keep the spec 1:1:3:1:1 radial proportions;
 *  - modules keep >= MODULE_FILL of their cell; ink/plate colors are taken from
 *    the theme but only accepted with a real luminance gap (see [qrPalette]),
 *    and the code is never rendered inverted (light-on-dark);
 *  - at animation rest EVERY element sits at exactly nominal size — baking
 *    spring overshoot into the resting geometry inflates the finder patterns
 *    and breaks scanners' module-size estimate (found the hard way);
 *  - a full 4-module quiet zone lives inside the light plate.
 *
 * Long-press fallback: [plain] renders the same matrix as a classic
 * black-on-white square QR for scanners that reject the styled look.
 */

/** Hard fallback pair (also the plain mode palette): near-black on white. */
private val QrInkFallback = Color(0xFF1D1D1F)
private val QrPlateFallback = Color.White

/** Fraction of the module cell a mark fills — decorative gaps stay above the readability floor. */
private const val MODULE_FILL = 0.92f

/** Quiet zone (in modules) kept inside the plate on every side, per QR spec. */
private const val QUIET_MODULES = 4f

// Master-progress segmentation (see the reference GIF timing: plate ~250 ms →
// staggered modules ~800 ms → finder cores last ~250 ms).
private const val PHASE_PLATE_END = 0.25f
private const val PHASE_MARKS_END = 0.85f

/** One dark data/format/timing module with its precomputed style and stagger key. */
internal class QrMark(
    val x: Int,
    val y: Int,
    /**
     * Reveal order key, 0..1: mostly center-out distance with a deterministic
     * per-module jitter — the assembly spreads outwards but with a ragged,
     * organic edge instead of a strict circular wavefront (as in the reference).
     */
    val stagger: Float,
    val circle: Boolean,
    /** Corner radius as a fraction of the mark size (ignored for circles). */
    val cornerFrac: Float,
)

/** Center (in continuous module units) + stagger key of a finder/alignment pattern. */
internal class QrRing(val cx: Float, val cy: Float, val dist: Float)

internal class StyledQrData(
    val size: Int,
    /** Full raw matrix (row-major), for the plain fallback rendering. */
    val grid: Array<BooleanArray>,
    val marks: List<QrMark>,
    val finders: List<QrRing>,
    val alignments: List<QrRing>,
)

/**
 * Encodes [content] at ECC H and splits the raw matrix into styled parts:
 * plain dark modules (with a shape picked DETERMINISTICALLY from the module's
 * coordinates — same input, same visual on every recomposition) and the
 * finder/alignment patterns that are drawn as concentric circles instead.
 */
internal fun styledQrData(content: String): StyledQrData {
    val code = Encoder.encode(content, ErrorCorrectionLevel.H)
    val matrix = code.matrix
    val size = matrix.width
    val center = (size - 1) / 2f
    val maxDist = hypot(center, center)
    fun dist(x: Float, y: Float) = (hypot(x - center, y - center) / maxDist).coerceIn(0f, 1f)

    val grid = Array(size) { y -> BooleanArray(size) { x -> matrix.get(x, y).toInt() == 1 } }

    // Alignment patterns exist from version 2 up; the three that would overlap
    // the finder corners are omitted by the QR spec itself.
    val centers = code.version.alignmentPatternCenters.toList()
    val alignments = buildList {
        for (cx in centers) for (cy in centers) {
            val onFinder = (cx <= 8 && cy <= 8) || (cx >= size - 9 && cy <= 8) || (cx <= 8 && cy >= size - 9)
            if (!onFinder) add(QrRing(cx + 0.5f, cy + 0.5f, dist(cx.toFloat(), cy.toFloat())))
        }
    }

    fun inFinderZone(x: Int, y: Int) =
        (x < 8 && y < 8) || (x >= size - 8 && y < 8) || (x < 8 && y >= size - 8)

    fun inAlignmentZone(x: Int, y: Int) =
        alignments.any { abs(x + 0.5f - it.cx) <= 2.5f && abs(y + 0.5f - it.cy) <= 2.5f }

    val marks = buildList {
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (!grid[y][x]) continue
                if (inFinderZone(x, y) || inAlignmentZone(x, y)) continue
                // Deterministic per-coordinate hash → stable shape/stagger choice.
                val h = (x * 73856093) xor (y * 19349663)
                val timing = x == 6 || y == 6
                val jitter = ((h ushr 16) and 0xFF) / 255f
                add(
                    QrMark(
                        x = x,
                        y = y,
                        stagger = 0.65f * dist(x.toFloat(), y.toFloat()) + 0.35f * jitter,
                        // Timing rows read best as a clean dotted line.
                        circle = timing || (h ushr 3) % 4 == 0,
                        cornerFrac = 0.25f + ((h ushr 8) % 3) * 0.125f,
                    )
                )
            }
        }
    }

    val f = 3.5f
    val finders = listOf(
        QrRing(f, f, dist(3f, 3f)),
        QrRing(size - f, f, dist(size - 4f, 3f)),
        QrRing(f, size - f, dist(3f, size - 4f)),
    )
    return StyledQrData(size, grid, marks, finders, alignments)
}

/**
 * Theme-derived plate/ink pair, ACCEPTED only with a real luminance gap. The
 * plate must stay light and the ink dark in every theme — an inverted QR is
 * rejected by many camera scanners, so in dark themes the inverse-surface
 * tokens (which are light there) are used instead of surface, and if even those
 * fail the gap check (exotic dynamic palettes) the hard black-on-white pair wins
 * over looking pretty.
 */
@Composable
private fun qrPalette(): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    fun scannable(plate: Color, ink: Color) =
        plate.luminance() >= 0.55f && plate.luminance() - ink.luminance() >= 0.45f

    val lightPair = cs.surfaceContainerLow to cs.primary
    if (scannable(lightPair.first, lightPair.second)) return lightPair
    val inversePair = cs.inverseSurface to cs.inverseOnSurface
    if (scannable(inversePair.first, inversePair.second)) return inversePair
    return QrPlateFallback to QrInkFallback
}

/**
 * Animated, system-style QR code. Choreography follows the reference recording:
 * one master value runs through three expressive-spring segments — the blob
 * plate settles first, then the modules assemble center-out with per-module
 * jittered stagger, and the finder/alignment center dots land last with a
 * transient spring pop. [plain] instead renders a static, classic black-on-white
 * square QR of the same payload (long-press fallback for strict scanners).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StyledQrCode(content: String, modifier: Modifier = Modifier, plain: Boolean = false) {
    val data = remember(content) { styledQrData(content) }
    val (plateColor, inkColor) = qrPalette()

    if (plain) {
        Canvas(modifier = modifier) { drawPlainQr(data) }
        return
    }

    val progress = remember(content) { Animatable(0f) }
    val motion = MaterialTheme.motionScheme
    val plateSpec = motion.defaultEffectsSpec<Float>()
    val marksSpec = motion.slowSpatialSpec<Float>()
    val coresSpec = motion.defaultSpatialSpec<Float>()
    LaunchedEffect(content) {
        progress.snapTo(0f)
        // Three chained springs ≈ the reference's ~1.2–1.4 s total: plate,
        // module assembly, finder cores. Springs, not stretched tweens.
        progress.animateTo(PHASE_PLATE_END, plateSpec)
        progress.animateTo(PHASE_MARKS_END, marksSpec)
        progress.animateTo(1f, coresSpec)
    }

    // Deterministic per-content corner radii make the blob organic but stable.
    val blobCorners = remember(content) {
        val h = content.hashCode()
        List(4) { i -> 0.26f + ((h ushr (i * 5)) and 0xF) / 15f * 0.16f }
    }

    Canvas(modifier = modifier) {
        val p = progress.value
        val geo = qrGeometry(data.size)

        // --- Blob plate: settles first so marks always land on the light plate --
        val plateReveal = (p / PHASE_PLATE_END).coerceIn(0f, 1f)
        val plate = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset(geo.plateLeft, geo.plateTop), Size(geo.side, geo.side)),
                    topLeft = CornerRadius(geo.side * blobCorners[0]),
                    topRight = CornerRadius(geo.side * blobCorners[1]),
                    bottomRight = CornerRadius(geo.side * blobCorners[2]),
                    bottomLeft = CornerRadius(geo.side * blobCorners[3]),
                )
            )
        }
        scale(0.92f + 0.08f * plateReveal) {
            drawPath(plate, plateColor.copy(alpha = plateColor.alpha * plateReveal))
        }
        if (p <= PHASE_PLATE_END * 0.6f) return@Canvas

        // Local reveals CLAMP AT EXACTLY 1: springiness comes only from the
        // master spring transiently overshooting a segment target — at rest
        // every element sits at nominal size (scanner-critical, see header).
        fun markReveal(stagger: Float) =
            ((p - PHASE_PLATE_END - stagger * 0.5f) / 0.10f).coerceIn(0f, 1f)

        fun ringReveal(dist: Float) =
            ((p - 0.35f - dist * 0.30f) / 0.15f).coerceIn(0f, 1f)

        val coreReveal = ((p - PHASE_MARKS_END) / (1f - PHASE_MARKS_END)).coerceIn(0f, 1f) +
            (p - 1f).coerceIn(0f, 0.1f)

        // --- Plain modules ------------------------------------------------------
        data.marks.forEach { mark ->
            val r = markReveal(mark.stagger)
            if (r <= 0f) return@forEach
            val markSize = geo.module * MODULE_FILL * r
            val cx = geo.originX + (mark.x + 0.5f) * geo.module
            val cy = geo.originY + (mark.y + 0.5f) * geo.module
            if (mark.circle) {
                drawCircle(inkColor, radius = markSize / 2f, center = Offset(cx, cy))
            } else {
                drawRoundRect(
                    color = inkColor,
                    topLeft = Offset(cx - markSize / 2f, cy - markSize / 2f),
                    size = Size(markSize, markSize),
                    cornerRadius = CornerRadius(markSize * mark.cornerFrac),
                )
            }
        }

        // --- Finder patterns as concentric circles ------------------------------
        // Radial section keeps the spec 1:1:3:1:1 ratio: ink ring 2.5..3.5
        // modules, light gap 1.5..2.5, solid core radius 1.5. Rings assemble
        // with the modules; the core dots pop in last (reference behavior).
        data.finders.forEach { ring ->
            val r = ringReveal(ring.dist)
            if (r <= 0f) return@forEach
            val c = Offset(geo.originX + ring.cx * geo.module, geo.originY + ring.cy * geo.module)
            drawCircle(inkColor, radius = 3f * geo.module * r, center = c, style = Stroke(width = geo.module * r))
            if (coreReveal > 0f) {
                drawCircle(inkColor, radius = 1.5f * geo.module * coreReveal, center = c)
            }
        }

        // --- Alignment patterns, same treatment (ring 1.5..2.5 + center dot) ----
        data.alignments.forEach { ring ->
            val r = ringReveal(ring.dist)
            if (r <= 0f) return@forEach
            val c = Offset(
                geo.originX + ring.cx * geo.module,
                geo.originY + ring.cy * geo.module,
            )
            drawCircle(inkColor, radius = 2f * geo.module * r, center = c, style = Stroke(width = geo.module * r))
            if (coreReveal > 0f) {
                drawCircle(inkColor, radius = 0.55f * geo.module * coreReveal, center = c)
            }
        }
    }
}

private class QrGeometry(
    val module: Float,
    val side: Float,
    val plateLeft: Float,
    val plateTop: Float,
    val originX: Float,
    val originY: Float,
)

private fun DrawScope.qrGeometry(matrixSize: Int): QrGeometry {
    val totalModules = matrixSize + QUIET_MODULES * 2
    val module = minOf(size.width, size.height) / totalModules
    val side = module * totalModules
    val plateLeft = (size.width - side) / 2f
    val plateTop = (size.height - side) / 2f
    return QrGeometry(
        module = module,
        side = side,
        plateLeft = plateLeft,
        plateTop = plateTop,
        originX = plateLeft + QUIET_MODULES * module,
        originY = plateTop + QUIET_MODULES * module,
    )
}

/**
 * The guaranteed-compatible fallback: classic square modules and square finder
 * patterns, pure black on a white plate, no animation, no theme tinting.
 */
private fun DrawScope.drawPlainQr(data: StyledQrData) {
    val geo = qrGeometry(data.size)
    drawRoundRect(
        color = QrPlateFallback,
        topLeft = Offset(geo.plateLeft, geo.plateTop),
        size = Size(geo.side, geo.side),
        cornerRadius = CornerRadius(geo.module * 2f),
    )
    for (y in 0 until data.size) {
        for (x in 0 until data.size) {
            if (!data.grid[y][x]) continue
            // +0.5px bleed keeps adjacent modules seamless despite float rounding.
            drawRect(
                color = QrInkFallback,
                topLeft = Offset(geo.originX + x * geo.module, geo.originY + y * geo.module),
                size = Size(geo.module + 0.5f, geo.module + 0.5f),
            )
        }
    }
}
