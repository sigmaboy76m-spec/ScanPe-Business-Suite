package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "businesses")
data class Business(
    @PrimaryKey
    val id: String,
    val name: String,
    val ownerName: String,
    val phone: String,
    val category: String, // Kirana, Cafe, Restaurant, Salon, Retail, Medical
    val address: String,
    val city: String,
    val upiId: String,
    val openTime: String = "08:00 AM",
    val closeTime: String = "10:00 PM",
    val openDays: String = "All Days",
    val pointsPer100Rs: Int = 1, // e.g. 1 point per 100 Rs
    val socialInstagram: String = "https://instagram.com/scanpe_shop",
    val socialYoutube: String = "https://youtube.com/@scanpe_shop",
    val socialGoogleReview: String = "https://g.page/r/scanpe_shop",
    val subscriptionPlan: String = "1 Month Free Trial",
    val isSubscriptionActive: Boolean = true
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val id: String, // e.g. "CUST_9876543210"
    val businessId: String,
    val name: String,
    val phone: String,
    val pinHash: String = "1234", // Default 4-digit PIN for customer portal
    val loyaltyPoints: Int = 0,
    val totalSpent: Double = 0.0,
    val totalOutstandingUdhaar: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey
    val id: String,
    val businessId: String,
    val name: String,
    val price: Double,
    val unit: String = "PCS", // KG, LTR, PCS, PACK, MTR
    val category: String = "General",
    val stockQuantity: Int = 50,
    val isStockTracked: Boolean = true,
    val lowStockThreshold: Int = 10
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey
    val id: String,
    val billNumber: String,
    val businessId: String,
    val customerId: String?,
    val customerName: String,
    val customerPhone: String,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val finalAmount: Double,
    val paidAmount: Double,
    val creditAmount: Double, // Udhaar part
    val paymentMethod: String, // CASH, UPI, UDHAAR, SPLIT
    val pointsEarned: Int,
    val rewardCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bill_items")
data class BillItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: String,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val total: Double
)

@Entity(tableName = "khata_transactions")
data class KhataTransaction(
    @PrimaryKey
    val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val billId: String? = null,
    val type: String, // CREDIT (उधार दिया), PAYMENT (रुपये मिले), ADJUSTMENT
    val amount: Double,
    val balanceAfter: Double,
    val description: String,
    val disputeStatus: String = "NONE", // NONE, REPORTED, RESOLVED
    val disputeReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reward_codes")
data class RewardCode(
    @PrimaryKey
    val code: String, // e.g. "A7K92P"
    val businessId: String,
    val billId: String?,
    val points: Int,
    val billAmount: Double,
    val status: String = "UNUSED", // UNUSED, USED, EXPIRED
    val createdAt: Long = System.currentTimeMillis(),
    val redeemedAt: Long? = null,
    val redeemedByCustomerPhone: String? = null
)

@Entity(tableName = "reward_tiers")
data class RewardTier(
    @PrimaryKey
    val id: String,
    val businessId: String,
    val title: String,
    val pointsRequired: Int,
    val discountAmount: Double,
    val description: String
)

@Entity(tableName = "reward_redemptions")
data class RewardRedemption(
    @PrimaryKey
    val id: String,
    val businessId: String,
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val rewardTierId: String,
    val rewardTitle: String,
    val voucherCode: String,
    val pointsCost: Int,
    val status: String = "PENDING", // PENDING, CLAIMED
    val createdAt: Long = System.currentTimeMillis()
)
