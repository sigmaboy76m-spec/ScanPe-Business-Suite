package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.RewardCode
import com.example.data.model.RewardTier
import com.example.ui.ScanPeViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyRewardsScreen(viewModel: ScanPeViewModel) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val rewardCodes by viewModel.rewardCodes.collectAsState()
    val rewardTiers by viewModel.rewardTiers.collectAsState()
    val redemptions by viewModel.rewardRedemptions.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Codes & Points, 1: Reward Tiers, 2: Verify Voucher
    var showGenerateCodeDialog by remember { mutableStateOf(false) }
    var showAddTierDialog by remember { mutableStateOf(false) }
    var verifyInputCode by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("लॉयल्टी व रिवॉर्ड्स (Loyalty & Rewards)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Loyalty Rule Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ScanPeGold.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "सक्रिय लॉयल्टी नियम (Current Rule)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "₹100 की खरीदारी = ${currentBusiness?.pointsPer100Rs ?: 1} Point",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ScanPeNavy
                        )
                        Text(
                            "बिल बनते ही सिस्टम अपने आप पॉइंट्स की गणना करता है।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Rule Switch Buttons
                    Row {
                        FilledTonalButton(
                            onClick = {
                                currentBusiness?.let { biz ->
                                    val newRule = if (biz.pointsPer100Rs == 1) 2 else 1
                                    viewModel.updateBusinessProfile(biz.copy(pointsPer100Rs = newRule))
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if ((currentBusiness?.pointsPer100Rs ?: 1) == 1) "बदलें (₹50=1 Pt)" else "बदलें (₹100=1 Pt)")
                        }
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("One-Time Codes") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Rewards Catalog") }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("वाउचर Verify") }
                )
            }

            // Tab Contents
            when (activeTab) {
                0 -> {
                    // Reward Codes List & Generate Button
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "जारी किए गए One-Time Codes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showGenerateCodeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("नया कोड बनाएं")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(rewardCodes) { code ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = if (code.status == "UNUSED") ScanPeEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = code.code,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = if (code.status == "UNUSED") ScanPeEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "+${code.points} Points (बिल: ₹${code.billAmount.toInt()})",
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                dateFormat.format(Date(code.createdAt)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (code.status == "UNUSED") ScanPeGreenLight else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (code.status == "UNUSED") "उपलब्ध (UNUSED)" else "CLAIMED (${code.redeemedByCustomerPhone ?: ""})",
                                            color = if (code.status == "UNUSED") ScanPeGreenPaid else MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (rewardCodes.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("अभी कोई रिवॉर्ड कोड नहीं बना है।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Reward Tiers Catalog
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "दुकान के रिवॉर्ड्स (Reward Tiers)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { showAddTierDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("नया रिवॉर्ड जोड़ें")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(rewardTiers) { tier ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
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
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = ScanPeGold.copy(alpha = 0.2f),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CardGiftcard,
                                                contentDescription = null,
                                                tint = Color(0xFFB45309),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(tier.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                            Text(tier.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                color = ScanPeEmerald.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "${tier.pointsRequired} Points चाहिए",
                                                    color = ScanPeEmerald,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteRewardTier(tier) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ScanPeRedCredit)
                                    }
                                }
                            }
                        }

                        if (rewardTiers.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("कोई रिवॉर्ड नहीं है। 'नया रिवॉर्ड जोड़ें' दबाएं!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Verify Voucher Pass Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ग्राहक का वाउचर सत्यापित करें",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "जब ग्राहक दुकान पर अपना Reward Pass दिखाए, तो उसका 8-अंकीय वाउचर कोड यहाँ डालें:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = verifyInputCode,
                            onValueChange = { verifyInputCode = it.uppercase() },
                            placeholder = { Text("उदा. REW-48921") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = ScanPeEmerald) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (verifyInputCode.isNotBlank()) {
                                    viewModel.verifyVoucher(verifyInputCode)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("सत्यापित करें व छूट दें (Verify Voucher)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "हाल ही में रिडीम किए गए वाउचर",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(redemptions) { red ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(red.rewardTitle, fontWeight = FontWeight.Bold)
                                            Text("ग्राहक: ${red.customerName} (${red.customerPhone})", style = MaterialTheme.typography.bodySmall)
                                            Text("कोड: ${red.voucherCode}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = ScanPeNavy)
                                        }
                                        Surface(
                                            color = if (red.status == "CLAIMED") ScanPeGreenLight else Color(0xFFFFF3E0),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (red.status == "CLAIMED") "VERIFIED ✅" else "PENDING ⏳",
                                                color = if (red.status == "CLAIMED") ScanPeGreenPaid else Color(0xFFE65100),
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
            }
        }
    }

    // Generate Manual Reward Code Dialog
    if (showGenerateCodeDialog) {
        var billAmountStr by remember { mutableStateOf("") }
        var pointsStr by remember { mutableStateOf("5") }

        Dialog(onDismissRequest = { showGenerateCodeDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("One-Time Reward Code बनाएं", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("ग्राहक को Points देने के लिए कोड जनरेट करें:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = billAmountStr,
                        onValueChange = {
                            billAmountStr = it
                            val amt = it.toDoubleOrNull() ?: 0.0
                            val rule = currentBusiness?.pointsPer100Rs ?: 1
                            pointsStr = ((amt / 100.0) * rule).toInt().toString()
                        },
                        label = { Text("बिल राशि (Bill Amount ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pointsStr,
                        onValueChange = { pointsStr = it },
                        label = { Text("दिए जाने वाले Points") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showGenerateCodeDialog = false }) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val pts = pointsStr.toIntOrNull() ?: 0
                                val amt = billAmountStr.toDoubleOrNull() ?: 0.0
                                if (pts > 0) {
                                    viewModel.generateRewardCode(pts, amt)
                                    showGenerateCodeDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                        ) {
                            Text("Generate Code")
                        }
                    }
                }
            }
        }
    }

    // Add Reward Tier Dialog
    if (showAddTierDialog) {
        var title by remember { mutableStateOf("") }
        var pointsCostStr by remember { mutableStateOf("10") }
        var discountValStr by remember { mutableStateOf("50") }
        var desc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddTierDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("नया रिवॉर्ड जोड़ें (Add Reward)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("रिवॉर्ड का शीर्षक (उदा. ₹50 OFF या Free Chai)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pointsCostStr,
                            onValueChange = { pointsCostStr = it },
                            label = { Text("आवश्यक Points") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountValStr,
                            onValueChange = { discountValStr = it },
                            label = { Text("छूट मूल्य (₹ Value)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("शर्तें / विवरण (Terms)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddTierDialog = false }) { Text("रद्द करें") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val pts = pointsCostStr.toIntOrNull() ?: 10
                                val disc = discountValStr.toDoubleOrNull() ?: 0.0
                                if (title.isNotBlank()) {
                                    val tier = RewardTier(
                                        id = "TIER_${UUID.randomUUID().toString().take(8)}",
                                        businessId = currentBusiness?.id ?: "biz",
                                        title = title,
                                        pointsRequired = pts,
                                        discountAmount = disc,
                                        description = desc.ifBlank { "Claim $title" }
                                    )
                                    viewModel.saveRewardTier(tier)
                                    showAddTierDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                        ) {
                            Text("सुरक्षित करें")
                        }
                    }
                }
            }
        }
    }
}
