package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.Business
import com.example.data.model.Customer
import com.example.data.model.KhataTransaction
import com.example.ui.AppMode
import com.example.ui.BusinessTab
import com.example.ui.ScanPeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanPeViewModel,
    onNavigateTab: (BusinessTab) -> Unit,
    onOpenBillInvoice: (Bill) -> Unit,
    onOpenShopQr: () -> Unit
) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val allBusinesses by viewModel.allBusinesses.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.khataTransactions.collectAsState()
    val rewardCodes by viewModel.rewardCodes.collectAsState()

    var showBusinessSwitchMenu by remember { mutableStateOf(false) }

    // Calculate Today's Metrics
    val startOfDay = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val todayBills = remember(bills, startOfDay) {
        bills.filter { it.createdAt >= startOfDay }
    }
    val todaySales = remember(todayBills) {
        todayBills.sumOf { it.finalAmount }
    }
    val todayPaidCollected = remember(todayBills, transactions, startOfDay) {
        val fromBills = todayBills.sumOf { it.paidAmount }
        val manualPayments = transactions.filter { it.type == "PAYMENT" && it.createdAt >= startOfDay && it.billId == null }.sumOf { it.amount }
        fromBills + manualPayments
    }
    val todayUdhaarGiven = remember(todayBills, transactions, startOfDay) {
        val fromBills = todayBills.sumOf { it.creditAmount }
        val manualCredits = transactions.filter { it.type == "CREDIT" && it.createdAt >= startOfDay && it.billId == null }.sumOf { it.amount }
        fromBills + manualCredits
    }
    val todayPointsGiven = remember(todayBills) {
        todayBills.sumOf { it.pointsEarned }
    }

    val totalSales = remember(bills) { bills.sumOf { it.finalAmount } }
    val totalOutstandingCredit = remember(customers) { customers.sumOf { it.totalOutstandingUdhaar } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Top Store Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(ScanPeNavy, ScanPeNavySurface)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { showBusinessSwitchMenu = true }
                            ) {
                                Text(
                                    text = currentBusiness?.name ?: "ScanPe Store",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Store",
                                    tint = ScanPeEmeraldLight
                                )
                            }
                            Text(
                                text = "${currentBusiness?.category ?: "Retail"} • ${currentBusiness?.city ?: "India"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        // Counter QR Icon Button
                        Surface(
                            shape = CircleShape,
                            color = ScanPeEmerald.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable { onOpenShopQr() }
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "Shop QR",
                                tint = ScanPeEmeraldLight,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        // Business Switch Dropdown
                        DropdownMenu(
                            expanded = showBusinessSwitchMenu,
                            onDismissRequest = { showBusinessSwitchMenu = false }
                        ) {
                            Text(
                                "दुकान चुनें (Switch Store)",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            allBusinesses.forEach { biz ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(biz.name, fontWeight = FontWeight.SemiBold)
                                            Text(biz.category, style = MaterialTheme.typography.bodySmall)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectBusiness(biz.id)
                                        showBusinessSwitchMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Store, contentDescription = null, tint = ScanPeEmerald)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Switcher Banner (Business vs Customer Pass vs Admin)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("दुकानदार (Owner)") },
                                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScanPeEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.setAppMode(AppMode.CUSTOMER) },
                                label = { Text("ग्राहक पास (Customer)") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.setAppMode(AppMode.ADMIN) },
                                label = { Text("Admin") },
                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Today's Summary (आज का सारांश)
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "आज का सारांश (Today's Summary)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2x3 Grid of Today's Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "आज की बिक्री (Sales)",
                        value = "₹${todaySales.toInt()}",
                        icon = Icons.Default.TrendingUp,
                        color = ScanPeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "आज के बिल (Bills)",
                        value = "${todayBills.size}",
                        icon = Icons.Default.ReceiptLong,
                        color = ScanPeBlueUpi,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "आज का उधार (Credit)",
                        value = "₹${todayUdhaarGiven.toInt()}",
                        icon = Icons.Default.Outbox,
                        color = ScanPeRedCredit,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "प्राप्त Payment (Paid)",
                        value = "₹${todayPaidCollected.toInt()}",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = ScanPeGreenPaid,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Loyalty Points",
                        value = "+$todayPointsGiven Pts",
                        icon = Icons.Default.Stars,
                        color = ScanPeGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Redeemed Rewards",
                        value = "2 Claimed",
                        icon = Icons.Default.CardGiftcard,
                        color = Color(0xFF8E24AA),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Overall Business Overview (कुल जानकारी)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "कुल जानकारी (Store Overview)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        OverviewStat("कुल ग्राहक", "${customers.size}", Icons.Default.People)
                        OverviewStat("कुल बिक्री", "₹${totalSales.toInt()}", Icons.Default.CurrencyRupee)
                        OverviewStat("बाकी उधार", "₹${totalOutstandingCredit.toInt()}", Icons.Default.WarningAmber, isAlert = totalOutstandingCredit > 0)
                    }
                }
            }
        }

        // Quick Action Buttons
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = "त्वरित क्रियाएँ (Quick Actions)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "नया बिल बनाएं\n(New Bill)",
                        icon = Icons.Default.AddShoppingCart,
                        containerColor = ScanPeEmerald,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(BusinessTab.BILLING) }
                    )
                    QuickActionButton(
                        title = "उधार खाता\n(Khata)",
                        icon = Icons.Default.MenuBook,
                        containerColor = ScanPeNavySurface,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(BusinessTab.KHATA) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "दुकान QR कोड\n(Shop QR)",
                        icon = Icons.Default.QrCodeScanner,
                        containerColor = ScanPeBlueUpi,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenShopQr
                    )
                    QuickActionButton(
                        title = "रिवॉर्ड्स व पॉइंट्स\n(Loyalty)",
                        icon = Icons.Default.CardGiftcard,
                        containerColor = ScanPeGold,
                        contentColor = ScanPeNavy,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(BusinessTab.LOYALTY) }
                    )
                }
            }
        }

        // Recent Bills List Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "हाल के बिल (Recent Bills)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigateTab(BusinessTab.BILLING) }) {
                    Text("+ नया बिल")
                }
            }
        }

        if (bills.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("अभी कोई बिल नहीं बना है। 'नया बिल बनाएं' दबाएं!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(bills.take(5)) { bill ->
                BillListItem(
                    bill = bill,
                    onClick = { onOpenBillInvoice(bill) }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun OverviewStat(title: String, value: String, icon: ImageVector, isAlert: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isAlert) ScanPeRedCredit else ScanPeEmerald,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) ScanPeRedCredit else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun BillListItem(bill: Bill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (bill.paymentMethod) {
                        "UDHAAR" -> ScanPeRedCredit.copy(alpha = 0.15f)
                        "UPI" -> ScanPeBlueUpi.copy(alpha = 0.15f)
                        else -> ScanPeEmerald.copy(alpha = 0.15f)
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = when (bill.paymentMethod) {
                            "UDHAAR" -> ScanPeRedCredit
                            "UPI" -> ScanPeBlueUpi
                            else -> ScanPeEmerald
                        },
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = bill.customerName.ifBlank { "Walk-in Customer" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${bill.billNumber} • ${bill.paymentMethod}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${bill.finalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (bill.creditAmount > 0) {
                    Text(
                        text = "उधार: ₹${bill.creditAmount.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScanPeRedCredit,
                        fontWeight = FontWeight.Bold
                    )
                } else if (bill.pointsEarned > 0) {
                    Text(
                        text = "+${bill.pointsEarned} Pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScanPeGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
