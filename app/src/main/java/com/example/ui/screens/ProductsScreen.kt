package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.ScanPeViewModel
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: ScanPeViewModel) {
    val currentBusinessId by viewModel.selectedBusinessId.collectAsState()
    val products by viewModel.products.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

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

    val lowStockCount = remember(products) {
        products.count { it.isStockTracked && it.stockQuantity <= it.lowStockThreshold }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("सामान व स्टॉक (Products & Stock)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ScanPeEmerald,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("नया प्रोडक्ट जोड़ें")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Low Stock Warning Alert Banner (Section 6)
            if (lowStockCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "कम स्टॉक अलर्ट (Low Stock Alert)",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "$lowStockCount सामान का स्टॉक खत्म होने वाला है!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar & Category Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("प्रोडक्ट खोजें (Search)...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

            // Product List
            items(filteredProducts) { product ->
                ProductInventoryCard(
                    product = product,
                    onEdit = { productToEdit = product },
                    onDelete = { viewModel.deleteProduct(product) }
                )
            }

            if (filteredProducts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("कोई प्रोडक्ट नहीं मिला। 'नया प्रोडक्ट जोड़ें' दबाएं!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // Add or Edit Product Dialog
    if (showAddDialog || productToEdit != null) {
        val target = productToEdit
        AddEditProductDialog(
            product = target,
            businessId = currentBusinessId,
            onDismiss = {
                showAddDialog = false
                productToEdit = null
            },
            onSave = { saved ->
                viewModel.saveProduct(saved)
                showAddDialog = false
                productToEdit = null
            }
        )
    }
}

@Composable
fun ProductInventoryCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = product.isStockTracked && product.stockQuantity <= product.lowStockThreshold

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
                Text(product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${product.price.toInt()} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ScanPeEmerald
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            product.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (product.isStockTracked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLowStock) "⚠️ स्टॉक: केवल ${product.stockQuantity} बाकी" else "उपलब्ध स्टॉक: ${product.stockQuantity} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLowStock) ScanPeRedCredit else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ScanPeRedCredit)
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    businessId: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.toInt()?.toString() ?: "") }
    var unit by remember { mutableStateOf(product?.unit ?: "PCS") }
    var category by remember { mutableStateOf(product?.category ?: "Grocery") }
    var isTracked by remember { mutableStateOf(product?.isStockTracked ?: true) }
    var stockStr by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "50") }

    val units = listOf("PCS", "KG", "LTR", "PACK", "MTR")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (product == null) "नया प्रोडक्ट जोड़ें (Add Product)" else "प्रोडक्ट एडिट करें (Edit Product)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("प्रोडक्ट का नाम (Product Name)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("कीमत (Price ₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("श्रेणी (Category)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Unit selection
                Text("इकाई (Unit)", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    items(units) { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Stock tracking toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("स्टॉक ट्रैक करें (Track Stock)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isTracked, onCheckedChange = { isTracked = it })
                }

                if (isTracked) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("शुरुआती स्टॉक मात्रा (Stock Qty)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("रद्द करें") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val price = priceStr.toDoubleOrNull() ?: 0.0
                            val stock = stockStr.toIntOrNull() ?: 0
                            if (name.isNotBlank() && price > 0) {
                                val savedProduct = product?.copy(
                                    name = name,
                                    price = price,
                                    unit = unit,
                                    category = category,
                                    isStockTracked = isTracked,
                                    stockQuantity = stock
                                ) ?: Product(
                                    id = "PROD_${UUID.randomUUID().toString().take(8)}",
                                    businessId = businessId,
                                    name = name,
                                    price = price,
                                    unit = unit,
                                    category = category,
                                    isStockTracked = isTracked,
                                    stockQuantity = stock
                                )
                                onSave(savedProduct)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald)
                    ) {
                        Text("सुरक्षित करें (Save)")
                    }
                }
            }
        }
    }
}
