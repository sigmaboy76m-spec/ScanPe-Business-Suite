package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppMode
import com.example.ui.BusinessTab
import com.example.ui.ScanPeViewModel
import com.example.ui.components.DigitalInvoiceDialog
import com.example.ui.screens.*
import com.example.ui.theme.ScanPeEmerald
import com.example.ui.theme.ScanPeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanPeTheme {
                ScanPeMainApp()
            }
        }
    }
}

@Composable
fun ScanPeMainApp(viewModel: ScanPeViewModel = viewModel()) {
    val appMode by viewModel.appMode.collectAsState()
    val businessTab by viewModel.businessTab.collectAsState()
    val showInvoiceDialog by viewModel.showInvoiceDialog.collectAsState()
    val lastGeneratedBill by viewModel.lastGeneratedBill.collectAsState()
    val lastGeneratedItems by viewModel.lastGeneratedItems.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    var showShopQrScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // Back handling
    BackHandler(enabled = appMode != AppMode.BUSINESS || showShopQrScreen || showProfileScreen) {
        if (showShopQrScreen) {
            showShopQrScreen = false
        } else if (showProfileScreen) {
            showProfileScreen = false
        } else if (appMode != AppMode.BUSINESS) {
            viewModel.setAppMode(AppMode.BUSINESS)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (appMode == AppMode.BUSINESS && !showShopQrScreen && !showProfileScreen) {
                NavigationBar(
                    windowInsets = WindowInsets.navigationBars,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavBarItem(
                        selected = businessTab == BusinessTab.DASHBOARD,
                        onClick = { viewModel.setBusinessTab(BusinessTab.DASHBOARD) },
                        icon = Icons.Filled.Dashboard,
                        unselectedIcon = Icons.Outlined.Dashboard,
                        label = "होम"
                    )
                    NavBarItem(
                        selected = businessTab == BusinessTab.BILLING,
                        onClick = { viewModel.setBusinessTab(BusinessTab.BILLING) },
                        icon = Icons.Filled.AddShoppingCart,
                        unselectedIcon = Icons.Outlined.AddShoppingCart,
                        label = "बिल POS"
                    )
                    NavBarItem(
                        selected = businessTab == BusinessTab.KHATA,
                        onClick = { viewModel.setBusinessTab(BusinessTab.KHATA) },
                        icon = Icons.Filled.MenuBook,
                        unselectedIcon = Icons.Outlined.MenuBook,
                        label = "खाता"
                    )
                    NavBarItem(
                        selected = businessTab == BusinessTab.PRODUCTS,
                        onClick = { viewModel.setBusinessTab(BusinessTab.PRODUCTS) },
                        icon = Icons.Filled.Inventory2,
                        unselectedIcon = Icons.Outlined.Inventory2,
                        label = "स्टॉक"
                    )
                    NavBarItem(
                        selected = businessTab == BusinessTab.LOYALTY,
                        onClick = { viewModel.setBusinessTab(BusinessTab.LOYALTY) },
                        icon = Icons.Filled.Stars,
                        unselectedIcon = Icons.Outlined.Stars,
                        label = "रिवॉर्ड्स"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showShopQrScreen -> {
                    ShopQrScreen(
                        viewModel = viewModel,
                        onBack = { showShopQrScreen = false }
                    )
                }
                showProfileScreen -> {
                    BusinessProfileScreen(
                        viewModel = viewModel,
                        onBack = { showProfileScreen = false }
                    )
                }
                appMode == AppMode.CUSTOMER -> {
                    CustomerPortalScreen(
                        viewModel = viewModel,
                        onOpenBillInvoice = { bill ->
                            viewModel.showBillInvoice(bill)
                        }
                    )
                }
                appMode == AppMode.ADMIN -> {
                    AdminPanelScreen(viewModel = viewModel)
                }
                else -> {
                    // Business Owner Mode
                    when (businessTab) {
                        BusinessTab.DASHBOARD -> {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateTab = { tab -> viewModel.setBusinessTab(tab) },
                                onOpenBillInvoice = { bill -> viewModel.showBillInvoice(bill) },
                                onOpenShopQr = { showShopQrScreen = true }
                            )
                        }
                        BusinessTab.BILLING -> {
                            BillingScreen(viewModel = viewModel)
                        }
                        BusinessTab.KHATA -> {
                            KhataScreen(viewModel = viewModel)
                        }
                        BusinessTab.PRODUCTS -> {
                            ProductsScreen(viewModel = viewModel)
                        }
                        BusinessTab.LOYALTY -> {
                            LoyaltyRewardsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    // Digital Invoice Dialog
    if (showInvoiceDialog && lastGeneratedBill != null) {
        DigitalInvoiceDialog(
            bill = lastGeneratedBill!!,
            items = lastGeneratedItems,
            business = currentBusiness,
            onDismiss = { viewModel.closeInvoiceDialog() }
        )
    }
}

@Composable
private fun RowScope.NavBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    unselectedIcon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                if (selected) icon else unselectedIcon,
                contentDescription = label,
                tint = if (selected) ScanPeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) ScanPeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = ScanPeEmerald.copy(alpha = 0.15f)
        )
    )
}
