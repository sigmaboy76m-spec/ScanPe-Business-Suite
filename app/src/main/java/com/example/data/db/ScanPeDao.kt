package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanPeDao {

    // Businesses
    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusiness(id: String): Flow<Business?>

    @Query("SELECT * FROM businesses")
    fun getAllBusinesses(): Flow<List<Business>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business)

    @Update
    suspend fun updateBusiness(business: Business)

    // Customers
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name ASC")
    fun getCustomers(businessId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND phone = :phone LIMIT 1")
    suspend fun getCustomerByPhone(businessId: String, phone: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    // Products
    @Query("SELECT * FROM products WHERE businessId = :businessId ORDER BY name ASC")
    fun getProducts(businessId: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<Product>)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Bills
    @Query("SELECT * FROM bills WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getBills(businessId: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE customerPhone = :phone ORDER BY createdAt DESC")
    fun getBillsForCustomerPhone(phone: String): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    suspend fun getBillById(billId: String): Bill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItem>)

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getBillItems(billId: String): List<BillItem>

    // Khata Transactions
    @Query("SELECT * FROM khata_transactions WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getKhataTransactions(businessId: String): Flow<List<KhataTransaction>>

    @Query("SELECT * FROM khata_transactions WHERE businessId = :businessId AND customerId = :customerId ORDER BY createdAt DESC")
    fun getCustomerTransactions(businessId: String, customerId: String): Flow<List<KhataTransaction>>

    @Query("SELECT * FROM khata_transactions WHERE customerPhone = :phone ORDER BY createdAt DESC")
    fun getTransactionsForPhone(phone: String): Flow<List<KhataTransaction>>

    @Query("SELECT * FROM khata_transactions WHERE disputeStatus != 'NONE' ORDER BY createdAt DESC")
    fun getDisputedTransactions(): Flow<List<KhataTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKhataTransaction(transaction: KhataTransaction)

    @Update
    suspend fun updateKhataTransaction(transaction: KhataTransaction)

    // Reward Codes
    @Query("SELECT * FROM reward_codes WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getRewardCodes(businessId: String): Flow<List<RewardCode>>

    @Query("SELECT * FROM reward_codes WHERE code = :code LIMIT 1")
    suspend fun getRewardCode(code: String): RewardCode?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardCode(rewardCode: RewardCode)

    @Update
    suspend fun updateRewardCode(rewardCode: RewardCode)

    // Reward Tiers
    @Query("SELECT * FROM reward_tiers WHERE businessId = :businessId ORDER BY pointsRequired ASC")
    fun getRewardTiers(businessId: String): Flow<List<RewardTier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardTier(tier: RewardTier)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardTiers(tiers: List<RewardTier>)

    @Delete
    suspend fun deleteRewardTier(tier: RewardTier)

    // Reward Redemptions
    @Query("SELECT * FROM reward_redemptions WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getRewardRedemptions(businessId: String): Flow<List<RewardRedemption>>

    @Query("SELECT * FROM reward_redemptions WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getCustomerRedemptions(customerId: String): Flow<List<RewardRedemption>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardRedemption(redemption: RewardRedemption)

    @Update
    suspend fun updateRewardRedemption(redemption: RewardRedemption)
}
