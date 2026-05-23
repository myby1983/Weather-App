package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.api.GeocodingResult
import com.example.data.db.UserSession
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val userSession by viewModel.userSession.collectAsState()
    val weatherState by viewModel.weatherUiState.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    var showSignInPrompt by remember { mutableStateOf(false) }
    var showSimulationPanel by remember { mutableStateOf(false) }

    // Immersive UI soft light background with subtle gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFDFBFF), // Elegant soft light violet/white
            Color(0xFFF3F4F9)  // Fluid light gray-blue background anchor
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Main Responsive Layout
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth > 600.dp

            if (userSession == null) {
                // Beautiful Minimalist Login Backdrop Overlay
                GoogleLoginWelcomeScreen(
                    onSignIn = { email, name ->
                        viewModel.handleGoogleSignIn(email, name, "https://api.dicebear.com/7.x/bottts/svg?seed=$email")
                    }
                )
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                titleContentColor = Color(0xFF1A1C1E)
                            ),
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = null,
                                        tint = Color(0xFF0061A4)
                                    )
                                    Text(
                                        text = "KAALAM WEATHER",
                                        color = Color(0xFF1A1C1E),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp,
                                        fontSize = 18.sp
                                    )
                                }
                            },
                            actions = {
                                // Simulation control
                                IconButton(
                                    onClick = { showSimulationPanel = true },
                                    modifier = Modifier.testTag("simulation_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Simulate Severe Alerts",
                                        tint = Color(0xFF0061A4)
                                    )
                                }

                                // User Account Action
                                UserProfileButton(
                                    session = userSession!!,
                                    onSignOut = { viewModel.handleSignOut() }
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        if (isTablet) {
                            // SPLIT SCREEN TABLET RESPONSIVE VIEW
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Left column: Search and Real-Time detailed metrics
                                Column(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .fillMaxHeight()
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    LocationSearchBar(
                                        query = searchQuery,
                                        results = searchResults,
                                        favorites = favorites,
                                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                                        onSelectResult = { res ->
                                            viewModel.loadWeatherFor(res.latitude, res.longitude, res.name, res.country)
                                            viewModel.onSearchQueryChange("")
                                        },
                                        onToggleFavorite = { name, lat, lng, country ->
                                            viewModel.toggleFavorite(name, lat, lng, country, "")
                                        }
                                    )

                                    RealtimeWeatherMetricsCard(
                                        state = weatherState,
                                        onTriggerAlertTest = { viewModel.triggerSevereWeatherNotification("Thunderstorm warning", "Dangerous high-velocity winds and rain simulated.") },
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )
                                }

                                // Right column: Interactive Visualizations & 7-day extended forecasts
                                Column(
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .fillMaxHeight()
                                        .padding(bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    SevereAlertBanner(state = weatherState, onDismiss = { viewModel.dismissLocalSevereAlert() })

                                    InteractiveAnalyticsCard(
                                        state = weatherState,
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )

                                    Forecast7DayListCard(
                                        state = weatherState,
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )
                                }
                            }
                        } else {
                            // PORTRAIT MOBILE DASHBOARD VIEW
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    LocationSearchBar(
                                        query = searchQuery,
                                        results = searchResults,
                                        favorites = favorites,
                                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                                        onSelectResult = { res ->
                                            viewModel.loadWeatherFor(res.latitude, res.longitude, res.name, res.country)
                                            viewModel.onSearchQueryChange("")
                                        },
                                        onToggleFavorite = { name, lat, lng, country ->
                                            viewModel.toggleFavorite(name, lat, lng, country, "")
                                        }
                                    )
                                }

                                item {
                                    SevereAlertBanner(state = weatherState, onDismiss = { viewModel.dismissLocalSevereAlert() })
                                }

                                item {
                                    RealtimeWeatherMetricsCard(
                                        state = weatherState,
                                        onTriggerAlertTest = { viewModel.triggerSevereWeatherNotification("Hurricane Watch", "Very high wind gust risk inside your locality!") },
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )
                                }

                                item {
                                    InteractiveAnalyticsCard(
                                        state = weatherState,
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )
                                }

                                item {
                                    Forecast7DayListCard(
                                        state = weatherState,
                                        onSelectDay = { viewModel.selectActiveDayIndex(it) }
                                    )
                                }
                                
                                item {
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Severe Hazard Manual Simulator Modal Panel
        if (showSimulationPanel) {
            SimulationPanelDialog(
                onDismiss = { showSimulationPanel = false },
                onSimulateAlert = { title, message ->
                    viewModel.triggerSevereWeatherNotification(title, message)
                    showSimulationPanel = false
                }
            )
        }
    }
}

@Composable
fun GoogleLoginWelcomeScreen(
    onSignIn: (String, String) -> Unit
) {
    val sampleAccounts = listOf(
        "Arun Vasan" to "vasanarun@gmail.com",
        "Developer Account" to "dev.nimbus@gmail.com"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 450.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Glowing Weather Icon Simulation
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFD6E3FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudQueue,
                        contentDescription = null,
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "KAALAM Dashboard",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = Color(0xFF1A1C1E),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Unlock high-precision altitude vectors, severe wind indices, 7-day data visualizers, and emergency alert sweeps.",
                    color = Color(0xFF44474E),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign on securely via Google Authentication:",
                    fontSize = 12.sp,
                    color = Color(0xFF76767A),
                    fontWeight = FontWeight.SemiBold
                )

                // Render standard professional Google-styled sign-in layouts
                sampleAccounts.forEach { (name, email) ->
                    Button(
                        onClick = { onSignIn(email, name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3F4F9),
                            contentColor = Color(0xFF1A1C1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(12.dp))
                            .testTag("google_signin_button_${email.replace("@", "_")}"),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Standard Google colors G shape simulator
                            Text(
                                text = "G",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4),
                                fontSize = 18.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "Continue as $name",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileButton(
    session: UserSession,
    onSignOut: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag("user_profile_pill")
                .size(40.dp)
                .border(2.dp, Color(0xFF0061A4), CircleShape)
        ) {
            AsyncImage(
                model = session.photoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .clip(CircleShape)
                    .fillMaxSize()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(8.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = session.displayName,
                            color = Color(0xFF1A1C1E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = session.email,
                            color = Color(0xFF44474E),
                            fontSize = 12.sp
                        )
                    }
                },
                onClick = {},
                enabled = false
            )
            Divider(color = Color(0xFFE1E2EC))
            DropdownMenuItem(
                text = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                onClick = {
                    expanded = false
                    onSignOut()
                },
                modifier = Modifier.testTag("sign_out_menu_item")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchBar(
    query: String,
    results: List<GeocodingResult>,
    favorites: List<com.example.data.db.FavoriteLocation>,
    onQueryChange: (String) -> Unit,
    onSelectResult: (GeocodingResult) -> Unit,
    onToggleFavorite: (String, Double, Double, String?) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("location_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1A1C1E),
                unfocusedTextColor = Color(0xFF1A1C1E),
                focusedBorderColor = Color(0xFF0061A4),
                unfocusedBorderColor = Color(0xFFC4C6D0),
                focusedContainerColor = Color(0xFFF3F4F9),
                unfocusedContainerColor = Color(0xFFF3F4F9)
            ),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Search city (e.g. London, London, Tokyo...)", color = Color(0x9A44474E)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF0061A4)) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear search", tint = Color(0xFF44474E))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
        )

        // Show Instant Fuzzy Search Results Dropdown
        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    results.forEach { res ->
                        val isFav = favorites.any { it.name.equals(res.name, ignoreCase = true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectResult(res)
                                    keyboardController?.hide()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = res.name,
                                    color = Color(0xFF1A1C1E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = listOfNotNull(res.admin1, res.country).joinToString(", "),
                                    color = Color(0xFF44474E),
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(
                                onClick = { onToggleFavorite(res.name, res.latitude, res.longitude, res.country) }
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = "Toggle favorite",
                                    tint = if (isFav) Color(0xFFFFB300) else Color(0x7344474E)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Favorites Row
        if (favorites.isNotEmpty() && results.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favorites.take(3).forEach { fav ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFD6E3FF)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable {
                                onSelectResult(
                                    GeocodingResult(
                                        id = 0,
                                        name = fav.name,
                                        latitude = fav.latitude,
                                        longitude = fav.longitude,
                                        country = fav.country,
                                        admin1 = fav.admin1
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFF001B3E), modifier = Modifier.size(14.dp))
                            Text(text = fav.name, color = Color(0xFF001B3E), fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SevereAlertBanner(
    state: WeatherUiState,
    onDismiss: () -> Unit
) {
    if (state is WeatherUiState.Success && state.isSevereAlertActive) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2B8B5) // Beautiful light pastel warning rose
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF601410).copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFF601410),
                    modifier = Modifier.size(28.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.severeAlertTitle ?: "SEVERE WEATHER WARNING",
                        color = Color(0xFF601410),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.severeAlertMessage ?: "Hazardous values detected. Proceed with safety precautions.",
                        color = Color(0xFF601410).copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color(0xFF601410).copy(alpha = 0.1f), CircleShape).size(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss Alert", tint = Color(0xFF601410), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun RealtimeWeatherMetricsCard(
    state: WeatherUiState,
    onTriggerAlertTest: () -> Unit,
    onSelectDay: (Int) -> Unit
) {
    when (state) {
        is WeatherUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0061A4))
            }
        }

        is WeatherUiState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2B8B5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF601410).copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF601410), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Sensor integration failure", color = Color(0xFF601410), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = state.message, color = Color(0xFF601410).copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        }

        is WeatherUiState.Success -> {
            // Success view
            val activeIdx = state.activeDayIndex.coerceIn(0, (state.dailyDates.size - 1).coerceAtLeast(0))
            val dayName = if (activeIdx == 0) "Today" else {
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(state.dailyDates.getOrElse(activeIdx) { "" })
                    val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    outputFormat.format(date ?: Date())
                } catch (e: Exception) {
                    "Day ${activeIdx + 1}"
                }
            }

            val currentTemp = if (activeIdx == 0) state.temp else state.dailyMaxTemp.getOrElse(activeIdx) { state.temp }
            val precipProb = if (activeIdx == 0) state.rainProb else state.dailyPrecipProb.getOrElse(activeIdx) { state.rainProb }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Headline Gradient Showcase Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0061A4), // Deep premium blue
                                        Color(0xFF7CB9FF)  // Soft light sky blue
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            // Headline metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = state.cityName.uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = state.country ?: "Global Coordinate Data",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD6E3FF)),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = dayName.uppercase(),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = Color(0xFF001B3E),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Huge primary degree view
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${currentTemp.roundToInt()}°",
                                    color = Color.White,
                                    fontSize = 72.sp,
                                    fontWeight = FontWeight.Light,
                                    fontFamily = FontFamily.SansSerif,
                                    letterSpacing = (-2).sp
                                )

                                // Dynamic weather icon simulation depending on rain and temp
                                val icon = when {
                                    precipProb > 70 -> Icons.Outlined.Thunderstorm
                                    precipProb > 30 -> Icons.Outlined.CloudQueue
                                    currentTemp < 10 -> Icons.Outlined.AcUnit
                                    else -> Icons.Outlined.WbSunny
                                }
                                val summaryText = when {
                                    precipProb > 70 -> "Heavy Showers"
                                    precipProb > 30 -> "Precipitation Risk"
                                    currentTemp < 10 -> "Subzero Atmospheric"
                                    else -> "Clear Zenith"
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (icon == Icons.Outlined.WbSunny) Color(0xFFFFD54F) else Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = summaryText,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "H: ${state.dailyMaxTemp.getOrElse(activeIdx) { state.temp }.roundToInt()}°  L: ${state.dailyMinTemp.getOrElse(activeIdx) { state.temp }.roundToInt()}°",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Grid 2x2 Card Elements
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherMetricCard(
                        title = "Humidity",
                        value = "${state.humidity.roundToInt()}%",
                        progressBarFraction = (state.humidity / 100.0).toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                    WeatherMetricCard(
                        title = "Rain Prob.",
                        value = "$precipProb%",
                        progressBarFraction = (precipProb / 100.0).toFloat(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WeatherMetricCard(
                        title = "Wind Speed",
                        value = "${state.windSpeed.roundToInt()} km/h",
                        subText = "SW Direction",
                        subTextColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    WeatherMetricCard(
                        title = "Altitude",
                        value = "${state.elevation.roundToInt()} m",
                        subText = "Above sea level",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    progressBarFraction: Float? = null,
    subText: String? = null,
    subTextColor: Color = Color(0xFF44474E).copy(alpha = 0.6f)
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F9)),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title.uppercase(),
                color = Color(0xFF44474E).copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = value,
                color = Color(0xFF1A1C1E),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (progressBarFraction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressBarFraction.coerceIn(0f, 1f))
                            .background(Color(0xFF0061A4), CircleShape)
                    )
                }
            }
            if (subText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    color = subTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InteractiveAnalyticsCard(
    state: WeatherUiState,
    onSelectDay: (Int) -> Unit
) {
    if (state is WeatherUiState.Success) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(32.dp))
                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(32.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "7-DAY TEMPERATURE ANALYTICS",
                            color = Color(0xFF1A1C1E),
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Interactive visualizer • Drag/tap to inspect daily high/low curves",
                            color = Color(0xFF44474E),
                            fontSize = 11.sp
                        )
                    }

                    // Legends
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LegendItem(color = Color(0xFFFF5252), text = "Max")
                        LegendItem(color = Color(0xFF0061A4), text = "Min")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Modern visualizer with custom Interactive Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    InteractiveTemperatureGraph(
                        maxTemps = state.dailyMaxTemp,
                        minTemps = state.dailyMinTemp,
                        dates = state.dailyDates,
                        activeIndex = state.activeDayIndex,
                        onSelectIndex = onSelectDay,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(text = text, color = Color(0xFF44474E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InteractiveTemperatureGraph(
    maxTemps: List<Double>,
    minTemps: List<Double>,
    dates: List<String>,
    activeIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .testTag("interactive_canvas_visualizer")
    ) {
        val width = size.width
        val height = size.height

        if (maxTemps.isEmpty() || minTemps.isEmpty()) return@Canvas

        val numPoints = maxTemps.size
        val stepX = if (numPoints > 1) width / (numPoints - 1) else width
        val paddingY = 24.dp.toPx()

        val absPeakMax = (maxTemps.maxOrNull() ?: 100.0).toFloat()
        val absPeakMin = (minTemps.minOrNull() ?: 0.0).toFloat()
        val delta = if (absPeakMax - absPeakMin == 0f) 1f else (absPeakMax - absPeakMin)

        // Utility lambda to convert temperatures to exact Canvas height points
        val convertY = { temp: Float ->
            val scale = (temp - absPeakMin) / delta
            height - paddingY - (scale * (height - 2 * paddingY))
        }

        // Draw horizontal grid helper lines
        val gridLinesCount = 3
        for (i in 0..gridLinesCount) {
            val y = paddingY + i * (height - 2 * paddingY) / gridLinesCount
            drawLine(
                color = Color(0xFFE1E2EC),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // Generate Path points
        val maxPoints = maxTemps.mapIndexed { idx, t -> Offset(idx * stepX, convertY(t.toFloat())) }
        val minPoints = minTemps.mapIndexed { idx, t -> Offset(idx * stepX, convertY(t.toFloat())) }

        if (maxPoints.size >= 2 && minPoints.size >= 2 && maxPoints.size == minPoints.size) {
            val listSize = maxPoints.size
            // Draw curves with linear gradients
            val maxPath = Path().apply {
                moveTo(maxPoints[0].x, maxPoints[0].y)
                for (i in 1 until listSize) {
                    val p0 = maxPoints[i - 1]
                    val p1 = maxPoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
            }

            val minPath = Path().apply {
                moveTo(minPoints[0].x, minPoints[0].y)
                for (i in 1 until listSize) {
                    val p0 = minPoints[i - 1]
                    val p1 = minPoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
            }

            // Render min-max range fill path
            val fillPath = Path().apply {
                moveTo(maxPoints[0].x, maxPoints[0].y)
                for (i in 1 until listSize) {
                    val p0 = maxPoints[i - 1]
                    val p1 = maxPoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
                lineTo(minPoints[listSize - 1].x, minPoints[listSize - 1].y)
                for (i in (listSize - 2) downTo 0) {
                    val p0 = minPoints[i + 1]
                    val p1 = minPoints[i]
                    cubicTo(
                        (p0.x + p1.x) / 2, p0.y,
                        (p0.x + p1.x) / 2, p1.y,
                        p1.x, p1.y
                    )
                }
                close()
            }

            // Draw range translucent brush
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF5252).copy(alpha = 0.15f), Color(0xFF0061A4).copy(alpha = 0.1f))
                )
            )

            // Draw curves lines
            drawPath(path = maxPath, color = Color(0xFFFF5252), style = Stroke(width = 3.dp.toPx()))
            drawPath(path = minPath, color = Color(0xFF0061A4), style = Stroke(width = 3.dp.toPx()))
        }

        // Draw interactive selector vertical line and highlighting glowing dots
        val safeActiveIndex = activeIndex.coerceIn(0, (maxPoints.size - 1).coerceAtLeast(0))
        if (maxPoints.isNotEmpty() && minPoints.isNotEmpty() && safeActiveIndex < maxPoints.size && safeActiveIndex < minPoints.size) {
            val activeX = safeActiveIndex * stepX
            drawLine(
                color = Color(0xFF0061A4),
                start = Offset(activeX, 0f),
                end = Offset(activeX, height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )

            // Draw active nodes
            drawCircle(
                color = Color(0xFF0061A4),
                radius = 7.dp.toPx(),
                center = Offset(activeX, maxPoints[safeActiveIndex].y)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(activeX, maxPoints[safeActiveIndex].y)
            )

            drawCircle(
                color = Color(0xFF0061A4),
                radius = 7.dp.toPx(),
                center = Offset(activeX, minPoints[safeActiveIndex].y)
            )
            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(activeX, minPoints[safeActiveIndex].y)
            )
        }
    }

    // Compose overlay on top of canvas for touch triggers to offer perfect response and ripple feed
    val interactionCount = maxTemps.size
    Row(modifier = Modifier.fillMaxSize()) {
        for (idx in 0 until interactionCount) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color(0xFF0061A4).copy(alpha = 0.12f)),
                        onClick = { onSelectIndex(idx) }
                    )
            )
        }
    }
}

@Composable
fun Forecast7DayListCard(
    state: WeatherUiState,
    onSelectDay: (Int) -> Unit
) {
    if (state is WeatherUiState.Success) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(32.dp))
                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(32.dp))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "7-DAY EXTENDED FORECASTS",
                    color = Color(0xFF1A1C1E),
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val count = minOf(
                        state.dailyMaxTemp.size,
                        state.dailyMinTemp.size,
                        state.dailyPrecipProb.size,
                        state.dailyDates.size,
                        7
                    )
                    for (i in 0 until count) {
                        val maxT = state.dailyMaxTemp[i]
                        val minT = state.dailyMinTemp[i]
                        val prob = state.dailyPrecipProb[i]
                        val dateString = state.dailyDates[i]
                        val isActive = i == state.activeDayIndex

                        val dateLabel = try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val date = inputFormat.parse(dateString)
                            val dayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                            dayFormat.format(date ?: Date())
                        } catch (e: Exception) {
                            dateString
                        }

                        // Weather state descriptive vectors selector
                        val icon = when {
                            prob > 70 -> Icons.Default.Thunderstorm
                            prob > 30 -> Icons.Default.CloudQueue
                            maxT < 10 -> Icons.Default.AcUnit
                            else -> Icons.Default.WbSunny
                        }

                        val containerColor = if (isActive) Color(0xFFD6E3FF) else Color(0xFFF3F4F9)
                        val borderStroke = if (isActive) BorderStroke(1.dp, Color(0xFF0061A4).copy(alpha = 0.2f)) else null

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = borderStroke,
                            modifier = Modifier
                                .clickable { onSelectDay(i) }
                                .fillMaxWidth()
                                .shadow(if (isActive) 2.dp else 0.dp, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isActive) {
                                            Color(0xFF0061A4)
                                        } else {
                                            if (icon == Icons.Default.WbSunny) Color(0xFFFFB300) else Color(0xFF0061A4)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Column {
                                        Text(
                                            text = dateLabel,
                                            color = if (isActive) Color(0xFF001B3E) else Color(0xFF1A1C1E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (prob > 30) "Precipitation choice: $prob%" else "Dry, standard conditions",
                                            color = if (isActive) Color(0xFF001B3E).copy(alpha = 0.8f) else Color(0xFF44474E),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${minT.roundToInt()}°",
                                        color = if (isActive) Color(0xFF001B3E).copy(alpha = 0.8f) else Color(0xFF44474E),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "/",
                                        color = if (isActive) Color(0xFF001B3E).copy(alpha = 0.5f) else Color(0xFFC4C6D0),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${maxT.roundToInt()}°",
                                        color = if (isActive) Color(0xFF001B3E) else Color(0xFF1A1C1E),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimulationPanelDialog(
    onDismiss: () -> Unit,
    onSimulateAlert: (String, String) -> Unit
) {
    var customTitle by remember { mutableStateOf("Tornado Warning") }
    var customMsg by remember { mutableStateOf("Severe rotating atmospheric hazard detected approach. Take deep interior cover!") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .shadow(6.dp, RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFFE1E2EC), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SEVERE WEATHER SIMULATOR",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1C1E),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Forces a localized high-priority emergency alert sweep, deploying a real Android system notification alert.",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E)
                )

                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Alert Designation") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1A1C1E),
                        unfocusedTextColor = Color(0xFF1A1C1E),
                        focusedBorderColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedLabelColor = Color(0xFF44474E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customMsg,
                    onValueChange = { customMsg = it },
                    label = { Text("Information Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1A1C1E),
                        unfocusedTextColor = Color(0xFF1A1C1E),
                        focusedBorderColor = Color(0xFF0061A4),
                        unfocusedBorderColor = Color(0xFFC4C6D0),
                        focusedLabelColor = Color(0xFF0061A4),
                        unfocusedLabelColor = Color(0xFF44474E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF535F70))
                    }
                    Button(
                        onClick = { onSimulateAlert(customTitle, customMsg) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A))
                    ) {
                        Text("Post Broadcast Notification", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
