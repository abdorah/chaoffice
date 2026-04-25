package com.inventory.app.navigation

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.inventory.app.ui.screens.BudgetScreen
import com.inventory.app.ui.screens.CategoryListScreen
import com.inventory.app.ui.screens.DashboardScreen
import com.inventory.app.ui.screens.DealListScreen
import com.inventory.app.ui.screens.LocationListScreen
import com.inventory.app.ui.screens.LoginScreen
import com.inventory.app.ui.screens.PersonListScreen
import com.inventory.app.ui.screens.ProductFormScreen
import com.inventory.app.ui.screens.ProductListScreen
import com.inventory.app.ui.screens.ReportScreen
import com.inventory.app.ui.screens.StockTrackingScreen
import com.inventory.app.ui.screens.SyncSettingsScreen
import com.inventory.app.ui.screens.UserListScreen
import com.inventory.app.util.ErrorHandler
import com.inventory.app.viewmodel.AuthViewModel
import com.inventory.app.viewmodel.BudgetViewModel
import com.inventory.app.viewmodel.CategoryViewModel
import com.inventory.app.viewmodel.DealViewModel
import com.inventory.app.viewmodel.LocationViewModel
import com.inventory.app.viewmodel.PersonViewModel
import com.inventory.app.viewmodel.ProductViewModel
import com.inventory.app.viewmodel.ReportViewModel
import com.inventory.app.viewmodel.StockViewModel
import com.inventory.app.viewmodel.SyncViewModel
import com.inventory.app.viewmodel.UserViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String) {
    object Login : Screen("login", "Login")
    object Dashboard : Screen("dashboard", "Dashboard")
    object Products : Screen("products", "Products")
    object ProductForm : Screen("product_form/{id}", "Product") {
        fun createRoute(id: Long): String = "product_form/$id"
    }
    object Categories : Screen("categories", "Categories")
    object Persons : Screen("persons", "People")
    object Deals : Screen("deals", "Deals")
    object Locations : Screen("locations", "Locations")
    object Stock : Screen("stock", "Stock Tracking")
    object Budget : Screen("budget", "Budget")
    object Reports : Screen("reports", "Reports")
    object Users : Screen("users", "Users")
    object Sync : Screen("sync", "Sync Settings")
}

private val drawerScreens = listOf(
    Screen.Dashboard,
    Screen.Products,
    Screen.Categories,
    Screen.Persons,
    Screen.Deals,
    Screen.Locations,
    Screen.Stock,
    Screen.Budget,
    Screen.Reports,
    Screen.Users,
    Screen.Sync,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val navController: NavHostController = rememberNavController()
    val startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTitle = drawerScreens.find { it.route == currentRoute }?.title
        ?: when (currentRoute) {
            Screen.Login.route -> Screen.Login.title
            Screen.ProductForm.route -> Screen.ProductForm.title
            else -> "Inventory Manager"
        }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe auth errors from ErrorHandler and force logout + navigate to Login
    LaunchedEffect(Unit) {
        ErrorHandler.authErrors.collect { message ->
            authViewModel.forceLogout()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    // Navigate to Login when logged out (covers both manual logout and force logout)
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute != null && currentRoute != Screen.Login.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val showDrawer = isLoggedIn && currentRoute != Screen.Login.route

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        text = "Inventory Manager",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(
                            start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp
                        )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        drawerScreens.forEach { screen ->
                            NavigationDrawerItem(
                                label = { Text(screen.title) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            },
            modifier = modifier
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(currentTitle) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Open navigation drawer")
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                AppNavHost(
                    navController = navController,
                    authViewModel = authViewModel,
                    snackbarHostState = snackbarHostState,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    } else {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            AppNavHost(
                navController = navController,
                authViewModel = authViewModel,
                snackbarHostState = snackbarHostState,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    snackbarHostState: SnackbarHostState,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            val loggedIn by authViewModel.isLoggedIn.collectAsState()
            val authError by authViewModel.error.collectAsState()

            LaunchedEffect(loggedIn) {
                if (loggedIn) {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }

            // Show auth errors as snackbar on login screen
            LaunchedEffect(authError) {
                authError?.let {
                    snackbarHostState.showSnackbar(it)
                    authViewModel.clearError()
                }
            }

            LoginScreen(authViewModel = authViewModel)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }

        composable(Screen.Products.route) {
            val productViewModel: ProductViewModel = viewModel()
            val error by productViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    productViewModel.clearError()
                }
            }
            ProductListScreen(
                productViewModel = productViewModel,
                onNavigateToForm = { id ->
                    navController.navigate(Screen.ProductForm.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.ProductForm.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val productViewModel: ProductViewModel = viewModel()
            val error by productViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    productViewModel.clearError()
                }
            }
            ProductFormScreen(
                productId = id,
                productViewModel = productViewModel,
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Categories.route) {
            val categoryViewModel: CategoryViewModel = viewModel()
            val error by categoryViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    categoryViewModel.clearError()
                }
            }
            CategoryListScreen(categoryViewModel = categoryViewModel)
        }

        composable(Screen.Persons.route) {
            val personViewModel: PersonViewModel = viewModel()
            val error by personViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    personViewModel.clearError()
                }
            }
            PersonListScreen(personViewModel = personViewModel)
        }

        composable(Screen.Deals.route) {
            val dealViewModel: DealViewModel = viewModel()
            val error by dealViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    dealViewModel.clearError()
                }
            }
            DealListScreen(dealViewModel = dealViewModel)
        }

        composable(Screen.Locations.route) {
            val locationViewModel: LocationViewModel = viewModel()
            val error by locationViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    locationViewModel.clearError()
                }
            }
            LocationListScreen(locationViewModel = locationViewModel)
        }

        composable(Screen.Stock.route) {
            val stockViewModel: StockViewModel = viewModel()
            val sessionToken by authViewModel.sessionToken.collectAsState()
            val error by stockViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    stockViewModel.clearError()
                }
            }
            StockTrackingScreen(
                stockViewModel = stockViewModel,
                sessionToken = sessionToken
            )
        }

        composable(Screen.Budget.route) {
            val budgetViewModel: BudgetViewModel = viewModel()
            val sessionToken by authViewModel.sessionToken.collectAsState()
            val error by budgetViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    budgetViewModel.clearError()
                }
            }
            BudgetScreen(
                budgetViewModel = budgetViewModel,
                sessionToken = sessionToken
            )
        }

        composable(Screen.Reports.route) {
            val reportViewModel: ReportViewModel = viewModel()
            val error by reportViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    reportViewModel.clearError()
                }
            }
            ReportScreen(reportViewModel = reportViewModel)
        }

        composable(Screen.Users.route) {
            val userViewModel: UserViewModel = viewModel()
            val error by userViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    userViewModel.clearError()
                }
            }
            UserListScreen(userViewModel = userViewModel)
        }

        composable(Screen.Sync.route) {
            val syncViewModel: SyncViewModel = viewModel()
            val sessionToken by authViewModel.sessionToken.collectAsState()
            val error by syncViewModel.error.collectAsState()
            LaunchedEffect(error) {
                error?.let {
                    snackbarHostState.showSnackbar(it)
                    syncViewModel.clearError()
                }
            }
            SyncSettingsScreen(
                syncViewModel = syncViewModel,
                sessionToken = sessionToken
            )
        }
    }
}
