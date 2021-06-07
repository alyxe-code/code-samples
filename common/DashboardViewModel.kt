/*
 * Copyright (c) 2021.
 */

package com.kmgi.unicornhunters.components.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmgi.unicornhunters.components.dashboard.DashboardViewModel.State.Loading.NextState
import com.kmgi.unicornhunters.components.dashboard.loaders.BroadcastWithProductsService
import com.kmgi.unicornhunters.components.dashboard.loaders.DashboardService
import com.kmgi.unicorns.core.models.Broadcast
import com.kmgi.unicorns.core.models.Investment
import com.kmgi.unicorns.core.models.NewsPost
import com.kmgi.unicorns.core.models.Product
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect

class DashboardViewModel(repository: DashboardRepository) : ViewModel() {

    private val _state = MutableStateFlow<State>(
        State.Loading(
            NextState.Dashboard(
                fromPlayer = false,
                lastPosition = 0L
            )
        )
    )
    val state: StateFlow<State> = _state

    private var lastBroadcast: Broadcast? = null

    private val _sideEffect = MutableSharedFlow<SideEffect>(
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
    internal val sideEffect = _sideEffect

    private val services = listOf(
        BroadcastWithProductsService(repository),
    )
    private var loadJob: Job? = null


    init {
        viewModelScope.launch {
            _state.collect { state ->
                if (state !is State.Loading) return@collect
                onLoadNextState(state.nextState)
            }
        }
    }

    private suspend fun onLoadNextState(state: NextState) {
        loadJob?.cancel()
        when (state) {
            is NextState.Dashboard -> {
                _state.value = State.Dashboard(
                    autoplay = state.fromPlayer,
                    initialPosition = state.lastPosition
                )

                load()
            }
            is NextState.Player -> {
                val url = lastBroadcast
                    ?.broadcastVideo
                    ?.let { it.playbackHls ?: it.playbackDash }

                if (url == null) {
                    _state.value = State.Loading(
                        NextState.Dashboard(
                            fromPlayer = true,
                            lastPosition = 0L
                        )
                    )
                } else {
                    _state.value = State.Player(
                        url = url,
                        initialPosition = state.lastPosition ?: 0L
                    )
                }
            }
        }
    }

    private suspend fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.Default) {
            for (method in DashboardService.LoadingMethod.values()) {
                var state = _state.value

                services.map { service ->
                    async {
                        val newData = try {
                            service.loadData(method)
                        } catch (t: Throwable) {
                            null
                        }

                        newData ?: return@async

                        state = service.createState(state, newData)
                    }
                }.awaitAll()

                _state.value = state
            }
        }
    }

    fun onProductSelected(product: Product) {
        _sideEffect.tryEmit(SideEffect.ProductDetails(product))
    }

    @Suppress("UNUSED_PARAMETER")
    fun onInvestmentSelected(investment: Investment) {
        _sideEffect.tryEmit(SideEffect.ComingSoon)
    }

    fun onFullscreen(lastPosition: Long) {
        _state.value = State.Loading(NextState.Player(lastPosition))
    }

    fun onExitFullscreen(lastPosition: Long, isPlaying: Boolean = true) {
        _state.value = State.Loading(
            NextState.Dashboard(
                fromPlayer = isPlaying,
                lastPosition = lastPosition.takeIf { isPlaying } ?: 0,
            )
        )
    }

    fun navigateBack(lastPosition: Long, isPlaying: Boolean = true) {
        if (_state.value !is State.Player) return

        _state.value = State.Loading(
            NextState.Dashboard(
                fromPlayer = isPlaying,
                lastPosition = lastPosition.takeIf { isPlaying } ?: 0
            )
        )
    }

    sealed class State {
        class Loading(val nextState: NextState) : State() {
            sealed class NextState {
                class Dashboard(
                    val fromPlayer: Boolean,
                    val lastPosition: Long,
                ) : NextState()

                class Player(val lastPosition: Long?) : NextState()
            }
        }

        data class Dashboard(
            val broadcast: Broadcast? = null,
            val autoplay: Boolean = false,
            val initialPosition: Long = 0L,
            val investments: List<Investment>? = null,
            val futureProducts: List<Product>? = null,
            val news: List<NewsPost>? = null,
        ) : State()

        data class Player(
            val url: String,
            val initialPosition: Long = 0L,
        ) : State()
    }
}
