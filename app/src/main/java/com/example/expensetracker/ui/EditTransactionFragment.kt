package com.example.expensetracker.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentEditTransactionBinding
import com.example.expensetracker.model.TransactionModel
import com.example.expensetracker.util.Constants
import com.example.expensetracker.util.TransactionCategory
import com.example.expensetracker.util.getCurrencySymbol
import com.example.expensetracker.viewmodel.TransactionsViewModel
import com.google.android.material.snackbar.Snackbar
import java.lang.Double.parseDouble
import java.text.SimpleDateFormat
import java.util.*

class EditTransactionFragment : Fragment(R.layout.fragment_edit_transaction) {

    private lateinit var binding: FragmentEditTransactionBinding
    private val args: EditTransactionFragmentArgs by navArgs()
    private lateinit var viewModel: TransactionsViewModel
    private var receiptUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            receiptUri = result.data?.data

            Toast.makeText(requireContext(), "Новый чек выбран: $receiptUri", Toast.LENGTH_SHORT).show()
            updateReceiptPreview()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentEditTransactionBinding.bind(view)
        viewModel = (activity as MainActivity).viewModel
        setUpViewWithData()
        binding.btnSaveTransaction.setOnClickListener {
            validateFields()
            updateTransaction()
        }
    }

    private fun setUpViewWithData() {
        val transaction = args.transaction
        with(binding.inputFields) {
            etTransactionTitle.setText(transaction.title)
            etTransactionAmount.setText(transaction.amount.toString())
            etTransactionType.apply {
                setText(transaction.transactionType)
                setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        Constants(requireContext()).TRANSACTION_TYPE
                    )
                )
            }
            etTransactionCategory.apply {
                setText(getString(transaction.category.description))
                setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        Constants(requireContext()).TRANSACTION_CATEGORY
                    )
                )
            }
            etTransactionDate.apply {
                setText(transaction.date)
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
            etTransactionNotes.setText(transaction.note)
            lifecycleScope.launchWhenCreated {
                viewModel.selectedCurrency.collect {
                    tilTransactionAmount.prefixText = getCurrencySymbol(it)
                }
            }

            // Инициализация текущего чека
            receiptUri = transaction.receiptUri?.let { Uri.parse(it) }
            updateReceiptPreview()

            // Кнопка для изменения чека
            btnChangeReceipt.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                pickImage.launch(intent)
            }

            // Кнопка для удаления чека
            btnRemoveReceipt.setOnClickListener {
                receiptUri = null
                updateReceiptPreview()
                Toast.makeText(requireContext(), "Чек удален", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateReceiptPreview() {
        with(binding.inputFields) {
            if (receiptUri != null) {
                Glide.with(this@EditTransactionFragment)
                    .load(receiptUri)
                    .placeholder(R.drawable.ic_image_placeholder) // Опционально
                    .error(R.drawable.ic_image_error) // Опционально
                    .into(ivReceiptPreview)
                ivReceiptPreview.visibility = View.VISIBLE
            } else {
                ivReceiptPreview.setImageDrawable(null)
                ivReceiptPreview.visibility = View.GONE
            }
        }
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
            id = args.transaction.id, // Сохраняем ID для обновления
            title = title,
            transactionType = type,
            amount = amount,
            category = category,
            date = date,
            note = note,
            receiptUri = receiptUri?.toString() // Обновляем или удаляем receiptUri
        )
    }

    private fun validateFields() {
        with(binding.inputFields) {
            when {
                etTransactionAmount.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_field), Toast.LENGTH_SHORT).show()
                    etTransactionAmount.error = getString(R.string.required_field)
                }
                etTransactionTitle.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_field), Toast.LENGTH_SHORT).show()
                    etTransactionTitle.error = getString(R.string.required_field)
                }
                etTransactionCategory.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_category), Toast.LENGTH_SHORT).show()
                }
                etTransactionType.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.missing_transaction_type), Toast.LENGTH_SHORT).show()
                }
                etTransactionDate.text.isNullOrEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.invalid_date), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTransaction() {
        if (with(binding.inputFields) {
                etTransactionDate.text!!.isNotEmpty() &&
                        etTransactionTitle.text!!.isNotEmpty() &&
                        etTransactionAmount.text!!.isNotEmpty() &&
                        etTransactionType.text.isNotEmpty() &&
                        etTransactionCategory.text.isNotEmpty()
            }) {
            viewModel.updateTransaction(getNoteFromData())
            Snackbar.make(binding.root, getString(R.string.transaction_saved), Snackbar.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }
}