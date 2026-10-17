package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.AppMode
import com.example.ui.ScanPeViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(viewModel: ScanPeViewModel) {
    val allBusinesses by viewModel.allBusinesses.collectAsState()
    val allBills by viewModel.bills.collectAsState()
    val allCustomers by viewModel.customers.collectAsState()
    val transactions by viewModel.khataTransactions.collectAsState()
    val rewardCodes by viewModel.rewardCodes.collectAsState()
    val disputes by viewModel.disputes.collectAsState()
    val currentBusiness by viewModel.currentBusiness.collectAsState()

    val totalPlatformSales = remember(allBills) { allBills.sumOf { it.finalAmount } }
    val totalPlatformCredit = remember(allCustomers) { allCustomers.sumOf { it.totalOutstandingUdhaar } }
    val totalOnlinePayments = remember(transactions) {
        transactions.filter { it.type == "PAYMENT" }.sumOf { it.amount }
    }
    val totalPoints = remember(rewardCodes) { rewardCodes.sumOf { it.points } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScanPe Admin Panel", fontWeight = FontWeight.Bold) },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.setAppMode(AppMode.BUSINESS) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("दुकान पर लौटें")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Header stats
            item {
                Text(
                    "प्लेटफ़ॉर्म सारांश (Platform Overview)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Total Businesses", "${allBusinesses.size} Active", Icons.Default.Store, ScanPeEmerald, Modifier.weight(1f))
                    MetricCard("Total Customers", "${allCustomers.size}", Icons.Default.Group, ScanPeNavy, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Billing Volume", "₹${totalPlatformSales.toInt()}", Icons.Default.CurrencyRupee, ScanPeBlueUpi, Modifier.weight(1f))
                    MetricCard("Total Outstanding", "₹${totalPlatformCredit.toInt()}", Icons.Default.WarningAmber, ScanPeRedCredit, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Online UPI Settled", "₹${totalOnlinePayments.toInt()}", Icons.Default.CheckCircle, ScanPeGreenPaid, Modifier.weight(1f))
                    MetricCard("Total Points Issued", "$totalPoints Pts", Icons.Default.Stars, ScanPeGold, Modifier.weight(1f))
                }
            }

            // Section 25: Business Subscription Management
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Business Subscription Plans (Section 25)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "दुकान: ${currentBusiness?.name ?: "Current Shop"} • वर्तमान प्लान: ${currentBusiness?.subscriptionPlan ?: "1 Month Free Trial"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    currentBusiness?.let { biz ->
                                        viewModel.updateBusinessProfile(biz.copy(subscriptionPlan = "1 Month Free Trial"))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Free Trial", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    currentBusiness?.let { biz ->
                                        viewModel.updateBusinessProfile(biz.copy(subscriptionPlan = "₹199 / Month Pro Plan"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("₹199/Month", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    currentBusiness?.let { biz ->
                                        viewModel.updateBusinessProfile(biz.copy(subscriptionPlan = "₹1,999 / Year Super Plan"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeBlueUpi),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("₹1999/Year", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Fraud Monitoring & Audit Log (Section 26)
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "धोखाधड़ी निगरानी व विवाद (Fraud Monitoring & Disputes)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (disputes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = ScanPeGreenLight)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = ScanPeGreenPaid)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("कोई विवाद या संदिग्ध गतिविधि नहीं मिली। सिस्टम सुरक्षित है! ✅", color = ScanPeGreenPaid, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(disputes) { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ग्राहक विवाद: ${tx.customerName} (${tx.customerPhone})", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Text("प्रविष्टि: ${tx.description} (₹${tx.amount.toInt()})", style = MaterialTheme.typography.bodySmall)
                            Text("कारण: ${tx.disputeReason ?: "No details"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.resolveDispute(tx) },
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("विवाद सुलझाएं (Resolve Dispute)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Registered Businesses List
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text("पंजीकृत व्यवसाय (Registered Businesses)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allBusinesses) { biz ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
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
                        Column {
                            Text(biz.name, fontWeight = FontWeight.Bold)
                            Text("${biz.ownerName} • ${biz.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("UPI: ${biz.upiId}", style = MaterialTheme.typography.labelSmall, color = ScanPeEmerald)
                        }
                        Surface(
                            color = ScanPeEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ACTIVE",
                                color = ScanPeEmerald,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
