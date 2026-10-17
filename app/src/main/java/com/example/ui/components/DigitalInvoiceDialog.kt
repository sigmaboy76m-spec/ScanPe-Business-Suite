package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Bill
import com.example.data.model.BillItem
import com.example.data.model.Business
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DigitalInvoiceDialog(
    bill: Bill,
    items: List<BillItem>,
    business: Business?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(bill.createdAt))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ScanPeEmerald.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Receipt,
                                contentDescription = null,
                                tint = ScanPeEmerald,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "डिजिटल रसीद (Digital Invoice)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Bill Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Invoice Card Frame
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Business Branding
                            Text(
                                text = business?.name ?: "ScanPe Verified Merchant",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ScanPeNavy
                            )
                            Text(
                                text = "${business?.category ?: "Retail"} • ${business?.address ?: ""}, ${business?.city ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ph: ${business?.phone ?: ""} | UPI: ${business?.upiId ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Bill Metadata
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("बिल नंबर (Bill No)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bill.billNumber, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("दिनांक व समय (Date)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dateStr, style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Customer Details
                            if (bill.customerName.isNotBlank() || bill.customerPhone.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("ग्राहक (Customer)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${bill.customerName} (${bill.customerPhone})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("भुगतान प्रकार (Method)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Surface(
                                            color = when (bill.paymentMethod) {
                                                "UDHAAR" -> ScanPeRedCredit.copy(alpha = 0.15f)
                                                "UPI" -> ScanPeBlueUpi.copy(alpha = 0.15f)
                                                else -> ScanPeEmerald.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = bill.paymentMethod,
                                                color = when (bill.paymentMethod) {
                                                    "UDHAAR" -> ScanPeRedCredit
                                                    "UPI" -> ScanPeBlueUpi
                                                    else -> ScanPeEmerald
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Items Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(vertical = 6.dp, horizontal = 8.dp)
                            ) {
                                Text("सामान (Item)", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("मात्रा (Qty)", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("दर (Rate)", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text("कुल (Total)", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Item Rows
                            items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.productName, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)
                                    Text("${item.quantity} ${item.unit}", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
                                    Text("₹${item.price.toInt()}", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                                    Text("₹${item.total.toInt()}", modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Calculation Breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                                Text("₹${bill.subtotal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                            }

                            if (bill.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Discount (छूट)", style = MaterialTheme.typography.bodyMedium, color = ScanPeEmerald)
                                    Text("-₹${bill.discount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = ScanPeEmerald, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (bill.tax > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tax / GST", style = MaterialTheme.typography.bodyMedium)
                                    Text("+₹${bill.tax.toInt()}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Grand Total
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ScanPeEmerald.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("कुल देय (Total Bill)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("₹${bill.finalAmount.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ScanPeEmerald)
                            }

                            // Payment & Udhaar Split
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("भुगतान किया (Paid)", style = MaterialTheme.typography.bodyMedium)
                                Text("₹${bill.paidAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ScanPeGreenPaid)
                            }

                            if (bill.creditAmount > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("बाकी उधार (Udhaar Balance)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ScanPeRedCredit)
                                    Text("₹${bill.creditAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ScanPeRedCredit)
                                }
                            }

                            // Loyalty Points & Reward Code Box
                            if (bill.pointsEarned > 0 && bill.rewardCode != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ScanPeGold.copy(alpha = 0.15f)),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(ScanPeGold)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = ScanPeSecondary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Loyalty Points Earned: +${bill.pointsEarned} Points",
                                                fontWeight = FontWeight.Bold,
                                                color = ScanPeSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ग्राहक का One-Time Reward Code:",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(6.dp),
                                            border = CardDefaults.outlinedCardBorder(),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = bill.rewardCode,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 4.sp,
                                                color = ScanPeNavy,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                            )
                                        }
                                        Text(
                                            text = "ग्राहक scanpe.in/shop/${bill.businessId} पर यह कोड डालकर Points Claim कर सकता है।",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareInvoiceOnWhatsApp(context, bill, items, business)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            shareInvoiceText(context, bill, items, business)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = ScanPeEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

private val ScanPeSecondary = Color(0xFFB45309)

private fun generateInvoiceText(bill: Bill, items: List<BillItem>, business: Business?): String {
    val itemsSummary = items.joinToString("\n") { "• ${it.productName} x ${it.quantity} ${it.unit} = ₹${it.total.toInt()}" }
    return buildString {
        appendLine("🧾 *ScanPe Digital Invoice*")
        appendLine("🏪 *${business?.name ?: "Merchant"}*")
        appendLine("📍 ${business?.address ?: ""}")
        appendLine("------------------------------")
        appendLine("Bill No: ${bill.billNumber}")
        appendLine("Customer: ${bill.customerName} (${bill.customerPhone})")
        appendLine("Payment: ${bill.paymentMethod}")
        appendLine("------------------------------")
        appendLine(itemsSummary)
        appendLine("------------------------------")
        appendLine("Subtotal: ₹${bill.subtotal.toInt()}")
        if (bill.discount > 0) appendLine("Discount: -₹${bill.discount.toInt()}")
        appendLine("*Grand Total: ₹${bill.finalAmount.toInt()}*")
        appendLine("Paid: ₹${bill.paidAmount.toInt()}")
        if (bill.creditAmount > 0) {
            appendLine("⚠️ *Outstanding Udhaar: ₹${bill.creditAmount.toInt()}*")
        }
        if (bill.pointsEarned > 0 && bill.rewardCode != null) {
            appendLine("🎁 Loyalty Points Earned: +${bill.pointsEarned}")
            appendLine("🔑 One-Time Reward Code: *${bill.rewardCode}*")
            appendLine("Claim points at: https://scanpe.in/shop/${bill.businessId}")
        }
        appendLine("\nधन्यवाद! दोबारा पधारें! 🙏")
    }
}

private fun shareInvoiceOnWhatsApp(context: Context, bill: Bill, items: List<BillItem>, business: Business?) {
    val text = generateInvoiceText(bill, items, business)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage("com.whatsapp")
    }
    try {
        context.startActivity(sendIntent)
    } catch (e: Exception) {
        // Fallback to general share chooser
        shareInvoiceText(context, bill, items, business)
    }
}

private fun shareInvoiceText(context: Context, bill: Bill, items: List<BillItem>, business: Business?) {
    val text = generateInvoiceText(bill, items, business)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Invoice #${bill.billNumber}")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Invoice"))
}
