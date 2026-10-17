package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.ScanPeViewModel
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(viewModel: ScanPeViewModel) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    val customerName by viewModel.billingCustomerName.collectAsState()
    val customerPhone by viewModel.billingCustomerPhone.collectAsState()
    val discount by viewModel.billingDiscount.collectAsState()
    val taxPercent by viewModel.billingTaxPercent.collectAsState()
    val paymentMethod by viewModel.billingPaymentMethod.collectAsState()
    val paidAmount by viewModel.billingPaidAmount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showUpiQrDialog by remember { mutableStateOf(false) }

    // Categories
    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchCategory = selectedCategory == "All" || prod.category == selectedCategory
            val matchQuery = searchQuery.isBlank() || prod.name.contains(searchQuery, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    val subtotal = remember(cartItems) { cartItems.sumOf { it.product.price * it.quantity } }
    val tax = if (taxPercent > 0) ((subtotal - discount) * taxPercent / 100.0) else 0.0
    val finalAmount = (subtotal - discount + tax).coerceAtLeast(0.0)
    val pointsEarned = ((finalAmount / 100.0) * (currentBusiness?.pointsPer100Rs ?: 1)).toInt()

    val actualPaid = when (paymentMethod) {
        "CASH", "UPI" -> finalAmount
        "UDHAAR" -> 0.0
        else -> paidAmount.coerceIn(0.0, finalAmount)
    }
    val creditAmount = (finalAmount - actualPaid).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("नया बिल (Billing POS)", fontWeight = FontWeight.Bold)
                        Text(
                            "${cartItems.size} आइटम कार्ट में • कुल: ₹${finalAmount.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ScanPeEmerald
                        )
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Cart", tint = ScanPeRedCredit)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Customer Phone & Name Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ग्राहक विवरण (Customer Details)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { showCustomerPicker = true }) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ग्राहक चुनें")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { newPhone ->
                                    viewModel.setBillingCustomer(customerName, newPhone)
                                    // auto-match customer
                                    val match = customers.find { it.phone == newPhone.trim() }
                                    if (match != null) {
                                        viewModel.setBillingCustomer(match.name, newPhone)
                                    }
                                },
                                label = { Text("मोबाइल नंबर") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ScanPeEmerald) },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { newName ->
                                    viewModel.setBillingCustomer(newName, customerPhone)
                                },
                                label = { Text("ग्राहक का नाम") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )
                        }

                        // Existing Udhaar Badge if any
                        val existingCust = customers.find { it.phone == customerPhone.trim() }
                        if (existingCust != null && existingCust.totalOutstandingUdhaar > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = ScanPeRedCredit.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "⚠️ इस ग्राहक का पहले से ₹${existingCust.totalOutstandingUdhaar.toInt()} उधार बाकी है!",
                                    color = ScanPeRedCredit,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Product Search & Categories
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("सामान खोजें (Search Product)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { showCustomItemDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("अन्य")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ScanPeEmerald.copy(alpha = 0.2f),
                                    selectedLabelColor = ScanPeEmerald
                                )
                            )
                        }
                    }
                }
            }

            // Products Grid
            item {
                Text(
                    text = "सामान सूची (Select Products)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(filteredProducts) { product ->
                val inCart = cartItems.find { it.product.id == product.id }
                ProductBillingCard(
                    product = product,
                    quantityInCart = inCart?.quantity ?: 0.0,
                    onAdd = { viewModel.addToCart(product) },
                    onDecrease = {
                        val currentQty = inCart?.quantity ?: 0.0
                        viewModel.updateCartQuantity(product.id, currentQty - 1.0)
                    }
                )
            }

            // Cart Summary & Checkout Details
            if (cartItems.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "बिल का सारांश (Bill Summary)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // Cart Items List
                            cartItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("₹${item.product.price.toInt()} / ${item.product.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Quantity Selector
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    viewModel.updateCartQuantity(item.product.id, item.quantity - 1.0)
                                                }
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.padding(6.dp))
                                        }

                                        Text(
                                            text = "${item.quantity} ${item.product.unit}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        Surface(
                                            shape = CircleShape,
                                            color = ScanPeEmerald.copy(alpha = 0.2f),
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clickable {
                                                    viewModel.updateCartQuantity(item.product.id, item.quantity + 1.0)
                                                }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = ScanPeEmerald, modifier = Modifier.padding(6.dp))
                                        }
                                    }

                                    Text(
                                        "₹${(item.product.price * item.quantity).toInt()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            // Subtotal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal")
                                Text("₹${subtotal.toInt()}")
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Discount & Tax Inputs
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (discount > 0) discount.toInt().toString() else "",
                                    onValueChange = {
                                        viewModel.setBillingDiscount(it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("छूट (Discount ₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = if (taxPercent > 0) taxPercent.toInt().toString() else "",
                                    onValueChange = {
                                        viewModel.setBillingTaxPercent(it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("GST/Tax %") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Total Bill Banner
                            Surface(
                                color = ScanPeEmerald.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("कुल बिल (Final Bill)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("₹${finalAmount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = ScanPeEmerald)
                                }
                            }

                            // Loyalty points preview
                            if (pointsEarned > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Stars, contentDescription = null, tint = ScanPeGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "ग्राहक को मिलेंगे: +$pointsEarned Points (One-Time Code बनेगा)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Payment Method Selection
                            Text("भुगतान का तरीका (Payment Method)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PaymentChip("CASH", "💵 नकद", paymentMethod == "CASH") {
                                    viewModel.setBillingPaymentMethod("CASH")
                                }
                                PaymentChip("UPI", "📱 UPI", paymentMethod == "UPI") {
                                    viewModel.setBillingPaymentMethod("UPI")
                                    showUpiQrDialog = true
                                }
                                PaymentChip("UDHAAR", "📒 उधार", paymentMethod == "UDHAAR") {
                                    viewModel.setBillingPaymentMethod("UDHAAR")
                                }
                                PaymentChip("SPLIT", "⚖️ Split", paymentMethod == "SPLIT") {
                                    viewModel.setBillingPaymentMethod("SPLIT")
                                    viewModel.setBillingPaidAmount(finalAmount / 2)
                                }
                            }

                            // If Split Payment selected
                            if (paymentMethod == "SPLIT") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = paidAmount.toInt().toString(),
                                    onValueChange = {
                                        viewModel.setBillingPaidAmount(it.toDoubleOrNull() ?: 0.0)
                                    },
                                    label = { Text("प्राप्त नकद/UPI (Paid Now ₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = {
                                        Text("बाकी उधार खाते में जुड़ेगा: ₹${creditAmount.toInt()}", color = ScanPeRedCredit, fontWeight = FontWeight.Bold)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Generate Bill Button
                            Button(
                                onClick = {
                                    if (customerPhone.isBlank() && (paymentMethod == "UDHAAR" || creditAmount > 0)) {
                                        viewModel.showMessage("उधार बिल के लिए ग्राहक का मोबाइल नंबर अनिवार्य है!")
                                    } else {
                                        viewModel.completeBill()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("बिल बनाएं व रसीद दें (Generate Bill)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Item Dialog
    if (showCustomItemDialog) {
        AddCustomItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onAdd = { name, price, qty, unit ->
                viewModel.addCustomItemToCart(name, price, qty, unit)
                showCustomItemDialog = false
            }
        )
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            onDismiss = { showCustomerPicker = false },
            onSelect = { cust ->
                viewModel.setBillingCustomer(cust.name, cust.phone)
                showCustomerPicker = false
            }
        )
    }

    // Dynamic Instant UPI QR Code Dialog for screen payment
    if (showUpiQrDialog) {
        val upiId = currentBusiness?.upiId ?: "shop@upi"
        val shopName = currentBusiness?.name ?: "ScanPe Shop"
        val upiUri = "upi://pay?pa=$upiId&pn=${shopName.replace(" ", "%20")}&am=${finalAmount.toInt()}&cu=INR&tn=BillPayment"

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
                    Text("राशि: ₹${finalAmount.toInt()} • $shopName", style = MaterialTheme.typography.bodyMedium, color = ScanPeEmerald, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    QrCodeCanvas(payload = upiUri, size = 200.dp)

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(upiId, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showUpiQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("भुगतान प्राप्त हुआ (Payment Done)")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductBillingCard(
    product: Product,
    quantityInCart: Double,
    onAdd: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${product.price.toInt()} / ${product.unit}", style = MaterialTheme.typography.bodySmall, color = ScanPeEmerald, fontWeight = FontWeight.Bold)
                    if (product.isStockTracked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (product.stockQuantity <= product.lowStockThreshold) "⚠️ ${product.stockQuantity} बाकी" else "स्टॉक: ${product.stockQuantity}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.stockQuantity <= product.lowStockThreshold) ScanPeRedCredit else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (quantityInCart > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onDecrease)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.padding(6.dp))
                    }
                    Text(
                        "${quantityInCart.toInt()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = ScanPeEmerald,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onAdd)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.padding(6.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("जोड़ें")
                }
            }
        }
    }
}

@Composable
fun PaymentChip(key: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) ScanPeEmerald else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("PCS") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("नया आइटम जोड़ें (Custom Item)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("सामान का नाम (Product Name)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("मूल्य (Price ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("मात्रा (Qty)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val qty = qtyStr.toDoubleOrNull() ?: 1.0
                            if (name.isNotBlank() && price > 0) {
                                onAdd(name, price, qty, unit)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                    ) {
                        Text("कार्ट में जोड़ें")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerPickerDialog(
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSelect: (Customer) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(customers, search) {
        customers.filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true) || it.phone.contains(search)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ग्राहक चुनें (Select Customer)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("नाम या नंबर से खोजें...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { cust ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cust) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.SemiBold)
                                Text(cust.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (cust.totalOutstandingUdhaar > 0) {
                                Text("उधार: ₹${cust.totalOutstandingUdhaar.toInt()}", color = ScanPeRedCredit, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("बंद करें")
                }
            }
        }
    }
}
