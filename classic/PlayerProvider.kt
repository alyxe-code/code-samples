package com.p2lem8dev.uikit.video

import android.view.View

interface PlayerProvider<TView : View> {
    fun provide(
        view: TView,
        callbacks: Player.Callbacks?,
        id: String = view.hashCode().toString(),
    ): Player
}

fun <TView : View> PlayerProvider<TView>.provide(
    view: TView,
    callbacks: CompoundPlayerCallbacks,
    id: String = view.hashCode().toString(),
) = provide(
    view = view,
    callbacks = object : Player.Callbacks {
        override fun onStateChanged(state: Player.State) = callbacks.apply(state, null)
        override fun onPlayerError(throwable: Throwable) = callbacks.apply(Player.State.Idle, throwable)
    },
    id = id
)

fun interface CompoundPlayerCallbacks {
    fun apply(state: Player.State, throwable: Throwable?)
}

fun <TView : View> PlayerProvider<TView>.provide(
    view: TView,
    id: String = view.hashCode().toString(),
    onError: (Throwable) -> Unit = {},
    onStateChanged: (Player.State) -> Unit = {},
) = provide(
    view = view,
    callbacks = object : Player.Callbacks {
        override fun onStateChanged(state: Player.State) = onStateChanged.invoke(state)
        override fun onPlayerError(throwable: Throwable) = onError.invoke(throwable)
    },
    id = id,
)