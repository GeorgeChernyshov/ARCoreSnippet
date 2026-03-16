package com.example.arcoresnippet.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen() {

    @Serializable
    data class ARCore(val recordingPath: String? = null) : Screen()

    @Serializable
    data object Map : Screen()

    @Serializable
    data object Source : Screen()

    @Serializable
    data object Welcome : Screen()
}