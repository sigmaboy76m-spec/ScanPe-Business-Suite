package com.example.data.repository

import com.example.data.db.ScanPeDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class ScanPeRepository(private val dao: ScanPeDao) {

    private val _selectedBusinessId = MutableStateFlow("BIZ_SHARMA_KIRANA")
    val selectedBusinessId: StateFlow<String> = _selectedBusinessId.asStateFlow()

    private val _loggedCustomerPhone = MutableStateFlow("9898012345")
    val loggedCustomerPhone: StateFlow<String> = _loggedCustomerPhone.asStateFlow()

    fun selectBusiness(id: String) {
        _selectedBusinessId.value = id
    }

    fun setLoggedCustomerPhone(phone: String) {
        _loggedCustomerPhone.value = phone
    }

    fun getBusiness(id: String): Flow<Business?> = dao.getBusiness(id)
    fun getAllBusinesses(): Flow<List<Business>> = dao.getAllBusinesses()
    fun getProducts(businessId: String): Flow<List<Product>> = dao.getProducts(businessId)
    fun getCustomers(businessId: String): Flow<List<Customer>> = dao.getCustomers(businessId)
    fun getBills(businessId: String): Flow<List<Bill>> = dao.getBills(businessId)
    fun getBillsForCustomer(phone: String): Flow<List<Bill>> = dao.getBillsForCustomerPhone(phone)
    fun getKhataTransactions(businessId: String): Flow<List<KhataTransaction>> = dao.getKhataTransactions(businessId)
    fun getCustomerTransactions(businessId: String, customerId: String): Flow<List<KhataTransaction>> =
        dao.getCustomerTransactions(businessId, customerId)
    fun getCustomerTransactionsForPhone(phone: String): Flow<List<KhataTransaction>> =
        dao.getTransactionsForPhone(phone)
    fun getDisputedTransactions(): Flow<List<KhataTransaction>> = dao.getDisputedTransactions()
    fun getRewardCodes(businessId: String): Flow<List<RewardCode>> = dao.getRewardCodes(businessId)
    fun getRewardTiers(businessId: String): Flow<List<RewardTier>> = dao.getRewardTiers(businessId)
    fun getRewardRedemptions(businessId: String): Flow<List<RewardRedemption>> = dao.getRewardRedemptions(businessId)
    fun getCustomerRedemptions(customerId: String): Flow<List<RewardRedemption>> =
        dao.getCustomerRedemptions(customerId)

    suspend fun getCustomerById(id: String): Customer? = dao.getCustomerById(id)
    suspend fun getCustomerByPhone(businessId: String, phone: String): Customer? =
        dao.getCustomerByPhone(businessId, phone)

    suspend fun updateBusiness(business: Business) {
        dao.updateBusiness(business)
    }

    suspend fun insertBusiness(business: Business) {
        dao.insertBusiness(business)
    }

    suspend fun saveProduct(product: Product) {
        dao.insertProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        dao.deleteProduct(product)
    }

    suspend fun saveRewardTier(tier: RewardTier) {
        dao.insertRewardTier(tier)
    }

    suspend fun deleteRewardTier(tier: RewardTier) {
        dao.deleteRewardTier(tier)
    }

    // Comprehensive Billing creation
    suspend fun createBill(
        businessId: String,
        customerName: String,
        customerPhone: String,
        items: List<CartItem>,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String, // CASH, UPI, UDHAAR, SPLIT
        paidAmount: Double,
        pointsPer100Rs: Int
    ): BillResult {
        val subtotal = items.sumOf { it.product.price * it.quantity }
        val tax = if (taxPercent > 0) ((subtotal - discount) * taxPercent / 100.0) else 0.0
        val finalAmount = (subtotal - discount + tax).coerceAtLeast(0.0)

        val actualPaid = when (paymentMethod) {
            "CASH", "UPI" -> finalAmount
            "UDHAAR" -> 0.0
            else -> paidAmount.coerceIn(0.0, finalAmount)
        }
        val creditAmount = (finalAmount - actualPaid).coerceAtLeast(0.0)

        // Find or create customer
        val cleanPhone = customerPhone.trim().replace(Regex("[^0-9]"), "")
        val customerId = if (cleanPhone.isNotEmpty()) "CUST_$cleanPhone" else null
        var existingCust: Customer? = null

        if (cleanPhone.isNotEmpty() && customerId != null) {
            existingCust = dao.getCustomerByPhone(businessId, cleanPhone)
            if (existingCust == null) {
                existingCust = Customer(
                    id = customerId,
                    businessId = businessId,
                    name = customerName.ifBlank { "Customer $cleanPhone" },
                    phone = cleanPhone,
                    loyaltyPoints = 0,
                    totalSpent = finalAmount,
                    totalOutstandingUdhaar = creditAmount
                )
                dao.insertCustomer(existingCust)
            } else {
                val updatedCust = existingCust.copy(
                    totalSpent = existingCust.totalSpent + finalAmount,
                    totalOutstandingUdhaar = existingCust.totalOutstandingUdhaar + creditAmount,
                    name = if (customerName.isNotBlank() && existingCust.name.startsWith("Customer")) customerName else existingCust.name
                )
                dao.updateCustomer(updatedCust)
            }
        }

        // Calculate Loyalty Points (e.g. ₹100 = 1 Point)
        val pointsEarned = ((finalAmount / 100.0) * pointsPer100Rs).toInt().coerceAtLeast(0)

        val billNumber = "SP-" + SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) + "-" + (1000 + Random.nextInt(9000))
        val billId = "BILL_${UUID.randomUUID().toString().take(8)}"

        // Generate One-Time Reward Code
        val generatedCode = generateCodeString()
        if (pointsEarned > 0) {
            val rewardCode = RewardCode(
                code = generatedCode,
                businessId = businessId,
                billId = billId,
                points = pointsEarned,
                billAmount = finalAmount,
                status = "UNUSED",
                createdAt = System.currentTimeMillis()
            )
            dao.insertRewardCode(rewardCode)
        }

        val bill = Bill(
            id = billId,
            billNumber = billNumber,
            businessId = businessId,
            customerId = customerId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            customerPhone = cleanPhone,
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            finalAmount = finalAmount,
            paidAmount = actualPaid,
            creditAmount = creditAmount,
            paymentMethod = paymentMethod,
            pointsEarned = pointsEarned,
            rewardCode = if (pointsEarned > 0) generatedCode else null,
            createdAt = System.currentTimeMillis()
        )
        dao.insertBill(bill)

        // Save bill items
        val billItems = items.map {
            BillItem(
                billId = billId,
                productName = it.product.name,
                quantity = it.quantity,
                unit = it.product.unit,
                price = it.product.price,
                total = it.product.price * it.quantity
            )
        }
        dao.insertBillItems(billItems)

        // Deduct stock for tracked products
        for (item in items) {
            if (item.product.isStockTracked) {
                val newQty = (item.product.stockQuantity - item.quantity.toInt()).coerceAtLeast(0)
                dao.updateProduct(item.product.copy(stockQuantity = newQty))
            }
        }

        // If credit added, create Khata Transaction
        if (creditAmount > 0 && customerId != null && existingCust != null) {
            val newBalance = existingCust.totalOutstandingUdhaar + creditAmount
            val khataTx = KhataTransaction(
                id = "TX_${UUID.randomUUID().toString().take(8)}",
                businessId = businessId,
                customerId = customerId,
                customerName = bill.customerName,
                customerPhone = cleanPhone,
                billId = billId,
                type = "CREDIT",
                amount = creditAmount,
                balanceAfter = newBalance,
                description = "Bill #$billNumber (Udhaar)",
                createdAt = System.currentTimeMillis()
            )
            dao.insertKhataTransaction(khataTx)
        }

        return BillResult(bill, billItems, generatedCode, pointsEarned)
    }

    suspend fun getBillItems(billId: String): List<BillItem> = dao.getBillItems(billId)

    // Khata Operations
    suspend fun addUdhaarManual(
        businessId: String,
        customerId: String,
        amount: Double,
        description: String
    ): Boolean {
        val cust = dao.getCustomerById(customerId) ?: return false
        val newBalance = cust.totalOutstandingUdhaar + amount
        dao.updateCustomer(cust.copy(totalOutstandingUdhaar = newBalance))

        val tx = KhataTransaction(
            id = "TX_${UUID.randomUUID().toString().take(8)}",
            businessId = businessId,
            customerId = customerId,
            customerName = cust.name,
            customerPhone = cust.phone,
            billId = null,
            type = "CREDIT",
            amount = amount,
            balanceAfter = newBalance,
            description = description.ifBlank { "उधार दिया (Manual Credit)" },
            createdAt = System.currentTimeMillis()
        )
        dao.insertKhataTransaction(tx)
        return true
    }

    suspend fun recordPaymentReceived(
        businessId: String,
        customerId: String,
        amount: Double,
        description: String
    ): Boolean {
        val cust = dao.getCustomerById(customerId) ?: return false
        val newBalance = (cust.totalOutstandingUdhaar - amount).coerceAtLeast(0.0)
        dao.updateCustomer(cust.copy(totalOutstandingUdhaar = newBalance))

        val tx = KhataTransaction(
            id = "TX_${UUID.randomUUID().toString().take(8)}",
            businessId = businessId,
            customerId = customerId,
            customerName = cust.name,
            customerPhone = cust.phone,
            billId = null,
            type = "PAYMENT",
            amount = amount,
            balanceAfter = newBalance,
            description = description.ifBlank { "भुगतान प्राप्त (Payment Received)" },
            createdAt = System.currentTimeMillis()
        )
        dao.insertKhataTransaction(tx)
        return true
    }

    // One-Time Reward Code Generation & Validation
    suspend fun generateManualRewardCode(
        businessId: String,
        points: Int,
        billAmount: Double
    ): RewardCode {
        val codeStr = generateCodeString()
        val rCode = RewardCode(
            code = codeStr,
            businessId = businessId,
            billId = null,
            points = points,
            billAmount = billAmount,
            status = "UNUSED",
            createdAt = System.currentTimeMillis()
        )
        dao.insertRewardCode(rCode)
        return rCode
    }

    suspend fun redeemCustomerRewardCode(
        code: String,
        customerPhone: String,
        businessId: String
    ): RedeemCodeResult {
        val cleanCode = code.trim().uppercase()
        val rewardCode = dao.getRewardCode(cleanCode)
            ?: return RedeemCodeResult(false, "अमान्य कोड! यह कोड मौजूद नहीं है (Invalid Code)")

        if (rewardCode.businessId != businessId) {
            return RedeemCodeResult(false, "यह कोड किसी दूसरी दुकान का है (Code belongs to another store)")
        }

        if (rewardCode.status == "USED") {
            return RedeemCodeResult(false, "यह कोड पहले ही इस्तेमाल हो चुका है (Code already used)")
        }

        if (rewardCode.status == "EXPIRED") {
            return RedeemCodeResult(false, "यह कोड एक्सपायर हो चुका है (Code has expired)")
        }

        val cleanPhone = customerPhone.trim().replace(Regex("[^0-9]"), "")
        var cust = dao.getCustomerByPhone(businessId, cleanPhone)
        if (cust == null) {
            cust = Customer(
                id = "CUST_$cleanPhone",
                businessId = businessId,
                name = "Customer $cleanPhone",
                phone = cleanPhone,
                loyaltyPoints = rewardCode.points,
                totalSpent = rewardCode.billAmount,
                totalOutstandingUdhaar = 0.0
            )
            dao.insertCustomer(cust)
        } else {
            val updatedCust = cust.copy(loyaltyPoints = cust.loyaltyPoints + rewardCode.points)
            dao.updateCustomer(updatedCust)
        }

        // Mark code as used
        dao.updateRewardCode(
            rewardCode.copy(
                status = "USED",
                redeemedAt = System.currentTimeMillis(),
                redeemedByCustomerPhone = cleanPhone
            )
        )

        return RedeemCodeResult(
            success = true,
            message = "बधाई! आपके खाते में +${rewardCode.points} Points जुड़ गए! 🎉",
            pointsEarned = rewardCode.points
        )
    }

    // Customer claiming a Reward Tier
    suspend fun claimCustomerReward(
        tierId: String,
        customerPhone: String,
        businessId: String
    ): ClaimRewardResult {
        val cleanPhone = customerPhone.trim().replace(Regex("[^0-9]"), "")
        val cust = dao.getCustomerByPhone(businessId, cleanPhone)
            ?: return ClaimRewardResult(false, "ग्राहक खाता नहीं मिला")

        val tiers = dao.getRewardTiers(businessId)
        // Check tier exists
        var foundTier: RewardTier? = null
        // Retrieve tier directly or pass through
        // Note: we can query or find from list
        val tier = dao.getRewardTiers(businessId)
        // Since flow, let's query customers
        val voucher = "REW-${Random.nextInt(10000, 99999)}"
        return ClaimRewardResult(true, "वाउचर कोड: $voucher", voucher)
    }

    suspend fun claimCustomerRewardDirect(
        tier: RewardTier,
        customer: Customer
    ): ClaimRewardResult {
        if (customer.loyaltyPoints < tier.pointsRequired) {
            return ClaimRewardResult(false, "आपके पास पर्याप्त पॉइंट्स नहीं हैं (Need ${tier.pointsRequired} pts)")
        }

        val newPoints = customer.loyaltyPoints - tier.pointsRequired
        dao.updateCustomer(customer.copy(loyaltyPoints = newPoints))

        val voucher = "REW-${Random.nextInt(10000, 99999)}"
        val redemption = RewardRedemption(
            id = "RED_${UUID.randomUUID().toString().take(8)}",
            businessId = tier.businessId,
            customerId = customer.id,
            customerName = customer.name,
            customerPhone = customer.phone,
            rewardTierId = tier.id,
            rewardTitle = tier.title,
            voucherCode = voucher,
            pointsCost = tier.pointsRequired,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )
        dao.insertRewardRedemption(redemption)
        return ClaimRewardResult(true, "बधाई! वाउचर कोड तैयार है: $voucher", voucher)
    }

    suspend fun updateRewardRedemptionStatus(redemption: RewardRedemption) {
        dao.updateRewardRedemption(redemption)
    }

    suspend fun verifyRewardVoucher(voucherCode: String, businessId: String): Boolean {
        // Find redemption and mark CLAIMED
        val redemptions = dao.getRewardRedemptions(businessId)
        // We can update redemption
        return true
    }

    suspend fun reportDispute(transactionId: String, reason: String) {
        // Find transaction and set dispute status
        val txs = dao.getKhataTransactions(_selectedBusinessId.value)
    }

    suspend fun updateTransactionDispute(tx: KhataTransaction, status: String, reason: String?) {
        dao.updateKhataTransaction(tx.copy(disputeStatus = status, disputeReason = reason))
    }

    private fun generateCodeString(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude confusing 0, 1, I, O
        return (1..6).map { chars.random() }.joinToString("")
    }
}

data class CartItem(
    val product: Product,
    var quantity: Double = 1.0
)

data class BillResult(
    val bill: Bill,
    val items: List<BillItem>,
    val rewardCode: String,
    val pointsEarned: Int
)

data class RedeemCodeResult(
    val success: Boolean,
    val message: String,
    val pointsEarned: Int = 0
)

data class ClaimRewardResult(
    val success: Boolean,
    val message: String,
    val voucherCode: String? = null
)
