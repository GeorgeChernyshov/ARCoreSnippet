package com.example.arcoresnippet.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class Screen(val route: String) {

    @Serializable
    data object ARCore : Screen(route = "arCore")

    @Serializable
    data object Map : Screen(route = "map")

    @Serializable
    data object Welcome : Screen(route = "welcome")
}