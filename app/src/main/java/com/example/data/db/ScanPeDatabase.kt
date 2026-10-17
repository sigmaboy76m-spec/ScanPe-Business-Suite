package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Business::class,
        Customer::class,
        Product::class,
        Bill::class,
        BillItem::class,
        KhataTransaction::class,
        RewardCode::class,
        RewardTier::class,
        RewardRedemption::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ScanPeDatabase : RoomDatabase() {

    abstract fun scanPeDao(): ScanPeDao

    companion object {
        @Volatile
        private var INSTANCE: ScanPeDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ScanPeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanPeDatabase::class.java,
                    "scanpe_database"
                )
                    .addCallback(ScanPeDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ScanPeDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.scanPeDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: ScanPeDao) {
            val biz1Id = "BIZ_SHARMA_KIRANA"
            val biz1 = Business(
                id = biz1Id,
                name = "Sharma Kirana & General Store",
                ownerName = "Ramesh Sharma",
                phone = "9876543210",
                category = "Kirana Store",
                address = "Shop 14, Main Market, Sector 18",
                city = "New Delhi",
                upiId = "sharmakirana@upi",
                openTime = "07:30 AM",
                closeTime = "10:30 PM",
                pointsPer100Rs = 1,
                socialInstagram = "https://instagram.com/sharmakirana",
                socialYoutube = "https://youtube.com/@sharmakirana",
                socialGoogleReview = "https://g.page/r/sharmakirana/review"
            )

            val biz2Id = "BIZ_CHAI_BITES"
            val biz2 = Business(
                id = biz2Id,
                name = "Chai & Bites Cafe",
                ownerName = "Priya Kapoor",
                phone = "9811223344",
                category = "Cafe & Snacks",
                address = "Corner Hub, Near Metro Gate 2",
                city = "New Delhi",
                upiId = "chaibites@upi",
                openTime = "09:00 AM",
                closeTime = "11:00 PM",
                pointsPer100Rs = 2
            )

            dao.insertBusiness(biz1)
            dao.insertBusiness(biz2)

            // Seed Products for Kirana
            val products = listOf(
                Product("PROD_1", biz1Id, "बासमती चावल (Basmati Rice)", 120.0, "KG", "Grocery", 45),
                Product("PROD_2", biz1Id, "फार्च्यून सरसों तेल (Mustard Oil)", 160.0, "LTR", "Oils", 30),
                Product("PROD_3", biz1Id, "आशीर्वाद चक्की आटा (Wheat Flour 5kg)", 240.0, "PACK", "Grocery", 25),
                Product("PROD_4", biz1Id, "टाटा प्रीमियम चाय (Tata Tea 500g)", 210.0, "PACK", "Beverages", 40),
                Product("PROD_5", biz1Id, "अरहर दाल (Toor Dal)", 140.0, "KG", "Pulses", 35),
                Product("PROD_6", biz1Id, "सर्फ एक्सेल डिटर्जेंट (Surf Excel 1kg)", 135.0, "PACK", "Household", 28),
                Product("PROD_7", biz1Id, "डेटॉल साबुन (Dettol Soap 3in1)", 95.0, "PACK", "Personal Care", 50),
                Product("PROD_8", biz1Id, "अमूल मक्खन (Amul Butter 100g)", 58.0, "PCS", "Dairy", 18),
                Product("PROD_9", biz1Id, "मैगी नूडल्स (Maggi 4-Pack)", 56.0, "PACK", "Snacks", 60),
                Product("PROD_10", biz1Id, "पारले-जी बिस्किट (Parle-G Family)", 35.0, "PACK", "Snacks", 80)
            )
            dao.insertProducts(products)

            // Seed Customers
            val cust1 = Customer(
                id = "CUST_9898012345",
                businessId = biz1Id,
                name = "Rahul Verma",
                phone = "9898012345",
                pinHash = "1234",
                loyaltyPoints = 45,
                totalSpent = 4850.0,
                totalOutstandingUdhaar = 700.0
            )

            val cust2 = Customer(
                id = "CUST_9871122334",
                businessId = biz1Id,
                name = "Pooja Singh",
                phone = "9871122334",
                pinHash = "1234",
                loyaltyPoints = 85,
                totalSpent = 8500.0,
                totalOutstandingUdhaar = 0.0
            )

            val cust3 = Customer(
                id = "CUST_9810987654",
                businessId = biz1Id,
                name = "Amit Kumar",
                phone = "9810987654",
                pinHash = "1234",
                loyaltyPoints = 20,
                totalSpent = 2300.0,
                totalOutstandingUdhaar = 1250.0
            )

            dao.insertCustomer(cust1)
            dao.insertCustomer(cust2)
            dao.insertCustomer(cust3)

            // Seed Khata Transactions
            val now = System.currentTimeMillis()
            val dayMs = 86400000L
            val trans1 = KhataTransaction(
                id = "TX_1001",
                businessId = biz1Id,
                customerId = cust1.id,
                customerName = cust1.name,
                customerPhone = cust1.phone,
                type = "CREDIT",
                amount = 1000.0,
                balanceAfter = 1000.0,
                description = "Monthly Grocery Bill #SP-801",
                createdAt = now - (3 * dayMs)
            )
            val trans2 = KhataTransaction(
                id = "TX_1002",
                businessId = biz1Id,
                customerId = cust1.id,
                customerName = cust1.name,
                customerPhone = cust1.phone,
                type = "PAYMENT",
                amount = 300.0,
                balanceAfter = 700.0,
                description = "UPI Payment via ScanPe QR",
                createdAt = now - (1 * dayMs)
            )
            val trans3 = KhataTransaction(
                id = "TX_1003",
                businessId = biz1Id,
                customerId = cust3.id,
                customerName = cust3.name,
                customerPhone = cust3.phone,
                type = "CREDIT",
                amount = 1250.0,
                balanceAfter = 1250.0,
                description = "Oil & Ration Udhaar",
                createdAt = now - (2 * dayMs)
            )
            dao.insertKhataTransaction(trans1)
            dao.insertKhataTransaction(trans2)
            dao.insertKhataTransaction(trans3)

            // Seed Sample Bills
            val bill1 = Bill(
                id = "BILL_SP_801",
                billNumber = "SP-2026-801",
                businessId = biz1Id,
                customerId = cust1.id,
                customerName = cust1.name,
                customerPhone = cust1.phone,
                subtotal = 1050.0,
                discount = 50.0,
                tax = 0.0,
                finalAmount = 1000.0,
                paidAmount = 0.0,
                creditAmount = 1000.0,
                paymentMethod = "UDHAAR",
                pointsEarned = 10,
                rewardCode = "A7K92P",
                createdAt = now - (3 * dayMs)
            )
            val bill2 = Bill(
                id = "BILL_SP_802",
                billNumber = "SP-2026-802",
                businessId = biz1Id,
                customerId = cust2.id,
                customerName = cust2.name,
                customerPhone = cust2.phone,
                subtotal = 540.0,
                discount = 40.0,
                tax = 0.0,
                finalAmount = 500.0,
                paidAmount = 500.0,
                creditAmount = 0.0,
                paymentMethod = "UPI",
                pointsEarned = 5,
                rewardCode = "R4M88K",
                createdAt = now - (1 * dayMs)
            )
            dao.insertBill(bill1)
            dao.insertBill(bill2)

            // Seed Reward Codes
            val rCode1 = RewardCode(
                code = "A7K92P",
                businessId = biz1Id,
                billId = bill1.id,
                points = 10,
                billAmount = 1000.0,
                status = "UNUSED",
                createdAt = now - (3 * dayMs)
            )
            val rCode2 = RewardCode(
                code = "R4M88K",
                businessId = biz1Id,
                billId = bill2.id,
                points = 5,
                billAmount = 500.0,
                status = "USED",
                createdAt = now - (1 * dayMs),
                redeemedAt = now - (1 * dayMs),
                redeemedByCustomerPhone = cust2.phone
            )
            val rCode3 = RewardCode(
                code = "BONUS9",
                businessId = biz1Id,
                billId = null,
                points = 25,
                billAmount = 1500.0,
                status = "UNUSED",
                createdAt = now
            )
            dao.insertRewardCode(rCode1)
            dao.insertRewardCode(rCode2)
            dao.insertRewardCode(rCode3)

            // Seed Reward Tiers
            val tiers = listOf(
                RewardTier(
                    id = "TIER_1",
                    businessId = biz1Id,
                    title = "₹50 Discount Voucher",
                    pointsRequired = 10,
                    discountAmount = 50.0,
                    description = "Get flat ₹50 OFF on your next purchase over ₹300"
                ),
                RewardTier(
                    id = "TIER_2",
                    businessId = biz1Id,
                    title = "Free Maggi Family Pack",
                    pointsRequired = 20,
                    discountAmount = 60.0,
                    description = "Claim a free 4-pack Maggi noodles"
                ),
                RewardTier(
                    id = "TIER_3",
                    businessId = biz1Id,
                    title = "₹200 Mega Saving Voucher",
                    pointsRequired = 50,
                    discountAmount = 200.0,
                    description = "Get flat ₹200 OFF on billing above ₹1,000"
                )
            )
            dao.insertRewardTiers(tiers)
        }
    }
}
