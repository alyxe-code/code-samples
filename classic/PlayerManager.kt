package com.p2lem8dev.uikit.video

import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

interface PlayerManager<TIdentifier, TView : View> {

    fun play(
        id: TIdentifier,
        link: String,
        view: TView,
        callbacks: Player.Callbacks? = null,
        linkType: PlaybackLinkType = PlaybackLinkType.Unknown,
        cacheDataSource: Boolean = true,
    )

    fun resume(): Boolean

    fun pause(): Boolean

    fun pause(identifier: TIdentifier): Boolean

    fun stop(identifier: TIdentifier): Boolean
    fun stopCurrent(): Boolean

    fun destroy()

    var currentPosition: Long

    var volume: Float

    enum class PlaybackLinkType {
        Hls,
        Dash,
        Unknown,
    }

    /**
     * Current position flow collected with delay
     * @param delay milliseconds to wait before emit current position
     */
    fun currentPositionAsFlow(delay: Long = 100) = flow {
        while (true) {
            delay(delay)
            emit(currentPosition)
        }
    }
}