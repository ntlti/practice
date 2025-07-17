package com.example.expensetracker.ui.filter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.expensetracker.R
import com.example.expensetracker.adapter.TransactionItemAdapter
import com.example.expensetracker.util.TransactionCategory
import com.example.expensetracker.viewmodel.TransactionsViewModel
import java.text.SimpleDateFormat
import java.util.*

class FilterFragment : Fragment() {

    private lateinit var viewModel: TransactionsViewModel
    private var dateFrom: Long? = null
    private var dateTo: Long? = null
    private var selectedCategory: TransactionCategory? = null
    private var sortOrder: String = "ASC"
    private lateinit var adapter: TransactionItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_filter, container, false)
        viewModel = ViewModelProvider(requireActivity()).get(TransactionsViewModel::class.java)

        // Настройка RecyclerView
        adapter = TransactionItemAdapter()
        adapter.currency = viewModel.selectedCurrency.value ?: "USD"
        val recyclerView: RecyclerView = view.findViewById(R.id.rv_transactions)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener {
            Log.d("FilterFragment", "Item clicked: ${it.title}")
        }
        Log.d("FilterFragment", "RecyclerView initialized")

        // Настройка спиннера категорий
        val spinnerCategory: Spinner = view.findViewById(R.id.spinner_category)
        val categories = TransactionCategory.TRANSACTION_CATEGORIES.map { requireContext().getString(it.description) }
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, listOf("Все") + categories)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapterSpinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                selectedCategory = if (position == 0) null else TransactionCategory.TRANSACTION_CATEGORIES[position - 1]
                val categoryName = if (position == 0) "Все" else requireContext().getString(selectedCategory?.description ?: R.string.other)
                Log.d("FilterFragment", "Selected category: $categoryName")
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategory = null
                Log.d("FilterFragment", "No category selected")
            }
        }

        // Настройка кнопок даты
        val btnDateFrom: Button = view.findViewById(R.id.btn_date_from)
        val btnDateTo: Button = view.findViewById(R.id.btn_date_to)

        btnDateFrom.setOnClickListener {
            showDatePicker("От", btnDateFrom) { date ->
                dateFrom = date.time
                btnDateFrom.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                Log.d("FilterFragment", "Date from: $dateFrom")
            }
        }

        btnDateTo.setOnClickListener {
            showDatePicker("До", btnDateTo) { date ->
                dateTo = date.time
                btnDateTo.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
                Log.d("FilterFragment", "Date to: $dateTo")
            }
        }

        // Настройка сортировки
        val radioGroupSort: RadioGroup = view.findViewById(R.id.radio_group_sort)
        radioGroupSort.setOnCheckedChangeListener { _, checkedId ->
            sortOrder = if (checkedId == R.id.radio_asc) "ASC" else "DESC"
            Log.d("FilterFragment", "Sort order: $sortOrder")
        }

        // Применение фильтров
        val btnApplyFilter: Button = view.findViewById(R.id.btn_apply_filter)
        btnApplyFilter.setOnClickListener {
            try {
                viewModel.setFilters(dateFrom, dateTo, selectedCategory, sortOrder)
                Log.d("FilterFragment", "Applied filters - dateFrom: $dateFrom, dateTo: $dateTo, category: $selectedCategory, sortOrder: $sortOrder")
                viewModel.getFilteredTransactions().observe(viewLifecycleOwner) { transactions ->
                    adapter.differ.submitList(transactions)
                    Log.d("FilterFragment", "Filtered transactions size: ${transactions?.size ?: 0}")
                    if (transactions.isNullOrEmpty()) {
                        Toast.makeText(requireContext(), "Нет транзакций для отображения", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.d("FilterFragment", "First transaction: ${transactions.firstOrNull()?.title}")
                    }
                }
            } catch (e: Exception) {
                Log.e("FilterFragment", "Error applying filters", e)
                Toast.makeText(requireContext(), "Ошибка при применении фильтров: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Изначальная загрузка данных
        viewModel.setFilters(null, null, null, "ASC")
        viewModel.getFilteredTransactions().observe(viewLifecycleOwner) { transactions ->
            adapter.differ.submitList(transactions)
            Log.d("FilterFragment", "Initial transactions size: ${transactions?.size ?: 0}")
            if (transactions.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Нет начальных транзакций", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showDatePicker(title: String, button: Button, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePicker.setTitle(title)
        datePicker.show()
    }
}