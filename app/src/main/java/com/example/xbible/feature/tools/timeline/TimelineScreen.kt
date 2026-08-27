package com.example.xbible.feature.tools.timeline

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xbible.R
import com.example.xbible.viewmodel.TimelineViewModel
import uniffi.xbible_engine.Event
import uniffi.xbible_engine.Period

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onBack: () -> Unit,
    onEventClick: (Event) -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val periods by viewModel.periods.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timeline)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (periods.isEmpty()) {
                    Text(stringResource(R.string.no_modules_found))
                } else {
                    TimelineContent(
                        periods = periods,
                        onEventClick = {
                            selectedEvent = it
                            showDetails = true
                        }
                    )
                }
            }

            // Right Sidebar for Tablet
            if (isTablet && showDetails && selectedEvent != null) {
                Surface(
                    modifier = Modifier
                        .width(360.dp)
                        .fillMaxHeight()
                        .animateContentSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            IconButton(
                                onClick = { showDetails = false },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Close")
                            }
                        }
                        EventDetailContent(selectedEvent!!)
                    }
                }
            }
        }

        // Bottom Sheet for Mobile
        if (!isTablet && showDetails && selectedEvent != null) {
            ModalBottomSheet(
                onDismissRequest = { showDetails = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                EventDetailContent(selectedEvent!!)
            }
        }
    }
}

fun yearLabel(year: Long): String {
    return when {
        year < 0 -> "${-year} BC"
        year == 0L -> "0"
        else -> "$year AD"
    }
}

fun yearRangeLabel(start: Long, end: Long): String {
    return if (start == end) yearLabel(start) else "${yearLabel(start)} – ${yearLabel(end)}"
}

@Composable
fun EventDetailContent(event: Event) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
            .navigationBarsPadding()
    ) {
        if (!event.image.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = event.image,
                    contentDescription = event.title,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = yearRangeLabel(event.start, event.end),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(24.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        Text(
            text = "Scriptural Context",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        Text(
            text = "Explore the scriptural context and historical impact of this event in the study modules.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )

        if (event.slug.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Slug: ${event.slug}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun TimelineContent(
    periods: List<Period>,
    onEventClick: (Event) -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    
    var scale by remember { mutableStateOf(1f) }
    val transformState = rememberTransformableState { zoomChange, _, _ ->
        scale = (scale * zoomChange).coerceIn(0.1f, 10f)
    }

    if (periods.isEmpty()) return

    val minYear = periods.minOf { it.startYear }
    val maxYear = periods.maxOf { it.endYear }
    val totalYears = (maxYear - minYear).toInt()
    
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val isTablet = configuration.screenWidthDp >= 600

    // Spacing constants - much denser for mobile overview
    val baseYearWidth = 5.dp 
    val yearWidth = baseYearWidth * scale
    val rowHeight = 52.dp // Slightly taller for labels
    val timelineTopPadding = 60.dp
    val timelineBottomPadding = 80.dp

    val yearWidthPx = with(density) { yearWidth.toPx() }

    // Packing algorithm to avoid overlaps (considering label length)
    val eventRows by remember(periods, scale) {
        derivedStateOf {
            val rows = mutableMapOf<ULong, Int>()
            val rowEnds = mutableListOf<Float>()
            
            // Sort events by start year
            val allEvents = periods.flatMap { period -> 
                period.events.map { it to period.color } 
            }.sortedBy { it.first.start }

            allEvents.forEach { (event, _) ->
                val startX = (event.start - minYear) * yearWidthPx
                val visualWidth = (event.end - event.start) * yearWidthPx
                
                // Estimate label width: approx 7px per char at 10sp
                val estimatedLabelWidth = event.title.length * 6.5f * (density.fontScale)
                val totalWidth = maxOf(visualWidth, estimatedLabelWidth) + 30f // buffer
                val endX = startX + totalWidth

                var assignedRow = -1
                for (i in rowEnds.indices) {
                    if (startX >= rowEnds[i]) {
                        assignedRow = i
                        rowEnds[i] = endX
                        break
                    }
                }

                if (assignedRow == -1) {
                    assignedRow = rowEnds.size
                    rowEnds.add(endX)
                }
                rows[event.id] = assignedRow
            }
            rows
        }
    }

    val maxRow = eventRows.values.maxOfOrNull { it } ?: 0
    val totalContentHeight = timelineTopPadding + (rowHeight * (maxRow + 1)) + timelineBottomPadding
    
    // Scrubber/Year calculation
    val centerYear by remember(periods, scale, horizontalScrollState.value) {
        derivedStateOf {
            if (yearWidthPx <= 0f) minYear.toFloat()
            else minYear + (horizontalScrollState.value / yearWidthPx)
        }
    }

    val progressInSection by remember(horizontalScrollState.value, totalYears, yearWidthPx) {
        derivedStateOf {
            val totalScrollWidth = totalYears * yearWidthPx
            if (totalScrollWidth <= 0) 0f
            else (horizontalScrollState.value / totalScrollWidth).coerceIn(0f, 1f)
        }
    }

    val currentPeriod by remember(periods, centerYear) {
        derivedStateOf {
            periods.find { centerYear.toLong() >= it.startYear && centerYear.toLong() <= it.endYear }
        }
    }

    val prevPeriod by remember(periods, centerYear) {
        derivedStateOf {
            periods.filter { it.startYear < centerYear.toLong() - 2 }.maxByOrNull { it.startYear }
        }
    }

    val nextPeriod by remember(periods, centerYear) {
        derivedStateOf {
            periods.filter { it.startYear > centerYear.toLong() + 2 }.minByOrNull { it.startYear }
        }
    }
    
    val chunkYears = 200 
    val numChunks = ((totalYears + chunkYears - 1) / chunkYears).coerceAtLeast(1)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    var isPeriodExpanded by remember { mutableStateOf(false) }

    // Auto-expand when reaching the start of a period
    LaunchedEffect(currentPeriod?.id) {
        if (currentPeriod != null) {
            // Check if we just entered a new period at its start
            if (centerYear.toLong() == currentPeriod?.startYear) {
                isPeriodExpanded = true
            }
        }
    }

    // Contract on scroll
    LaunchedEffect(horizontalScrollState.isScrollInProgress) {
        if (horizontalScrollState.isScrollInProgress) {
            isPeriodExpanded = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformState)
            .onSizeChanged { viewportSize = it }
    ) {
        // Main Scrollable Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .verticalScroll(verticalScrollState)
                .background(surfaceColor)
        ) {
            Row(modifier = Modifier.height(totalContentHeight)) {
                val startPadding = with(density) { (viewportSize.width / 2f).toDp() }
                Spacer(modifier = Modifier.width(startPadding))

                for (i in 0 until numChunks) {
                    val chunkStartYear = minYear + i * chunkYears
                    val chunkEndYear = minOf(maxYear, chunkStartYear + chunkYears)
                    val currentChunkWidth = yearWidth * (chunkEndYear - chunkStartYear).toInt()
                    
                    Canvas(
                        modifier = Modifier
                            .width(currentChunkWidth)
                            .fillMaxHeight()
                            .pointerInput(periods, chunkStartYear, scale) {
                                detectTapGestures { offset ->
                                    val rowHeightPx = with(density) { rowHeight.toPx() }
                                    val topPaddingPx = with(density) { timelineTopPadding.toPx() }

                                    val clickedYear = chunkStartYear + (offset.x / yearWidthPx)
                                    val clickedRow = ((offset.y - topPaddingPx) / rowHeightPx).toInt()
                                    
                                    periods.forEach { period ->
                                        period.events.forEach { event ->
                                            val assignedRow = eventRows[event.id] ?: event.row.toInt()
                                            if (clickedRow == assignedRow && 
                                                clickedYear.toLong() >= event.start && 
                                                clickedYear.toLong() <= event.end) {
                                                onEventClick(event)
                                            }
                                        }
                                    }
                                }
                            }
                    ) {
                        // 2. Grid & Year Labels (Adaptive Ruler)
                        val interval = when {
                            yearWidthPx > 40f -> 1L
                            yearWidthPx > 15f -> 5L
                            yearWidthPx > 8f -> 10L
                            scale > 2f -> 50L
                            scale > 0.5f -> 100L
                            else -> 500L
                        }
                        var year = if (chunkStartYear % interval == 0L) chunkStartYear else (chunkStartYear / interval + 1) * interval
                        while (year <= chunkEndYear) {
                            val x = (year - chunkStartYear) * yearWidthPx
                            
                            val isMajor = year % (interval * 5) == 0L
                            drawLine(
                                color = onSurfaceColor.copy(alpha = if (isMajor) 0.12f else 0.05f),
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = if (isMajor) 1.dp.toPx() else 0.5.dp.toPx()
                            )
                            
                            // Every ruler line gets a label now. 
                            // Minor ones are "dimmed" (lower alpha) as requested.
                            drawIntoCanvas { canvas ->
                                val yearPaint = android.graphics.Paint().apply {
                                    color = onSurfaceColor.toArgb()
                                    // Bolder/Brighter for major intervals, dimmed for minor ones
                                    alpha = if (isMajor) 180 else 70 
                                    textSize = with(density) { (if (isMajor) 10.sp else 8.sp).toPx() }
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = if (isMajor) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                                }
                                val labelY = if (isTablet) {
                                    with(density) { 45.dp.toPx() }
                                } else {
                                    size.height - with(density) { 30.dp.toPx() }
                                }
                                
                                // Draw the label if there's enough room to avoid a mess, 
                                // but always prioritize major markers.
                                if (yearWidthPx * interval > 28f || isMajor) {
                                    canvas.nativeCanvas.drawText(yearLabel(year), x, labelY, yearPaint)
                                }
                            }
                            year += interval
                        }

                        // 3. Events (Dynamic "Covers")
                        periods.forEach { period ->
                            val periodColor = try {
                                val colorStr = if (period.color.startsWith("#")) period.color else "#${period.color}"
                                Color(colorStr.toColorInt())
                            } catch (e: Exception) {
                                primaryColor
                            }

                            period.events.forEach { event ->
                                if (event.start <= chunkEndYear && event.end >= chunkStartYear) {
                                    val assignedRow = eventRows[event.id] ?: event.row.toInt()
                                    val startX = (event.start - chunkStartYear) * yearWidthPx
                                    val width = (event.end - event.start) * yearWidthPx
                                    val y = timelineTopPadding.toPx() + (assignedRow * rowHeight.toPx())
                                    
                                    val eventWidth = width.coerceAtLeast(6.dp.toPx())
                                    val eventHeight = rowHeight.toPx() * 0.7f

                                    // Dynamic Surface
                                    drawRoundRect(
                                        color = surfaceColor.copy(alpha = 0.85f),
                                        topLeft = Offset(startX, y),
                                        size = Size(eventWidth, eventHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx())
                                    )
                                    
                                    // Border & Accent
                                    drawRoundRect(
                                        color = periodColor.copy(alpha = 0.3f),
                                        topLeft = Offset(startX, y),
                                        size = Size(eventWidth, eventHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx())
                                    )

                                    drawRect(
                                        color = periodColor,
                                        topLeft = Offset(startX, y),
                                        size = Size(3.dp.toPx(), eventHeight)
                                    )
                                    
                                    // Text rendering
                                    if (startX + eventWidth > 0 && startX < size.width) {
                                        drawIntoCanvas { canvas ->
                                            val titlePaint = android.graphics.Paint().apply {
                                                color = onSurfaceColor.toArgb()
                                                textSize = with(density) { 10.sp.toPx() }
                                                isFakeBoldText = true
                                            }
                                            val datePaint = android.graphics.Paint().apply {
                                                color = periodColor.toArgb()
                                                textSize = with(density) { 8.sp.toPx() }
                                            }
                                            
                                            val paddingPx = with(density) { 6.dp.toPx() }
                                            val textX = startX + paddingPx
                                            
                                            // Always draw text if possible, and ensure it has enough space
                                            // The packing algorithm already accounted for label width
                                            canvas.nativeCanvas.drawText(
                                                event.title,
                                                textX,
                                                y + (eventHeight * 0.8f),
                                                titlePaint
                                            )

                                            if (eventWidth > with(density) { 60.dp.toPx() }) {
                                                canvas.nativeCanvas.drawText(
                                                    yearRangeLabel(event.start, event.end),
                                                    textX,
                                                    y + (eventHeight * 0.35f),
                                                    datePaint
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(startPadding))
            }
        }

        // Year Scrubber & Controls (Parity with CurrentYear React snippet)
        val scrubberColor = currentPeriod?.let {
            try {
                val colorStr = if (it.color.startsWith("#")) it.color else "#${it.color}"
                Color(colorStr.toColorInt())
            } catch (e: Exception) { Color.Red }
        } ?: Color.Red

        val prevColor = prevPeriod?.let {
            try {
                val colorStr = if (it.color.startsWith("#")) it.color else "#${it.color}"
                Color(colorStr.toColorInt())
            } catch (e: Exception) { primaryColor }
        } ?: onSurfaceColor.copy(alpha = 0.38f)

        val nextColor = nextPeriod?.let {
            try {
                val colorStr = if (it.color.startsWith("#")) it.color else "#${it.color}"
                Color(colorStr.toColorInt())
            } catch (e: Exception) { primaryColor }
        } ?: onSurfaceColor.copy(alpha = 0.38f)

        // Center Scrubber Line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .align(Alignment.Center)
                .background(scrubberColor.copy(alpha = 0.4f))
        )

        // Bottom Controls Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            shadowElevation = 8.dp
        ) {
            Box {
                // Progress Bar at the very bottom of the control bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressInSection)
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(scrubberColor)
                )

                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Navigation Left
                    IconButton(
                        onClick = {
                            prevPeriod?.let {
                                val target = (it.startYear - minYear) * yearWidthPx
                                scope.launch { horizontalScrollState.animateScrollTo(target.toInt()) }
                            }
                        },
                        enabled = prevPeriod != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Previous",
                            tint = prevColor,
                            modifier = Modifier.graphicsLayer { rotationZ = 180f }
                        )
                    }

                    // Floating Year Box (Centered)
                    Surface(
                        color = scrubberColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(30.dp).width(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = yearLabel(centerYear.toLong()),
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Navigation Right
                    IconButton(
                        onClick = {
                            nextPeriod?.let {
                                val target = (it.startYear - minYear) * yearWidthPx
                                scope.launch { horizontalScrollState.animateScrollTo(target.toInt()) }
                            }
                        },
                        enabled = nextPeriod != null
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = nextColor
                        )
                    }
                }
            }
        }
        
        // Floating Era Header (Period Capsule / Cover)
        currentPeriod?.let { period ->
            val eraColor = try {
                val colorStr = if (period.color.startsWith("#")) period.color else "#${period.color}"
                Color(colorStr.toColorInt())
            } catch (e: Exception) { primaryColor }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .padding(horizontal = 24.dp)
                    .animateContentSize()
                    .clickable { isPeriodExpanded = !isPeriodExpanded },
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, eraColor.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(if (isPeriodExpanded) 0.dp else 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isPeriodExpanded) {
                        if (period.image.isNotEmpty()) {
                            AsyncImage(
                                model = period.image,
                                contentDescription = period.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = period.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = eraColor
                            )
                            if (period.subTitle.isNotEmpty()) {
                                Text(
                                    text = period.subTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = period.description,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = yearRangeLabel(period.startYear, period.endYear),
                                style = MaterialTheme.typography.labelSmall,
                                color = eraColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(eraColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = period.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
