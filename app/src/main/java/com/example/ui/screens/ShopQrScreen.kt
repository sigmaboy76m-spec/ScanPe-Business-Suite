package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Business
import com.example.ui.ScanPeViewModel
import com.example.ui.components.QrCodeCanvas
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopQrScreen(
    viewModel: ScanPeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentBusiness by viewModel.currentBusiness.collectAsState()
    var selectedQrType by remember { mutableStateOf(0) } // 0: ScanPe Portal QR, 1: Direct UPI QR

    val shopUrl = "https://scanpe.in/shop/${currentBusiness?.id ?: "shop"}"
    val upiUri = "upi://pay?pa=${currentBusiness?.upiId ?: "shop@upi"}&pn=${(currentBusiness?.name ?: "Shop").replace(" ", "%20")}&cu=INR"

    val activePayload = if (selectedQrType == 0) shopUrl else upiUri

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("दुकान का QR कोड (Shop QR)", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Switch QR Type
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                SegmentedButton(
                    selected = selectedQrType == 0,
                    onClick = { selectedQrType = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("ScanPe Loyalty QR")
                }
                SegmentedButton(
                    selected = selectedQrType == 1,
                    onClick = { selectedQrType = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Direct UPI QR")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Branded Counter Poster Frame
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(2.dp, ScanPeEmerald.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ScanPeNavy, ScanPeNavySurface)
                                )
                            )
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = ScanPeEmerald,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentBusiness?.name ?: "ScanPe Store",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${currentBusiness?.category ?: "Retail"} • ${currentBusiness?.address ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = if (selectedQrType == 0) "Scan to View Bills, Udhaar & Claim Points" else "Scan to Pay via any UPI App (GPay, PhonePe, Paytm)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ScanPeEmerald,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code
                    QrCodeCanvas(
                        payload = activePayload,
                        size = 230.dp,
                        qrColor = ScanPeNavy
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = if (selectedQrType == 0) shopUrl else "UPI ID: ${currentBusiness?.upiId ?: "shop@upi"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer branding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScanPeEmerald.copy(alpha = 0.1f))
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = ScanPeEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Powered by ScanPe Digital Business",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ScanPeEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        shareShopPoster(context, currentBusiness, activePayload)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QR शेयर करें (Share)")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.showMessage("दुकान काउंटर पोस्टर प्रिंट के लिए तैयार है! 🖨️")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("प्रिंट पोस्टर (Print)")
                }
            }
        }
    }
}

private fun shareShopPoster(context: Context, business: Business?, link: String) {
    val text = "🏪 *${business?.name ?: "Shop"}* पर पधारने के लिए धन्यवाद!\n\nहमारे ScanPe डिजिटल पेज पर अपने बिल, उधार हिसाब व लॉयल्टी पॉइंट्स देखने के लिए यह लिंक खोलें:\n$link\n\nScanPe — डिजिटल भारत, डिजिटल व्यापार! 🚀"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Shop QR"))
}
