/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicornhunters.components.dashboard.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.kmgi.unicornhunters.AndroidDisplayManager
import com.kmgi.unicornhunters.R
import com.kmgi.unicornhunters.components.dashboard.DashboardRepositoryImpl
import com.kmgi.unicornhunters.components.dashboard.DashboardViewModel
import com.kmgi.unicornhunters.components.dashboard.SideEffect
import com.kmgi.unicornhunters.dependencies.rememberAndroidDependenciesGraph
import com.kmgi.unicornhunters.ui.common.utils.viewModel
import com.kmgi.unicornhunters.ui.common.video.HlsVideoPlayer
import com.kmgi.unicornhunters.ui.common.video.State
import com.kmgi.unicornhunters.ui.main.app.ToolbarWithMenu
import com.kmgi.unicornhunters.ui.theme.Colors
import com.kmgi.unicorns.core.utils.DisplayManager
import kotlinx.coroutines.flow.collect

internal const val TAG = "DASHBOARD"

@ExperimentalAnimationApi
@Composable
fun DashboardScreen(scaffoldState: ScaffoldState) {
    val graph = rememberAndroidDependenciesGraph()
    val viewModel = viewModel("dashboard") {
        DashboardViewModel(
            DashboardRepositoryImpl(
                broadcastService = graph.networkAdapter.broadcast(),
                broadcastsDao = graph.persistence.dao(),
            )
        )
    }

    LaunchedEffect(TAG) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is SideEffect.ProductDetails -> Unit
                SideEffect.ComingSoon -> {
                    scaffoldState.snackbarHostState.showSnackbar("Coming soon")
                }
            }
        }
    }

    val viewModelState by viewModel.state.collectAsState()

    val displayManager = AndroidDisplayManager(LocalContext.current as Activity)
    if (viewModelState is DashboardViewModel.State.Player) {
        displayManager.orientation = DisplayManager.Orientation.Landscape
        displayManager.fullscreen(true)
    } else {
        displayManager.orientation = DisplayManager.Orientation.Portrait
        displayManager.fullscreen(false)
    }

    Surface(color = Colors.Background) {
        @Suppress("UnnecessaryVariable")
        when (val state = viewModelState) {
            is DashboardViewModel.State.Dashboard -> Dashboard(
                broadcast = state.broadcast,
                autoPlay = state.autoplay,
                playFromPosition = state.initialPosition,

                investments = state.investments,
                onInvestmentPressed = viewModel::onInvestmentSelected,

                futureProducts = state.futureProducts,
                onFutureProductsPressed = viewModel::onProductSelected,

                news = state.news,

                onFullscreen = viewModel::onFullscreen,
            )

            is DashboardViewModel.State.Loading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator(
                    color = Colors.YellowCircleProgressBar,
                    strokeWidth = 2.dp
                )
            }

            is DashboardViewModel.State.Player -> HlsVideoPlayer(
                uri = state.url.toUri(),
                state = State.Play,
                modifier = Modifier.fillMaxSize(),
                startFrom = state.initialPosition,
            ) { player, playerState ->
                displayManager.keepScreenOn(keepScreenOn = playerState == State.Play)

                Box(
                    contentAlignment = Alignment.TopEnd,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.exo_controls_fullscreen_exit),
                        contentDescription = "Exit fullscreen",
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp)
                            .clip(CircleShape)
                            .clickable {
                                player.release()
                                viewModel.onExitFullscreen(player.currentPosition)
                            }
                            .padding(12.dp)
                    )
                }

                BackHandler {
                    player.release()
                    viewModel.navigateBack(
                        lastPosition = player.currentPosition,
                        isPlaying = playerState == State.Play
                    )
                }
            }
        }

        if (viewModelState !is DashboardViewModel.State.Player) {
            ToolbarWithMenu(title = "Unicorns\nShow")
        }
    }
}

@ExperimentalAnimationApi
@Preview
@Composable
fun DashboardScreenPreview() = Surface(color = Colors.Background) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(scaffoldState = scaffoldState) {
        DashboardScreen(scaffoldState)
    }
}