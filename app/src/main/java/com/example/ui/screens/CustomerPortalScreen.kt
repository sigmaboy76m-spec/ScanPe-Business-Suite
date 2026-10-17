package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.AppMode
import com.example.ui.ScanPeViewModel
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPortalScreen(
    viewModel: ScanPeViewModel,
    onOpenBillInvoice: (Bill) -> Unit
) {
    val context = LocalContext.current
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val allBusinesses by viewModel.allBusinesses.collectAsState()
    val loggedPhone by viewModel.loggedCustomerPhone.collectAsState()
    val customerData by viewModel.currentCustomerData.collectAsState()
    val customerBills by viewModel.customerBills.collectAsState()
    val customerTransactions by viewModel.customerTransactions.collectAsState()
    val rewardTiers by viewModel.rewardTiers.collectAsState()

    var showStoreSwitcher by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showClaimCodeDialog by remember { mutableStateOf(false) }
    var showPayUdhaarDialog by remember { mutableStateOf(false) }
    var showDisputeDialog by remember { mutableStateOf<KhataTransaction?>(null) }
    var activeVoucherPass by remember { mutableStateOf<String?>(null) }

    val nextReward = remember(rewardTiers, customerData) {
        val currentPts = customerData?.loyaltyPoints ?: 0
        rewardTiers.filter { it.pointsRequired > currentPts }.minByOrNull { it.pointsRequired }
    }
    val pointsNeeded = remember(nextReward, customerData) {
        if (nextReward != null) {
            (nextReward.pointsRequired - (customerData?.loyaltyPoints ?: 0)).coerceAtLeast(0)
        } else 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showStoreSwitcher = true }
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    currentBusiness?.name ?: "ScanPe Store",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = ScanPeEmerald)
                            }
                            Text(
                                "Customer Web Portal (PWA)",
                                style = MaterialTheme.typography.labelSmall,
                                color = ScanPeEmerald
                            )
                        }
                    }
                },
                actions = {
                    // Switch back to Business Owner Mode button
                    FilledTonalButton(
                        onClick = { viewModel.setAppMode(AppMode.BUSINESS) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("दुकानदार मोड")
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
            contentPadding = PaddingValues(bottom = 60.dp)
        ) {
            // Customer Header Bar & Login Pill (Section 17 & 18)
            item {
                Surface(
                    color = ScanPeNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "नमस्ते, ${customerData?.name ?: "Customer"}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Ph: $loggedPhone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                            OutlinedButton(
                                onClick = { showLoginDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White.copy(alpha = 0.4f)))
                            ) {
                                Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("लॉगिन / बदलें")
                            }
                        }
                    }
                }
            }

            // Section 18: Customer Dashboard Cards
            // 💰 मेरा उधार (Outstanding Udhaar)
            item {
                val udhaarAmt = customerData?.totalOutstandingUdhaar ?: 0.0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (udhaarAmt > 0) ScanPeRedCredit.copy(alpha = 0.08f) else ScanPeGreenLight
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (udhaarAmt > 0) ScanPeRedCredit.copy(alpha = 0.15f) else ScanPeGreenPaid.copy(alpha = 0.15f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (udhaarAmt > 0) ScanPeRedCredit else ScanPeGreenPaid,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "💰 मेरा उधार (My Udhaar)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${currentBusiness?.name ?: "Shop"} के खाते में",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text(
                                "₹${udhaarAmt.toInt()}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (udhaarAmt > 0) ScanPeRedCredit else ScanPeGreenPaid
                            )
                        }

                        if (udhaarAmt > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showPayUdhaarDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeBlueUpi),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay Udhaar via UPI (उधार चुकाएं)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 🎁 मेरे Points & अगला Reward (Section 18)
            item {
                val pts = customerData?.loyaltyPoints ?: 0
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = ScanPeGold.copy(alpha = 0.2f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Stars,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("🎁 मेरे Points", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("उपलब्ध लॉयल्टी बैलेंस", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Text(
                                "$pts Points",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Next Reward Progress
                        if (nextReward != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("🏆 अगला Reward: ${nextReward.title}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("$pointsNeeded Points बाकी", style = MaterialTheme.typography.labelMedium, color = ScanPeEmerald, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { (pts.toFloat() / nextReward.pointsRequired.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = ScanPeEmerald,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Claim One-Time Code Button (Section 14 & 18)
                        Button(
                            onClick = { showClaimCodeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("रिवॉर्ड कोड डालें (Enter Reward Code)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 🎟️ Available Rewards to Redeem (Section 16 & 18)
            item {
                Text(
                    "🎟️ उपलब्ध रिवॉर्ड्स (Redeem Rewards)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            items(rewardTiers) { tier ->
                val canClaim = (customerData?.loyaltyPoints ?: 0) >= tier.pointsRequired
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tier.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(tier.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${tier.pointsRequired} Points लगेंगे", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                viewModel.claimRewardTier(tier)
                            },
                            enabled = canClaim,
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (canClaim) "Redeem" else "Points कम हैं", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // 🧾 मेरी खरीदारी (Recent Digital Bills - Section 18)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "🧾 मेरी खरीदारी (My Bills & Receipts)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (customerBills.isEmpty()) {
                item {
                    Text(
                        "अभी कोई बिल नहीं मिला।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                items(customerBills.take(5)) { bill ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable { onOpenBillInvoice(bill) },
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
                                Text("बिल #${bill.billNumber}", fontWeight = FontWeight.SemiBold)
                                Text("Payment: ${bill.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${bill.finalAmount.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("रसीद देखें", style = MaterialTheme.typography.labelSmall, color = ScanPeEmerald, fontWeight = FontWeight.Bold)
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = ScanPeEmerald)
                                }
                            }
                        }
                    }
                }
            }

            // 📜 खाता हिस्ट्री व Dispute (Sections 9 & 23)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "📜 उधार व भुगतान हिस्ट्री (Khata Ledger)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (customerTransactions.isEmpty()) {
                item {
                    Text(
                        "कोई लेन-देन नहीं है।",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                items(customerTransactions) { tx ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tx.description, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(dateFormat.format(Date(tx.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (tx.type == "CREDIT") "+" else "-"}₹${tx.amount.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (tx.type == "CREDIT") ScanPeRedCredit else ScanPeGreenPaid
                                    )
                                    Text("बाकी: ₹${tx.balanceAfter.toInt()}", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Dispute Button (Section 23: Report Problem)
                            if (tx.type == "CREDIT" && tx.disputeStatus == "NONE") {
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(
                                    onClick = { showDisputeDialog = tx },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.ReportProblem, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("गलत उधार? समस्या दर्ज करें (Report Problem)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                                }
                            } else if (tx.disputeStatus == "REPORTED") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(4.dp)) {
                                    Text("⚠️ समस्या दर्ज की गई है: ${tx.disputeReason ?: ""}", color = Color(0xFFE65100), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 21: Social Media Links ("Follow Us")
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Follow & Review ${currentBusiness?.name ?: "Shop"}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    openUrl(context, currentBusiness?.socialInstagram ?: "https://instagram.com")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Instagram", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    openUrl(context, currentBusiness?.socialYoutube ?: "https://youtube.com")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("YouTube", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    openUrl(context, currentBusiness?.socialGoogleReview ?: "https://google.com")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeBlueUpi),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Google Review", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Login Dialog (Section 17: Mobile + PIN)
    if (showLoginDialog) {
        var inputPhone by remember { mutableStateOf(loggedPhone) }
        var inputPin by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showLoginDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ग्राहक लॉगिन (Customer Login)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("अपना 10-अंकीय मोबाइल नंबर व 4-अंकीय PIN दर्ज करें:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        label = { Text("मोबाइल नंबर (Mobile)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4) inputPin = it },
                        label = { Text("4-digit PIN (उदा. 1234)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showLoginDialog = false }) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputPhone.isNotBlank()) {
                                    viewModel.setCustomerPhone(inputPhone)
                                    showLoginDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                        ) {
                            Text("लॉगिन करें")
                        }
                    }
                }
            }
        }
    }

    // Store Switcher (Section 19: Multiple Businesses Support)
    if (showStoreSwitcher) {
        Dialog(onDismissRequest = { showStoreSwitcher = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("दुकान चुनें (Select Business)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("आप कई ScanPe दुकानों से जुड़े हैं:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    allBusinesses.forEach { biz ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.selectBusiness(biz.id)
                                    showStoreSwitcher = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = ScanPeEmerald)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(biz.name, fontWeight = FontWeight.Bold)
                                Text(biz.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showStoreSwitcher = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("बंद करें")
                    }
                }
            }
        }
    }

    // Claim One-Time Reward Code Dialog (Section 14)
    if (showClaimCodeDialog) {
        var inputCode by remember { mutableStateOf("") }
        var claimStatusMsg by remember { mutableStateOf<String?>(null) }
        var isError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showClaimCodeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("One-Time Reward Code दर्ज करें", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("दुकानदार द्वारा रसीद पर दिया गया 6-अंकीय कोड डालें:", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { inputCode = it.uppercase() },
                        placeholder = { Text("उदा. A7K92P") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    if (claimStatusMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = claimStatusMsg!!,
                            color = if (isError) ScanPeRedCredit else ScanPeGreenPaid,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showClaimCodeDialog = false }) { Text("बंद करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputCode.isNotBlank()) {
                                    viewModel.redeemCustomerCode(inputCode) { success, msg ->
                                        claimStatusMsg = msg
                                        isError = !success
                                        if (success) {
                                            inputCode = ""
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                        ) {
                            Text("पॉइंट्स क्लेम करें")
                        }
                    }
                }
            }
        }
    }

    // Pay Udhaar Dialog via UPI (Section 11)
    if (showPayUdhaarDialog) {
        val totalUdhaar = customerData?.totalOutstandingUdhaar ?: 0.0
        var customAmountStr by remember { mutableStateOf("") }
        var selectedAmount by remember { mutableStateOf(totalUdhaar) }

        val upiId = currentBusiness?.upiId ?: "shop@upi"
        val shopName = currentBusiness?.name ?: "Shop"
        val payUpiUri = "upi://pay?pa=$upiId&pn=${shopName.replace(" ", "%20")}&am=${selectedAmount.toInt()}&cu=INR&tn=UdhaarSettlement"

        Dialog(onDismissRequest = { showPayUdhaarDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("उधार भुगतान (Pay Udhaar via UPI)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("कुल बकाया: ₹${totalUdhaar.toInt()}", color = ScanPeRedCredit, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Amount Chips (Section 11: पूरा भुगतान, ₹500, ₹1,000, Custom)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedAmount == totalUdhaar && customAmountStr.isEmpty(),
                            onClick = {
                                selectedAmount = totalUdhaar
                                customAmountStr = ""
                            },
                            label = { Text("पूरा ₹${totalUdhaar.toInt()}") }
                        )
                        if (totalUdhaar > 500) {
                            FilterChip(
                                selected = selectedAmount == 500.0 && customAmountStr.isEmpty(),
                                onClick = {
                                    selectedAmount = 500.0
                                    customAmountStr = ""
                                },
                                label = { Text("₹500") }
                            )
                        }
                        if (totalUdhaar > 1000) {
                            FilterChip(
                                selected = selectedAmount == 1000.0 && customAmountStr.isEmpty(),
                                onClick = {
                                    selectedAmount = 1000.0
                                    customAmountStr = ""
                                },
                                label = { Text("₹1,000") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customAmountStr,
                        onValueChange = {
                            customAmountStr = it
                            val amt = it.toDoubleOrNull()
                            if (amt != null && amt > 0) selectedAmount = amt
                        },
                        label = { Text("या अन्य राशि दर्ज करें (Custom Amount ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QrCodeCanvas(payload = payUpiUri, size = 180.dp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("UPI से भुगतान के बाद 'भुगतान संपन्न' दबाएं", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            // Deduct and update Khata
                            customerData?.let { cust ->
                                viewModel.receiveCustomerPayment(cust.id, selectedAmount, "UPI Online Payment by Customer")
                            }
                            showPayUdhaarDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("भुगतान संपन्न (Mark Paid ₹${selectedAmount.toInt()})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Dispute Problem Dialog (Section 23)
    if (showDisputeDialog != null) {
        val targetTx = showDisputeDialog!!
        var reasonText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showDisputeDialog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("समस्या दर्ज करें (Report Problem)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("यदि यह उधार गलत जोड़ा गया है, तो कारण लिखें:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("प्रविष्टि: ${targetTx.description} (₹${targetTx.amount.toInt()})", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = reasonText,
                        onValueChange = { reasonText = it },
                        label = { Text("आपत्ति का कारण (Reason)") },
                        placeholder = { Text("उदा. मैंने यह सामान नहीं लिया था / भुगतान पहले ही कर दिया था") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDisputeDialog = null }) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (reasonText.isNotBlank()) {
                                    viewModel.reportDispute(targetTx, reasonText)
                                    showDisputeDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeRedCredit)
                        ) {
                            Text("शिकायत भेजें")
                        }
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}
