package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ScanPeDatabase
import com.example.data.model.*
import com.example.data.repository.CartItem
import com.example.data.repository.ScanPeRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppMode {
    BUSINESS,
    CUSTOMER,
    ADMIN
}

enum class BusinessTab {
    DASHBOARD,
    BILLING,
    KHATA,
    PRODUCTS,
    LOYALTY
}

class ScanPeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ScanPeDatabase.getDatabase(application, viewModelScope)
    val repository = ScanPeRepository(db.scanPeDao())

    // App Navigation State
    private val _appMode = MutableStateFlow(AppMode.BUSINESS)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _businessTab = MutableStateFlow(BusinessTab.DASHBOARD)
    val businessTab: StateFlow<BusinessTab> = _businessTab.asStateFlow()

    // Business & Customer State
    val selectedBusinessId = repository.selectedBusinessId
    val loggedCustomerPhone = repository.loggedCustomerPhone

    val allBusinesses = repository.getAllBusinesses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentBusiness = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getBusiness(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val products = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getProducts(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getCustomers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getBills(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val khataTransactions = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getKhataTransactions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewardCodes = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getRewardCodes(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewardTiers = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getRewardTiers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewardRedemptions = repository.selectedBusinessId.flatMapLatest { id ->
        repository.getRewardRedemptions(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disputes = repository.getDisputedTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Customer View Specific State
    val customerBills = repository.loggedCustomerPhone.flatMapLatest { phone ->
        repository.getBillsForCustomer(phone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customerTransactions = repository.loggedCustomerPhone.flatMapLatest { phone ->
        repository.getCustomerTransactionsForPhone(phone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentCustomerData = combine(customers, loggedCustomerPhone) { custList, phone ->
        custList.find { it.phone == phone }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Billing Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _billingCustomerName = MutableStateFlow("")
    val billingCustomerName: StateFlow<String> = _billingCustomerName.asStateFlow()

    private val _billingCustomerPhone = MutableStateFlow("")
    val billingCustomerPhone: StateFlow<String> = _billingCustomerPhone.asStateFlow()

    private val _billingDiscount = MutableStateFlow(0.0)
    val billingDiscount: StateFlow<Double> = _billingDiscount.asStateFlow()

    private val _billingTaxPercent = MutableStateFlow(0.0)
    val billingTaxPercent: StateFlow<Double> = _billingTaxPercent.asStateFlow()

    private val _billingPaymentMethod = MutableStateFlow("CASH") // CASH, UPI, UDHAAR, SPLIT
    val billingPaymentMethod: StateFlow<String> = _billingPaymentMethod.asStateFlow()

    private val _billingPaidAmount = MutableStateFlow(0.0)
    val billingPaidAmount: StateFlow<Double> = _billingPaidAmount.asStateFlow()

    // Dialog & UI Action States
    private val _showInvoiceDialog = MutableStateFlow(false)
    val showInvoiceDialog: StateFlow<Boolean> = _showInvoiceDialog.asStateFlow()

    private val _lastGeneratedBill = MutableStateFlow<Bill?>(null)
    val lastGeneratedBill: StateFlow<Bill?> = _lastGeneratedBill.asStateFlow()

    private val _lastGeneratedItems = MutableStateFlow<List<BillItem>>(emptyList())
    val lastGeneratedItems: StateFlow<List<BillItem>> = _lastGeneratedItems.asStateFlow()

    private val _selectedCustomerForLedger = MutableStateFlow<Customer?>(null)
    val selectedCustomerForLedger: StateFlow<Customer?> = _selectedCustomerForLedger.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Search filters
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _customerSearchQuery = MutableStateFlow("")
    val customerSearchQuery: StateFlow<String> = _customerSearchQuery.asStateFlow()

    // Mode & Tab Navigation
    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun setBusinessTab(tab: BusinessTab) {
        _businessTab.value = tab
    }

    fun selectBusiness(id: String) {
        repository.selectBusiness(id)
    }

    fun setCustomerPhone(phone: String) {
        repository.setLoggedCustomerPhone(phone)
    }

    fun selectCustomerForLedger(customer: Customer?) {
        _selectedCustomerForLedger.value = customer
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun showMessage(msg: String) {
        _userMessage.value = msg
    }

    // Billing actions
    fun addToCart(product: Product) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + 1.0)
        } else {
            current.add(CartItem(product, 1.0))
        }
        _cartItems.value = current
    }

    fun updateCartQuantity(productId: String, qty: Double) {
        if (qty <= 0) {
            _cartItems.value = _cartItems.value.filter { it.product.id != productId }
        } else {
            _cartItems.value = _cartItems.value.map {
                if (it.product.id == productId) it.copy(quantity = qty) else it
            }
        }
    }

    fun addCustomItemToCart(name: String, price: Double, qty: Double, unit: String) {
        val customProduct = Product(
            id = "CUSTOM_${System.currentTimeMillis()}",
            businessId = selectedBusinessId.value,
            name = name.ifBlank { "Item" },
            price = price,
            unit = unit,
            category = "Custom",
            stockQuantity = 999,
            isStockTracked = false
        )
        val current = _cartItems.value.toMutableList()
        current.add(CartItem(customProduct, qty))
        _cartItems.value = current
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _billingDiscount.value = 0.0
        _billingTaxPercent.value = 0.0
        _billingPaidAmount.value = 0.0
        _billingPaymentMethod.value = "CASH"
    }

    fun setBillingCustomer(name: String, phone: String) {
        _billingCustomerName.value = name
        _billingCustomerPhone.value = phone
    }

    fun setBillingDiscount(discount: Double) {
        _billingDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun setBillingTaxPercent(tax: Double) {
        _billingTaxPercent.value = tax.coerceAtLeast(0.0)
    }

    fun setBillingPaymentMethod(method: String) {
        _billingPaymentMethod.value = method
    }

    fun setBillingPaidAmount(amount: Double) {
        _billingPaidAmount.value = amount
    }

    fun completeBill() {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "बिल बनाने के लिए कम से कम 1 आइटम जोड़ें"
            return
        }

        viewModelScope.launch {
            val biz = currentBusiness.value
            val pointsRule = biz?.pointsPer100Rs ?: 1
            val result = repository.createBill(
                businessId = selectedBusinessId.value,
                customerName = _billingCustomerName.value,
                customerPhone = _billingCustomerPhone.value,
                items = _cartItems.value,
                discount = _billingDiscount.value,
                taxPercent = _billingTaxPercent.value,
                paymentMethod = _billingPaymentMethod.value,
                paidAmount = _billingPaidAmount.value,
                pointsPer100Rs = pointsRule
            )

            _lastGeneratedBill.value = result.bill
            _lastGeneratedItems.value = result.items
            _showInvoiceDialog.value = true
            clearCart()
            _userMessage.value = "बिल #${result.bill.billNumber} सफलतापूर्वक बन गया! 🎉"
        }
    }

    fun closeInvoiceDialog() {
        _showInvoiceDialog.value = false
    }

    fun showBillInvoice(bill: Bill) {
        viewModelScope.launch {
            val items = repository.getBillItems(bill.id)
            _lastGeneratedBill.value = bill
            _lastGeneratedItems.value = items
            _showInvoiceDialog.value = true
        }
    }

    // Khata Operations
    fun addCustomerUdhaar(customerId: String, amount: Double, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            val success = repository.addUdhaarManual(selectedBusinessId.value, customerId, amount, note)
            if (success) {
                _userMessage.value = "₹$amount का नया उधार जोड़ दिया गया! ✅"
                // refresh customer in ledger
                val updated = repository.getCustomerById(customerId)
                _selectedCustomerForLedger.value = updated
            }
        }
    }

    fun receiveCustomerPayment(customerId: String, amount: Double, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            val success = repository.recordPaymentReceived(selectedBusinessId.value, customerId, amount, note)
            if (success) {
                _userMessage.value = "₹$amount का भुगतान प्राप्त हुआ! खाता अपडेट हो गया ✅"
                val updated = repository.getCustomerById(customerId)
                _selectedCustomerForLedger.value = updated
            }
        }
    }

    // Reward code generation by Shopkeeper
    fun generateRewardCode(points: Int, billAmount: Double) {
        viewModelScope.launch {
            val code = repository.generateManualRewardCode(selectedBusinessId.value, points, billAmount)
            _userMessage.value = "रिवॉर्ड कोड बनाया गया: ${code.code} (${code.points} Points) 🎁"
        }
    }

    // Customer claiming Reward Code
    fun redeemCustomerCode(code: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.redeemCustomerRewardCode(
                code = code,
                customerPhone = loggedCustomerPhone.value,
                businessId = selectedBusinessId.value
            )
            onComplete(result.success, result.message)
            _userMessage.value = result.message
        }
    }

    // Customer claiming a Reward Tier
    fun claimRewardTier(tier: RewardTier) {
        val cust = currentCustomerData.value
        if (cust == null) {
            _userMessage.value = "कृपया पहले कस्टमर लॉगिन करें"
            return
        }
        viewModelScope.launch {
            val result = repository.claimCustomerRewardDirect(tier, cust)
            _userMessage.value = result.message
        }
    }

    // Verify voucher by Shopkeeper
    fun verifyVoucher(voucherCode: String) {
        viewModelScope.launch {
            val redemptions = repository.getRewardRedemptions(selectedBusinessId.value).first()
            val match = redemptions.find { it.voucherCode.equals(voucherCode.trim(), ignoreCase = true) }
            if (match != null) {
                if (match.status == "CLAIMED") {
                    _userMessage.value = "⚠️ यह वाउचर पहले ही रिडीम हो चुका है!"
                } else {
                    repository.updateRewardRedemptionStatus(match.copy(status = "CLAIMED"))
                    _userMessage.value = "सफलतापूर्वक सत्यापित! ग्राहक को '${match.rewardTitle}' प्रदान करें ✅"
                }
            } else {
                _userMessage.value = "❌ अमान्य वाउचर कोड!"
            }
        }
    }

    // Customer dispute
    fun reportDispute(tx: KhataTransaction, reason: String) {
        viewModelScope.launch {
            repository.updateTransactionDispute(tx, "REPORTED", reason)
            _userMessage.value = "आपकी समस्या दर्ज कर ली गई है। दुकानदार को सूचना भेज दी गई है 📩"
        }
    }

    fun resolveDispute(tx: KhataTransaction) {
        viewModelScope.launch {
            repository.updateTransactionDispute(tx, "RESOLVED", "दुकानदार द्वारा समाधान किया गया")
            _userMessage.value = "Dispute resolved successfully ✅"
        }
    }

    // Product & Tier management
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "प्रोडक्ट सुरक्षित हो गया ✅"
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _userMessage.value = "प्रोडक्ट हटा दिया गया 🗑️"
        }
    }

    fun saveRewardTier(tier: RewardTier) {
        viewModelScope.launch {
            repository.saveRewardTier(tier)
            _userMessage.value = "रिवॉर्ड नियम सुरक्षित हो गया 🎁"
        }
    }

    fun deleteRewardTier(tier: RewardTier) {
        viewModelScope.launch {
            repository.deleteRewardTier(tier)
            _userMessage.value = "रिवॉर्ड हटाया गया 🗑️"
        }
    }

    fun updateBusinessProfile(business: Business) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            _userMessage.value = "दुकान प्रोफाइल अपडेट हो गई! ✅"
        }
    }

    fun setProductSearch(q: String) {
        _productSearchQuery.value = q
    }

    fun setCustomerSearch(q: String) {
        _customerSearchQuery.value = q
    }
}
