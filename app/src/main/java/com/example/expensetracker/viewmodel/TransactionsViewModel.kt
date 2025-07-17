package com.example.expensetracker.viewmodel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.*
import com.example.expensetracker.TransactionApp
import com.example.expensetracker.model.TransactionModel
import com.example.expensetracker.repository.TransactionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import android.util.Log
import com.example.expensetracker.util.TransactionCategory
import com.google.type.Date

class TransactionsViewModel(
    private val repository: TransactionsRepository,
    app: Application
) : AndroidViewModel(app) {

    private val Context.limitDataStore by preferencesDataStore("spending_limit")
    private val Context.UIdataStore by preferencesDataStore("UI_preference")
    private val limitDataStore = getApplication<TransactionApp>().limitDataStore
    private val UIdataStore = getApplication<TransactionApp>().UIdataStore

    private val _period = MutableLiveData(Period.ofYears(5))
    var period = _period

    var transaction: LiveData<List<TransactionModel>> =
        getAllTransactions().asLiveData()

    private val _isWarningClosed = MutableLiveData(false)
    var isWarningClosed = _isWarningClosed

    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency = _selectedCurrency

    // Переменные для фильтров
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var selectedCategory: TransactionCategory? = null // Изменил на TransactionCategory
    private var sortOrder: String = "ASC"

    suspend fun readUIPreference(key: String): Boolean? {
        val dataStoreKey = booleanPreferencesKey(key)
        val preferences = UIdataStore.data.first()
        return preferences[dataStoreKey]
    }

    suspend fun saveUIPreference(key: String, value: Boolean) {
        val preferenceKey = booleanPreferencesKey(key)
        UIdataStore.edit {
            it[preferenceKey] = value
        }
    }

    suspend fun saveLimit(key: String, value: Int) {
        val preferenceKey = intPreferencesKey(key)
        limitDataStore.edit {
            it[preferenceKey] = value
        }
    }

    suspend fun readLimit(key: String): Int? {
        val dataStoreKey = intPreferencesKey(key)
        val preferences = limitDataStore.data.first()
        return preferences[dataStoreKey]
    }

    init {
        transaction
    }

    // Метод для установки фильтров
    fun setFilters(dateFrom: Long?, dateTo: Long?, category: TransactionCategory?, sortOrder: String) {
        this.dateFrom = dateFrom
        this.dateTo = dateTo
        this.selectedCategory = category
        this.sortOrder = sortOrder
        Log.d("TransactionsViewModel", "Filters set - dateFrom: $dateFrom, dateTo: $dateTo, category: $category, sortOrder: $sortOrder")
    }

    // Метод для получения отфильтрованных транзакций
    fun getFilteredTransactions(): LiveData<List<TransactionModel>> {
        Log.d("TransactionsViewModel", "Getting filtered transactions - dateFrom: $dateFrom, dateTo: $dateTo, category: $selectedCategory, sortOrder: $sortOrder")
        return filterByExactDates(dateFrom, dateTo, selectedCategory)
    }

    // Метод для фильтрации по периоду
    fun filterAllTransactions(filterPeriod: Period? = period.value): LiveData<List<TransactionModel>> =
        if (filterPeriod != null) {
            repository.getAllTransactions().map { list ->
                list.filter { transaction ->
                    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val formattedDate = LocalDate.parse(transaction.date, dateFormatter)
                    val currentDate = LocalDate.now() // 11 июля 2025
                    formattedDate in currentDate.minus(filterPeriod)..currentDate
                }
            }.asLiveData(context = viewModelScope.coroutineContext)
        } else repository.getAllTransactions().asLiveData(context = viewModelScope.coroutineContext)

    // Метод для фильтрации по точным датам и категории
    fun filterByExactDates(dateFrom: Long?, dateTo: Long?, category: TransactionCategory?): LiveData<List<TransactionModel>> {
        val baseFlow = repository.getAllTransactions()
        return baseFlow.map { transactions ->
            transactions.filter { transaction ->
                val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val transactionDate = LocalDate.parse(transaction.date, dateFormatter)
                val fromDate = dateFrom?.let { java.util.Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
                val toDate = dateTo?.let { java.util.Date(it).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
                val dateFromMatch = fromDate == null || transactionDate >= fromDate
                val dateToMatch = toDate == null || transactionDate <= toDate
                val categoryMatch = category == null || transaction.category == category
                dateFromMatch && dateToMatch && categoryMatch
            }.let { filteredList ->
                if (sortOrder == "ASC") filteredList.sortedBy { it.amount } else filteredList.sortedByDescending { it.amount }
            }
        }.asLiveData(context = viewModelScope.coroutineContext)
    }

    fun getAllTransactions() = repository.getAllTransactions()

    fun getTransactionById(id: Int) = repository.getTransactionById(id)

    fun insertTransaction(transaction: TransactionModel) =
        viewModelScope.launch { repository.insert(transaction) }

    fun deleteTransaction(transaction: TransactionModel) =
        viewModelScope.launch { repository.delete(transaction) }

    fun updateTransaction(transaction: TransactionModel) =
        viewModelScope.launch { repository.update(transaction) }
}