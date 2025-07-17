package com.example.expensetracker.ui

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentAddTransactionBinding
import com.example.expensetracker.model.TransactionModel
import com.example.expensetracker.util.Constants
import com.example.expensetracker.util.TransactionCategory
import com.example.expensetracker.util.getCurrencySymbol
import com.example.expensetracker.viewmodel.TransactionsViewModel
import com.google.android.material.snackbar.Snackbar
import java.lang.Double.parseDouble
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment : Fragment(R.layout.fragment_add_transaction) {

    private lateinit var binding: FragmentAddTransactionBinding
    private lateinit var viewModel: TransactionsViewModel
    private var receiptUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            receiptUri = result.data?.data
            if (receiptUri != null) {
                binding.inputFields.ivReceiptPreview.setImageURI(receiptUri)
                binding.inputFields.ivReceiptPreview.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Чек выбран: $receiptUri", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Ошибка выбора чека", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddTransactionBinding.bind(view)
        viewModel = (activity as MainActivity).viewModel
        setUpView()
    }

    private fun setUpView() {
        with(binding) {
            inputFields.etTransactionCategory.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    Constants(requireContext()).TRANSACTION_CATEGORY
                )
            )
            inputFields.etTransactionType.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    Constants(requireContext()).TRANSACTION_TYPE
                )
            )

            inputFields.etTransactionDate.apply {
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = false
                val myCalendar = Calendar.getInstance()
                val sdf = SimpleDateFormat(context.getString(R.string.date_format), Locale.ITALIAN)
                val datePickerOnDateSetListener =
                    DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                        myCalendar.set(Calendar.YEAR, year)
                        myCalendar.set(Calendar.MONTH, monthOfYear)
                        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        setText(sdf.format(myCalendar.time))
                    }

                val datePickerDialog = DatePickerDialog(
                    requireContext(),
                    datePickerOnDateSetListener,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                )

                datePickerDialog.datePicker.maxDate = Date().time
                setOnClickListener {
                    datePickerDialog.show()
                }
            }

            lifecycleScope.launchWhenCreated {
                viewModel.selectedCurrency.collect {
                    inputFields.tilTransactionAmount.prefixText = getCurrencySymbol(it)
                }
            }

            // Кнопка для добавления чека
            btnAddReceipt.setOnClickListener {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)
                } else {
                    openGallery()
                }
            }

            // Кнопка для сохранения транзакции
            btnSaveTransaction.setOnClickListener {
                if (!validateFields()) {
                    insertTransaction()
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        pickImage.launch(intent)
    }

    private fun getNoteFromData(): TransactionModel = binding.inputFields.let {
        val title = it.etTransactionTitle.text.toString()
        val amount = parseDouble(it.etTransactionAmount.text.toString())
        val date = it.etTransactionDate.text.toString()
        val category = when (it.etTransactionCategory.text.toString()) {
            getString(R.string.bills) -> TransactionCategory.Bills
            getString(R.string.food) -> TransactionCategory.Food
            getString(R.string.education) -> TransactionCategory.Education
            getString(R.string.entertainment) -> TransactionCategory.Entertainment
            getString(R.string.housing) -> TransactionCategory.Housing
            getString(R.string.health) -> TransactionCategory.Health
            getString(R.string.travel) -> TransactionCategory.Travel
            getString(R.string.transportation) -> TransactionCategory.Transportation
            getString(R.string.shopping) -> TransactionCategory.Shopping
            getString(R.string.salary) -> TransactionCategory.Salary
            getString(R.string.investments) -> TransactionCategory.Investments
            getString(R.string.other) -> TransactionCategory.Other
            else -> TransactionCategory.Other
        }
        val type = when (it.etTransactionType.text.toString()) {
            getString(R.string.income) -> "Income"
            getString(R.string.expenses) -> "Expense"
            else -> "other"
        }
        val note = it.etTransactionNotes.text.toString()

        return TransactionModel(
            title = title,
            transactionType = type,
            amount = amount,
            category = category,
            date = date,
            note = note,
            receiptUri = receiptUri?.toString() // Сохраняем URI чека
        )
    }

    private fun validateFields(): Boolean {
        with(binding.inputFields) {
            when {
                etTransactionAmount.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_field), Toast.LENGTH_SHORT).show()
                    etTransactionAmount.error = getString(R.string.required_field)
                    return true
                }
                etTransactionTitle.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_field), Toast.LENGTH_SHORT).show()
                    etTransactionTitle.error = getString(R.string.required_field)
                    return true
                }
                etTransactionCategory.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_category), Toast.LENGTH_SHORT).show()
                    return true
                }
                etTransactionType.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_transaction_type), Toast.LENGTH_SHORT).show()
                    return true
                }
                etTransactionDate.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.invalid_date), Toast.LENGTH_SHORT).show()
                    return true
                }
            }
            return false
        }
    }

    private fun insertTransaction() {
        val transaction = getNoteFromData()
        viewModel.insertTransaction(transaction)
        Snackbar.make(
            requireView(),
            getString(R.string.transaction_saved),
            Snackbar.LENGTH_SHORT
        ).show()
        findNavController().navigateUp()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            Toast.makeText(context, "Разрешение на доступ к галерее не предоставлено", Toast.LENGTH_SHORT).show()
        }
    }
}