package org.sweetlab.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import org.sweetlab.ui.admin.AdminDashboard
import org.sweetlab.ui.admin.InventoryScreen
import org.sweetlab.ui.admin.RecipeManagementScreen
import org.sweetlab.ui.admin.ReportingScreen
import org.sweetlab.ui.admin.UserManagementScreen
import org.sweetlab.ui.admin.WalletSummaryScreen
import org.sweetlab.ui.auth.LoginScreen
import org.sweetlab.ui.chef.ProductionScreen
import org.sweetlab.ui.components.OfflineIndicator
import org.sweetlab.ui.representative.CustomerManagementScreen
import org.sweetlab.ui.representative.CustomerPaymentScreen
import org.sweetlab.ui.representative.ExpenseRecordingScreen
import org.sweetlab.ui.representative.SalesScreen

// ── Route constants ──────────────────────────────────────────────────

/** Top-level route groups */
object Routes {
    const val AUTH = "auth"
    const val ADMIN = "admin"
    const val CHEF = "chef"
    const val REPRESENTATIVE = "representative"
}

/** Individual screen routes */
object Screen {
    // Auth
    const val LOGIN = "auth/login"

    // Admin sub-graph
    const val ADMIN_DASHBOARD = "admin/dashboard"
    const val USER_MANAGEMENT = "admin/users"
    const val INVENTORY = "admin/inventory"
    const val RECIPE_MANAGEMENT = "admin/recipes"
    const val WALLET_SUMMARY = "admin/wallets"
    const val REPORTS = "admin/reports"

    // Chef sub-graph
    const val PRODUCTION = "chef/production"

    // Representative sub-graph
    const val SALES = "representative/sales"
    const val CUSTOMER_MANAGEMENT = "representative/customers"
    const val EXPENSE_RECORDING = "representative/expenses"
    const val CUSTOMER_PAYMENT = "representative/payments"
}

/**
 * Maps a user role string (from the Rust core Session) to the correct
 * start destination after login.
 *
 * - Admin → AdminDashboard  (Req 1.3, 2.6)
 * - Chef → ProductionScreen (Req 1.3, 2.4)
 * - Representative → SalesScreen (Req 1.3, 2.5)
 */
fun roleToStartRoute(role: String): String = when (role.lowercase()) {
    "admin" -> Screen.ADMIN_DASHBOARD
    "chef" -> Screen.PRODUCTION
    "representative" -> Screen.SALES
    else -> Screen.LOGIN
}

/**
 * Returns the navigation graph group for a given role.
 */
fun roleToGraphRoute(role: String): String = when (role.lowercase()) {
    "admin" -> Routes.ADMIN
    "chef" -> Routes.CHEF
    "representative" -> Routes.REPRESENTATIVE
    else -> Routes.AUTH
}

// ── Main NavHost ─────────────────────────────────────────────────────

/**
 * Root composable hosting the full navigation graph.
 * Wraps all screens with the [OfflineIndicator] banner (Req 13.4).
 */
@Composable
fun SweetLabNavHost(
    navController: NavHostController = rememberNavController()
) {
    // Offline state — will be driven by SweetLabCore.isOnline() once bindings are ready
    var isOffline by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Offline banner shown across ALL screens (Req 13.4)
            OfflineIndicator(isOffline = isOffline, modifier = Modifier.fillMaxWidth())

            NavHost(
                navController = navController,
                startDestination = Routes.AUTH,
                modifier = Modifier.weight(1f)
            ) {
                // ── Auth sub-graph ───────────────────────────────
                navigation(
                    startDestination = Screen.LOGIN,
                    route = Routes.AUTH
                ) {
                    composable(Screen.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = { role ->
                                val targetGraph = roleToGraphRoute(role)
                                navController.navigate(targetGraph) {
                                    // Clear the back stack so the user can't
                                    // press back to return to the login screen
                                    popUpTo(Routes.AUTH) { inclusive = true }
                                }
                            }
                        )
                    }
                }

                // ── Admin sub-graph (Req 2.6) ────────────────────
                navigation(
                    startDestination = Screen.ADMIN_DASHBOARD,
                    route = Routes.ADMIN
                ) {
                    composable(Screen.ADMIN_DASHBOARD) {
                        AdminDashboard(
                            onNavigateToUsers = { navController.navigate(Screen.USER_MANAGEMENT) },
                            onNavigateToInventory = { navController.navigate(Screen.INVENTORY) },
                            onNavigateToRecipes = { navController.navigate(Screen.RECIPE_MANAGEMENT) },
                            onNavigateToWallets = { navController.navigate(Screen.WALLET_SUMMARY) },
                            onNavigateToReports = { navController.navigate(Screen.REPORTS) }
                        )
                    }
                    composable(Screen.USER_MANAGEMENT) {
                        UserManagementScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.INVENTORY) {
                        InventoryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.RECIPE_MANAGEMENT) {
                        RecipeManagementScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.WALLET_SUMMARY) {
                        WalletSummaryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.REPORTS) {
                        ReportingScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Chef sub-graph (Req 2.4) ─────────────────────
                navigation(
                    startDestination = Screen.PRODUCTION,
                    route = Routes.CHEF
                ) {
                    composable(Screen.PRODUCTION) {
                        ProductionScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }

                // ── Representative sub-graph (Req 2.5) ───────────
                navigation(
                    startDestination = Screen.SALES,
                    route = Routes.REPRESENTATIVE
                ) {
                    composable(Screen.SALES) {
                        SalesScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.CUSTOMER_MANAGEMENT) {
                        CustomerManagementScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.EXPENSE_RECORDING) {
                        ExpenseRecordingScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.CUSTOMER_PAYMENT) {
                        CustomerPaymentScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
