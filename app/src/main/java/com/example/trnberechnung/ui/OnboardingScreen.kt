package com.example.trnberechnung.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trnberechnung.R
import com.example.trnberechnung.ui.theme.OnboardingBlue
import com.example.trnberechnung.ui.theme.OnboardingCard
import com.example.trnberechnung.ui.theme.OnboardingInk
import com.example.trnberechnung.ui.theme.OnboardingMuted
import com.example.trnberechnung.ui.theme.OnboardingOrange
import com.example.trnberechnung.ui.theme.OnboardingTeal
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGES = 3
private val OnboardingBackground = Color(0xFFEAFBFA)
private val OnboardingBrandBlue = Color(0xFF28549B)
private val TideNodeNavy = Color(0xFF062E4F)
private val TideNodeCyan = Color(0xFF12C5C7)
private val TideNodeSailWhite = Color(0xFFE5F8F7)
private val IllustrationCardShape = RoundedCornerShape(38.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onCompleted: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES })
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(OnboardingState()) }
    LaunchedEffect(pagerState.currentPage) { state = state.copy(page = pagerState.currentPage) }

    Box(Modifier.fillMaxSize().background(OnboardingBackground).testTag("onboarding_screen")) {
        Column(
            // Keep the content out of status/navigation bars and display cut-outs while the
            // background continues behind them as a calm, light header area.
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OnboardingBrand()
            Spacer(Modifier.height(16.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                OnboardingPage(page, state.disclaimerAccepted) { state = state.copy(disclaimerAccepted = it) }
            }
            PageIndicator(pagerState.currentPage)
            Spacer(Modifier.height(18.dp))
            val accent = pageAccent(pagerState.currentPage)
            Button(
                onClick = {
                    if (pagerState.currentPage == OnboardingState.LAST_PAGE) {
                        if (state.canFinish) onCompleted()
                    } else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                enabled = pagerState.currentPage != OnboardingState.LAST_PAGE || state.canFinish,
                modifier = Modifier.fillMaxWidth().height(60.dp).testTag("onboarding_continue"),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White, disabledContainerColor = accent.copy(alpha = .35f), disabledContentColor = Color.White.copy(alpha = .75f))
            ) {
                Text(stringResource(if (pagerState.currentPage == OnboardingState.LAST_PAGE) R.string.onboarding_start else R.string.onboarding_continue), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Icon(if (pagerState.currentPage == OnboardingState.LAST_PAGE) Icons.Default.Check else Icons.Default.LocationOn, null)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun OnboardingBrand() =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TideNodeMark(Modifier.size(44.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.onboarding_brand),
            color = TideNodeNavy,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 29.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.4).sp,
            ),
        )
    }

@Composable
private fun OnboardingPage(page: Int, disclaimerAccepted: Boolean, onDisclaimerChanged: (Boolean) -> Unit) {
    val (eyebrow, title, body) = when (page) {
        0 -> Triple(R.string.onboarding_navigation_eyebrow, R.string.onboarding_navigation_title, R.string.onboarding_navigation_body)
        1 -> Triple(R.string.onboarding_weather_eyebrow, R.string.onboarding_weather_title, R.string.onboarding_weather_body)
        else -> Triple(R.string.onboarding_crew_eyebrow, R.string.onboarding_crew_title, R.string.onboarding_crew_body)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(14.dp)); IllustrationCard(page); Spacer(Modifier.height(20.dp))
        Text(stringResource(eyebrow), color = pageAccent(page), style = MaterialTheme.typography.labelLarge, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(title), color = OnboardingInk, fontSize = 31.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(body), color = OnboardingMuted, fontSize = 18.sp, lineHeight = 25.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 560.dp))
        if (page == OnboardingState.LAST_PAGE) { Spacer(Modifier.height(20.dp)); Disclaimer(disclaimerAccepted, onDisclaimerChanged) }
    }
}

@Composable
private fun IllustrationCard(page: Int) =
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (page == OnboardingState.LAST_PAGE) 310.dp else 340.dp)
            .padding(horizontal = 10.dp)
            .shadow(
                elevation = 8.dp,
                shape = IllustrationCardShape,
                clip = false,
                ambientColor = TideNodeNavy.copy(alpha = .06f),
                spotColor = TideNodeNavy.copy(alpha = .10f),
            )
            .clip(IllustrationCardShape)
            .background(OnboardingCard)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (page) {
            0 -> RouteIllustration()
            1 -> WeatherIllustration()
            else -> CrewIllustration()
        }
    }

@Composable private fun RouteIllustration() {
    val transition = rememberInfiniteTransition(label = "route")
    val boatOffset by transition.animateFloat(0f, 8f, infiniteRepeatable(tween(1700), RepeatMode.Reverse), label = "boat")
    Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(30.dp))) {
        val scale = size.minDimension
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFF75C9E6), Color(0xFF287FA9))),
            cornerRadius = CornerRadius(30f, 30f),
        )
        repeat(3) { index ->
            val top = size.height * (.47f + index * .15f)
            val waveDepth = scale * (.030f + index * .003f)
            val path = Path().apply {
                moveTo(0f, top)
                cubicTo(size.width * .22f, top - waveDepth, size.width * .44f, top + waveDepth, size.width * .64f, top + waveDepth * .14f)
                cubicTo(size.width * .82f, top - waveDepth * .74f, size.width, top + waveDepth * .54f, size.width, top + waveDepth * .54f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            val color = when (index) {
                0 -> Color(0xFF63B5D2).copy(alpha = .42f)
                1 -> Color(0xFF469FC2).copy(alpha = .48f)
                else -> Color(0xFF347FA5).copy(alpha = .42f)
            }
            drawPath(path, color)
        }

        val start = Offset(size.width * .14f, size.height * .70f)
        val boat = Offset(size.width * .83f, size.height * .33f + boatOffset)
        val markerRadius = scale * .043f
        val completeRoute = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(size.width * .42f, size.height * .80f, size.width * .57f, size.height * .30f, boat.x, boat.y)
        }
        val routeMeasure = androidx.compose.ui.graphics.PathMeasure().apply {
            setPath(completeRoute, forceClosed = false)
        }
        val visibleRoute = Path()
        val routeStartDistance = markerRadius + 6.dp.toPx()
        val routeEndDistance = routeMeasure.length - scale * .043f - 6.dp.toPx()
        routeMeasure.getSegment(
            startDistance = routeStartDistance,
            stopDistance = routeEndDistance.coerceAtLeast(routeStartDistance),
            destination = visibleRoute,
            startWithMoveTo = true,
        )
        drawPath(
            path = visibleRoute,
            color = Color.White.copy(alpha = .88f),
            style = Stroke(
                width = scale * .0135f,
                cap = StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(scale * .034f, scale * .024f),
                ),
            ),
        )

        drawCircle(Color.White, radius = markerRadius, center = start)
        val pinHeight = markerRadius * 1.24f
        val pinWidth = pinHeight * .68f
        val pinHalfWidth = pinWidth * .5f
        val pinHalfHeight = pinHeight * .5f
        val pin = Path().apply {
            moveTo(start.x, start.y + pinHalfHeight)
            cubicTo(
                start.x - pinWidth * .08f,
                start.y + pinHeight * .34f,
                start.x - pinHalfWidth,
                start.y + pinHeight * .13f,
                start.x - pinHalfWidth,
                start.y - pinHeight * .08f,
            )
            cubicTo(
                start.x - pinHalfWidth,
                start.y - pinHeight * .32f,
                start.x - pinWidth * .27f,
                start.y - pinHalfHeight,
                start.x,
                start.y - pinHalfHeight,
            )
            cubicTo(
                start.x + pinWidth * .27f,
                start.y - pinHalfHeight,
                start.x + pinHalfWidth,
                start.y - pinHeight * .32f,
                start.x + pinHalfWidth,
                start.y - pinHeight * .08f,
            )
            cubicTo(
                start.x + pinHalfWidth,
                start.y + pinHeight * .13f,
                start.x + pinWidth * .08f,
                start.y + pinHeight * .34f,
                start.x,
                start.y + pinHalfHeight,
            )
            close()
        }
        drawPath(pin, OnboardingBrandBlue)
        drawCircle(
            color = Color.White,
            radius = pinWidth * .13f,
            center = Offset(start.x, start.y - pinHeight * .13f),
        )

        val x = boat.x
        val y = boat.y
        drawPath(
            Path().apply {
                moveTo(x + scale * .002f, y + scale * .023f)
                lineTo(x + scale * .002f, y - scale * .075f)
                cubicTo(x + scale * .023f, y - scale * .062f, x + scale * .042f, y - scale * .021f, x + scale * .047f, y + scale * .022f)
                cubicTo(x + scale * .030f, y + scale * .019f, x + scale * .016f, y + scale * .020f, x + scale * .002f, y + scale * .023f)
                close()
            },
            Color.White,
        )
        drawPath(
            Path().apply {
                moveTo(x - scale * .005f, y + scale * .021f)
                lineTo(x - scale * .005f, y - scale * .052f)
                cubicTo(x - scale * .022f, y - scale * .042f, x - scale * .037f, y - scale * .012f, x - scale * .043f, y + scale * .021f)
                close()
            },
            Color(0xFFE8F8FF),
        )
        drawPath(
            Path().apply {
                moveTo(x - scale * .043f, y + scale * .029f)
                quadraticTo(x, y + scale * .036f, x + scale * .047f, y + scale * .027f)
                lineTo(x + scale * .022f, y + scale * .051f)
                quadraticTo(x - scale * .002f, y + scale * .055f, x - scale * .029f, y + scale * .048f)
                close()
            },
            Color.White,
        )
    }
}

@Composable
private fun WeatherIllustration() =
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2398D7), Color(0xFF12396C)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = 22.dp, top = 22.dp, end = 22.dp, bottom = 4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        stringResource(R.string.onboarding_weather_location),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        stringResource(R.string.onboarding_weather_now),
                        color = Color.White.copy(alpha = .72f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text("☀", color = Color(0xFFFFC400), fontSize = 46.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    stringResource(R.string.onboarding_weather_temperature),
                    color = Color.White,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        stringResource(R.string.onboarding_weather_sunny),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        stringResource(R.string.onboarding_weather_high_low),
                        color = Color.White.copy(alpha = .7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = .16f))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("Jetzt", "20:00", "21:00", "22:00").forEachIndexed { index, time ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            time,
                            color = Color.White.copy(alpha = .75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (index < 2) "☀" else "☾",
                            color = if (index < 2) Color(0xFFFFC400) else Color.White,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Text(
                            "${21 - index}°",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.onboarding_weather_wind),
                color = Color.White.copy(alpha = .88f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

@Composable
private fun CrewIllustration() =
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth().height(168.dp), contentAlignment = Alignment.BottomCenter) {
            CrewConnectionLine(Modifier.fillMaxSize())
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    CrewAvatar(OnboardingBrandBlue, stringResource(R.string.onboarding_crew_question))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                    CrewAvatar(OnboardingTeal, stringResource(R.string.onboarding_crew_reply))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CrewFeature(Modifier.weight(1f), stringResource(R.string.onboarding_crew_chat)) {
                ChatBubblesIcon()
            }
            CrewFeature(Modifier.weight(1f), stringResource(R.string.onboarding_crew_dates)) {
                Icon(Icons.Default.DateRange, null, tint = OnboardingOrange, modifier = Modifier.size(28.dp))
            }
            CrewFeature(Modifier.weight(1f), stringResource(R.string.onboarding_crew_roles)) {
                Icon(Icons.Default.Person, null, tint = OnboardingTeal, modifier = Modifier.size(28.dp))
            }
        }
    }

/** The light dashed arch visually connects the two crew members, as in the iOS card. */
@Composable
private fun CrewConnectionLine(modifier: Modifier = Modifier) =
    Canvas(modifier) {
        val leftCenterX = size.width * .25f
        val rightCenterX = size.width * .75f
        val span = rightCenterX - leftCenterX
        val endpointY = size.height - 41.dp.toPx()
        val controlY = size.height * .40f
        val connection = Path().apply {
            moveTo(leftCenterX, endpointY)
            cubicTo(
                leftCenterX + span * .28f,
                controlY,
                rightCenterX - span * .28f,
                controlY,
                rightCenterX,
                endpointY,
            )
        }
        drawPath(
            path = connection,
            color = Color(0xFFD4E2F0),
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 5.dp.toPx()),
                ),
            ),
        )
    }

@Composable private fun CrewAvatar(color: Color, message: String) = Column(horizontalAlignment=Alignment.CenterHorizontally) { Text(message, color=Color.White, fontSize=15.sp, fontWeight=FontWeight.Bold, modifier=Modifier.clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal=14.dp, vertical=10.dp)); Spacer(Modifier.height(10.dp)); Box(Modifier.size(82.dp).clip(RoundedCornerShape(41.dp)).background(color), contentAlignment=Alignment.Center) { Icon(Icons.Default.Person, null, tint=Color.White, modifier=Modifier.size(42.dp)) } }

@Composable
private fun ChatBubblesIcon() =
    Canvas(Modifier.size(28.dp)) {
        val backColor = OnboardingBlue.copy(alpha = .55f)
        val radius = CornerRadius(size.minDimension * .13f, size.minDimension * .13f)
        drawRoundRect(
            color = backColor,
            topLeft = Offset(size.width * .05f, size.height * .10f),
            size = Size(size.width * .68f, size.height * .54f),
            cornerRadius = radius,
        )
        drawPath(
            Path().apply {
                moveTo(size.width * .18f, size.height * .60f)
                lineTo(size.width * .20f, size.height * .77f)
                lineTo(size.width * .36f, size.height * .63f)
                close()
            },
            backColor,
        )
        drawRoundRect(
            color = OnboardingBlue,
            topLeft = Offset(size.width * .27f, size.height * .29f),
            size = Size(size.width * .68f, size.height * .54f),
            cornerRadius = radius,
        )
        drawPath(
            Path().apply {
                moveTo(size.width * .70f, size.height * .80f)
                lineTo(size.width * .84f, size.height * .94f)
                lineTo(size.width * .84f, size.height * .80f)
                close()
            },
            OnboardingBlue,
        )
    }

@Composable
private fun CrewFeature(modifier: Modifier, label: String, iconContent: @Composable () -> Unit) =
    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF3F5F9)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        iconContent()
        Spacer(Modifier.height(5.dp))
        Text(label, color = OnboardingInk, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }

@Composable
private fun Disclaimer(accepted: Boolean, onChanged: (Boolean) -> Unit) =
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF5F6FA))
            .border(1.5.dp, OnboardingTeal.copy(alpha = .55f), RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = accepted,
            onCheckedChange = onChanged,
            modifier = Modifier.testTag("onboarding_disclaimer_checkbox"),
            colors = CheckboxDefaults.colors(checkedColor = OnboardingTeal, uncheckedColor = OnboardingTeal),
        )
        Text(
            stringResource(R.string.onboarding_disclaimer),
            color = OnboardingInk,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(top = 11.dp, end = 4.dp),
        )
    }

@Composable private fun PageIndicator(page: Int) = Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) { repeat(ONBOARDING_PAGES) { index -> val width by animateFloatAsState(if(page==index) 30f else 12f, label="indicator"); Box(Modifier.width(width.dp).height(12.dp).clip(RoundedCornerShape(12.dp)).background(if(page==index) pageAccent(page) else Color(0xFFD5DDE2))) } }

@Composable
private fun TideNodeMark(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "brand-waves")
    val wavePhase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200), RepeatMode.Reverse),
        label = "brand-wave-phase"
    )
    Canvas(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(TideNodeNavy)
    ) {
        val width = size.width
        val height = size.height

        // Two broad, asymmetric sails stay legible at launcher and 44 dp header sizes.
        drawPath(
            Path().apply {
                moveTo(width * .50f, height * .23f)
                cubicTo(
                    width * .60f,
                    height * .30f,
                    width * .69f,
                    height * .42f,
                    width * .73f,
                    height * .57f,
                )
                cubicTo(
                    width * .64f,
                    height * .54f,
                    width * .57f,
                    height * .54f,
                    width * .50f,
                    height * .56f,
                )
                close()
            },
            Color.White
        )
        drawPath(
            Path().apply {
                moveTo(width * .46f, height * .33f)
                cubicTo(
                    width * .38f,
                    height * .39f,
                    width * .31f,
                    height * .48f,
                    width * .27f,
                    height * .59f,
                )
                cubicTo(
                    width * .35f,
                    height * .56f,
                    width * .41f,
                    height * .55f,
                    width * .46f,
                    height * .56f,
                )
                close()
            },
            TideNodeSailWhite
        )

        drawPath(
            Path().apply {
                moveTo(width * .26f, height * .61f)
                cubicTo(
                    width * .36f,
                    height * .64f,
                    width * .62f,
                    height * .64f,
                    width * .74f,
                    height * .60f,
                )
                cubicTo(
                    width * .71f,
                    height * .66f,
                    width * .68f,
                    height * .69f,
                    width * .63f,
                    height * .71f,
                )
                cubicTo(
                    width * .51f,
                    height * .74f,
                    width * .39f,
                    height * .72f,
                    width * .31f,
                    height * .69f,
                )
                close()
            },
            Color.White
        )

        // Only the tide band moves, slowly and by well below one dp at 44 dp.
        val waveShift = wavePhase * width * .012f
        drawPath(
            Path().apply {
                moveTo(width * .343f + waveShift, height * .731f)
                cubicTo(
                    width * .407f + waveShift,
                    height * .694f,
                    width * .472f + waveShift,
                    height * .704f,
                    width * .528f + waveShift,
                    height * .731f,
                )
                cubicTo(
                    width * .583f + waveShift,
                    height * .759f,
                    width * .630f + waveShift,
                    height * .759f,
                    width * .657f + waveShift,
                    height * .731f,
                )
                lineTo(width * .657f + waveShift, height * .759f)
                cubicTo(
                    width * .620f + waveShift,
                    height * .778f,
                    width * .574f + waveShift,
                    height * .778f,
                    width * .519f + waveShift,
                    height * .769f,
                )
                cubicTo(
                    width * .463f + waveShift,
                    height * .741f,
                    width * .407f + waveShift,
                    height * .750f,
                    width * .361f + waveShift,
                    height * .769f,
                )
                close()
            },
            TideNodeCyan
        )
    }
}

private fun pageAccent(page: Int): Color = when(page) { 0 -> OnboardingBlue; 1 -> OnboardingOrange; else -> OnboardingTeal }
