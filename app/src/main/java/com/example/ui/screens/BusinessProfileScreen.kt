package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Business
import com.example.ui.ScanPeViewModel
import com.example.ui.theme.ScanPeEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(
    viewModel: ScanPeViewModel,
    onBack: () -> Unit
) {
    val currentBusiness by viewModel.currentBusiness.collectAsState()

    var name by remember(currentBusiness) { mutableStateOf(currentBusiness?.name ?: "") }
    var ownerName by remember(currentBusiness) { mutableStateOf(currentBusiness?.ownerName ?: "") }
    var phone by remember(currentBusiness) { mutableStateOf(currentBusiness?.phone ?: "") }
    var category by remember(currentBusiness) { mutableStateOf(currentBusiness?.category ?: "Kirana Store") }
    var address by remember(currentBusiness) { mutableStateOf(currentBusiness?.address ?: "") }
    var city by remember(currentBusiness) { mutableStateOf(currentBusiness?.city ?: "") }
    var upiId by remember(currentBusiness) { mutableStateOf(currentBusiness?.upiId ?: "") }
    var openTime by remember(currentBusiness) { mutableStateOf(currentBusiness?.openTime ?: "08:00 AM") }
    var closeTime by remember(currentBusiness) { mutableStateOf(currentBusiness?.closeTime ?: "10:00 PM") }
    var instagram by remember(currentBusiness) { mutableStateOf(currentBusiness?.socialInstagram ?: "") }
    var youtube by remember(currentBusiness) { mutableStateOf(currentBusiness?.socialYoutube ?: "") }
    var googleReview by remember(currentBusiness) { mutableStateOf(currentBusiness?.socialGoogleReview ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("दुकान प्रोफाइल (Business Profile)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("दुकान व मालिक का विवरण (Store Details)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("दुकान / व्यापार का नाम (Business Name)") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("मालिक का नाम (Owner Name)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("मोबाइल नंबर (Phone)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("श्रेणी (Category)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = upiId,
                onValueChange = { upiId = it },
                label = { Text("दुकान का UPI ID (GPay/PhonePe/Paytm)") },
                leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ScanPeEmerald) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("ग्राहक द्वारा ऑनलाइन भुगतान इसी UPI ID पर आएगा") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("दुकान का पता (Address)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = city,
                onValueChange = { city = it },
                label = { Text("शहर (City)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("दुकान का समय (Working Hours)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = openTime,
                    onValueChange = { openTime = it },
                    label = { Text("खुलने का समय (Open)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = closeTime,
                    onValueChange = { closeTime = it },
                    label = { Text("बंद होने का समय (Close)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("सोशल मीडिया लिंक्स (Social Media)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = instagram,
                onValueChange = { instagram = it },
                label = { Text("Instagram Profile URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = youtube,
                onValueChange = { youtube = it },
                label = { Text("YouTube Channel URL") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = googleReview,
                onValueChange = { googleReview = it },
                label = { Text("Google Review Link") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    currentBusiness?.let { biz ->
                        val updated = biz.copy(
                            name = name.ifBlank { biz.name },
                            ownerName = ownerName.ifBlank { biz.ownerName },
                            phone = phone.ifBlank { biz.phone },
                            category = category.ifBlank { biz.category },
                            address = address.ifBlank { biz.address },
                            city = city.ifBlank { biz.city },
                            upiId = upiId.ifBlank { biz.upiId },
                            openTime = openTime,
                            closeTime = closeTime,
                            socialInstagram = instagram,
                            socialYoutube = youtube,
                            socialGoogleReview = googleReview
                        )
                        viewModel.updateBusinessProfile(updated)
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("सुरक्षित करें (Save Profile)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
