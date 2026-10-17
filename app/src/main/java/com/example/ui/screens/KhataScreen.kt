package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.KhataTransaction
import com.example.ui.ScanPeViewModel
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataScreen(viewModel: ScanPeViewModel) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val allTransactions by viewModel.khataTransactions.collectAsState()
    val selectedCustomer by viewModel.selectedCustomerForLedger.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyUdhaar by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery, filterOnlyUdhaar) {
        customers.filter { cust ->
            val matchSearch = searchQuery.isBlank() ||
                    cust.name.contains(searchQuery, ignoreCase = true) ||
                    cust.phone.contains(searchQuery)
            val matchFilter = !filterOnlyUdhaar || cust.totalOutstandingUdhaar > 0
            matchSearch && matchFilter
        }
    }

    val totalOutstanding = remember(customers) {
        customers.sumOf { it.totalOutstandingUdhaar }
    }
    val debtorCount = remember(customers) {
        customers.count { it.totalOutstandingUdhaar > 0 }
    }

    if (selectedCustomer != null) {
        // Customer Ledger Detail Screen
        val customerTransactions = remember(allTransactions, selectedCustomer) {
            allTransactions.filter { it.customerId == selectedCustomer?.id }
        }
        CustomerLedgerDetailView(
            customer = selectedCustomer!!,
            businessName = currentBusiness?.name ?: "Shop",
            businessUpi = currentBusiness?.upiId ?: "shop@upi",
            businessId = currentBusiness?.id ?: "shop",
            transactions = customerTransactions,
            onBack = { viewModel.selectCustomerForLedger(null) },
            onAddCredit = { amount, note ->
                viewModel.addCustomerUdhaar(selectedCustomer!!.id, amount, note)
            },
            onReceivePayment = { amount, note ->
                viewModel.receiveCustomerPayment(selectedCustomer!!.id, amount, note)
            },
            onResolveDispute = { tx ->
                viewModel.resolveDispute(tx)
            }
        )
    } else {
        // All Customers Khata List
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("डिजिटल खाता (Digital Khata)", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Total Udhaar Banner
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ScanPeRedCredit.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "कुल बकाया उधार (Total Outstanding)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "₹${totalOutstanding.toInt()}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ScanPeRedCredit
                                )
                                Text(
                                    "$debtorCount ग्राहकों का उधार बाकी है",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = ScanPeRedCredit.copy(alpha = 0.15f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = ScanPeRedCredit,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                // Search & Filter
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ग्राहक का नाम या मोबाइल नंबर...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = filterOnlyUdhaar,
                                onClick = { filterOnlyUdhaar = !filterOnlyUdhaar },
                                label = { Text("केवल बकाया उधार वाले (Has Udhaar)") },
                                leadingIcon = {
                                    if (filterOnlyUdhaar) Icon(Icons.Default.Check, contentDescription = null)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Customer List
                items(filteredCustomers) { cust ->
                    CustomerKhataRow(
                        customer = cust,
                        onClick = { viewModel.selectCustomerForLedger(cust) }
                    )
                }

                if (filteredCustomers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("कोई ग्राहक नहीं मिला।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerKhataRow(customer: Customer, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (customer.totalOutstandingUdhaar > 0) ScanPeRedCredit.copy(alpha = 0.15f) else ScanPeEmerald.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (customer.totalOutstandingUdhaar > 0) ScanPeRedCredit else ScanPeEmerald,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(customer.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (customer.totalOutstandingUdhaar > 0) {
                    Text(
                        "₹${customer.totalOutstandingUdhaar.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ScanPeRedCredit
                    )
                    Text(
                        "बाकी उधार",
                        style = MaterialTheme.typography.labelSmall,
                        color = ScanPeRedCredit
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ScanPeGreenLight
                    ) {
                        Text(
                            "हिसाब चुकता",
                            color = ScanPeGreenPaid,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerDetailView(
    customer: Customer,
    businessName: String,
    businessUpi: String,
    businessId: String,
    transactions: List<KhataTransaction>,
    onBack: () -> Unit,
    onAddCredit: (Double, String) -> Unit,
    onReceivePayment: (Double, String) -> Unit,
    onResolveDispute: (KhataTransaction) -> Unit
) {
    val context = LocalContext.current
    var showCreditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold)
                        Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showCreditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeRedCredit),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("उधार दें (Gave ₹)", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showPaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeGreenPaid),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("रुपये मिले (Got ₹)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            // Customer Balance Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (customer.totalOutstandingUdhaar > 0) ScanPeRedCredit.copy(alpha = 0.1f) else ScanPeGreenLight
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (customer.totalOutstandingUdhaar > 0) "कुल बाकी उधार (Outstanding Udhaar)" else "कोई उधार बाकी नहीं है (All Settled)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "₹${customer.totalOutstandingUdhaar.toInt()}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (customer.totalOutstandingUdhaar > 0) ScanPeRedCredit else ScanPeGreenPaid
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reminder & UPI Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (customer.totalOutstandingUdhaar > 0) {
                                Button(
                                    onClick = {
                                        sendWhatsAppReminder(context, customer, businessName, businessId)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("WhatsApp Reminder", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = { showUpiQrDialog = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("UPI QR दिखाएं", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Ledger History Title
            item {
                Text(
                    text = "लेन-देन इतिहास (Transaction History)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("अभी कोई लेन-देन दर्ज नहीं है।", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(transactions) { tx ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (tx.type == "CREDIT") ScanPeRedCredit.copy(alpha = 0.15f) else ScanPeGreenLight,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (tx.type == "CREDIT") Icons.Default.ArrowOutward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (tx.type == "CREDIT") ScanPeRedCredit else ScanPeGreenPaid,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(tx.description, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            dateFormat.format(Date(tx.createdAt)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if (tx.type == "CREDIT") "+" else "-"}₹${tx.amount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.type == "CREDIT") ScanPeRedCredit else ScanPeGreenPaid
                                    )
                                    Text(
                                        text = "बाकी: ₹${tx.balanceAfter.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Dispute Alert if reported
                            if (tx.disputeStatus == "REPORTED") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "⚠️ ग्राहक ने इस प्रविष्टि पर आपत्ति जताई है:",
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                tx.disputeReason ?: "कोई कारण नहीं लिखा गया",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Button(
                                            onClick = { onResolveDispute(tx) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("समाधान करें (Resolve)", style = MaterialTheme.typography.labelSmall)
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

    // Add Credit Dialog
    if (showCreditDialog) {
        AmountNoteDialog(
            title = "उधार जोड़ें (Give Credit)",
            confirmLabel = "उधार दर्ज करें",
            confirmColor = ScanPeRedCredit,
            onDismiss = { showCreditDialog = false },
            onConfirm = { amount, note ->
                onAddCredit(amount, note)
                showCreditDialog = false
            }
        )
    }

    // Receive Payment Dialog
    if (showPaymentDialog) {
        AmountNoteDialog(
            title = "रुपये प्राप्त (Receive Payment)",
            confirmLabel = "भुगतान दर्ज करें",
            confirmColor = ScanPeGreenPaid,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note ->
                onReceivePayment(amount, note)
                showPaymentDialog = false
            }
        )
    }

    // UPI QR Dialog for customer balance
    if (showUpiQrDialog) {
        val upiUri = "upi://pay?pa=$businessUpi&pn=${businessName.replace(" ", "%20")}&am=${customer.totalOutstandingUdhaar.toInt()}&cu=INR&tn=UdhaarPayment"
        Dialog(onDismissRequest = { showUpiQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ग्राहक को QR स्कैन कराएं", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("बकाया राशि: ₹${customer.totalOutstandingUdhaar.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ScanPeRedCredit)
                    Spacer(modifier = Modifier.height(16.dp))

                    QrCodeCanvas(payload = upiUri, size = 200.dp)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(businessUpi, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showUpiQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("बंद करें")
                    }
                }
            }
        }
    }
}

@Composable
fun AmountNoteDialog(
    title: String,
    confirmLabel: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("राशि (Amount ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("विवरण / नोट (Description)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onConfirm(amt, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

private fun sendWhatsAppReminder(context: Context, customer: Customer, shopName: String, businessId: String) {
    val message = "नमस्ते ${customer.name} जी,\nआपके $shopName के खाते में ₹${customer.totalOutstandingUdhaar.toInt()} का उधार बाकी है।\nकृपया अपना हिसाब देखने व UPI द्वारा भुगतान करने के लिए इस लिंक पर जाएं: https://scanpe.in/shop/$businessId\nधन्यवाद! 🙏"
    val encoded = Uri.encode(message)
    val cleanPhone = customer.phone.replace(Regex("[^0-9]"), "")
    val targetPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

    val url = "https://api.whatsapp.com/send?phone=$targetPhone&text=$encoded"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic share
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Reminder"))
    }
}
