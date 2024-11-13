package org.chaos.office.services

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.chaos.office.components.HomePage
import org.chaos.office.components.SignInPage
import org.chaos.office.configuration.PageSize

@Composable
fun navigation(
    navController: NavHostController = rememberNavController(),
    screenSize: PageSize,
    modifier: Modifier
){
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        composable(route = "login") {
            SignInPage(screenSize) {
                navController.navigate(route = "home")
            }
        }
        composable(route = "home") {
            HomePage(screenSize)
        }
    }
}