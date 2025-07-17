package com.example.expensetracker.ui

import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentSettingsBinding
import com.example.expensetracker.util.getCurrencySymbol
import com.example.expensetracker.viewmodel.TransactionsViewModel
import com.opencsv.CSVWriter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var viewModel: TransactionsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)
        viewModel = (activity as MainActivity).viewModel
        collectData()
        binding.btnSave.setOnClickListener {
            if (binding.etSpendingLimit.text!!.isNotEmpty()) {
                updateData()
                Toast.makeText(activity, getString(R.string.change_saved), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else Toast.makeText(activity, getString(R.string.invalid_spending_limit), Toast.LENGTH_SHORT).show()
        }
        setUpSwitch()
        setupExportButton()
    }

    private fun setupExportButton() {
        binding.btnExportData.setOnClickListener {
            showExportDialog()
        }
    }

    private fun setUpSwitch() = with(binding) {
        lifecycleScope.launchWhenCreated {
            switchDarkTheme.isChecked = viewModel.readUIPreference("night_mode") == true
            switchDarkTheme.setOnCheckedChangeListener { _, nightMode ->
                lifecycleScope.launchWhenStarted {
                    if (nightMode) {
                        viewModel.saveUIPreference("night_mode", true)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        viewModel.saveUIPreference("night_mode", false)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
            }
        }
    }

    private fun collectData() = with(binding) {
        lifecycleScope.launchWhenStarted {
            viewModel.selectedCurrency.collect {
                tilSpendingLimit.prefixText = getCurrencySymbol(it)
            }
        }
        lifecycleScope.launchWhenStarted {
            val limit = viewModel.readLimit("limit")
            if (limit != null) etSpendingLimit.setText(limit.toString())
        }
    }

    private fun updateData() = with(binding) {
        lifecycleScope.launchWhenStarted {
            val spendingLimit = etSpendingLimit.text.toString().toInt()
            viewModel.saveLimit("limit", spendingLimit)
        }
    }

    private fun showExportDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.choose_format)
            .setItems(arrayOf(getString(R.string.export_csv), getString(R.string.export_excel))) { _, which ->
                when (which) {
                    0 -> exportToCSV()
                    1 -> exportToExcel()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun exportToCSV() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "expenses_$timeStamp.csv"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            val writer = CSVWriter(FileWriter(file))
            val headers = arrayOf("ID", "Title", "Type", "Amount", "Category", "Date", "Notes", "Receipt URI")
            writer.writeNext(headers)

            lifecycleScope.launchWhenCreated {
                viewModel.getAllTransactions().collect { transactions ->
                    transactions.forEach { transaction ->
                        val data = arrayOf(
                            transaction.id.toString(),
                            transaction.title,
                            transaction.transactionType,
                            transaction.amount.toString(),
                            getString(transaction.category.description),
                            transaction.date,
                            transaction.note ?: "",
                            transaction.receiptUri ?: ""
                        )
                        writer.writeNext(data)
                    }
                    writer.close()
                    Toast.makeText(activity, getString(R.string.export_success, file.absolutePath), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, getString(R.string.export_error, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun exportToExcel() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "expenses_$timeStamp.xlsx"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Expenses")
            val headers = arrayOf("ID", "Title", "Type", "Amount", "Category", "Date", "Notes", "Receipt URI")
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, value ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(value)
            }

            lifecycleScope.launchWhenCreated {
                viewModel.getAllTransactions().collect { transactions ->
                    var rowNum = 1
                    transactions.forEach { transaction ->
                        val row = sheet.createRow(rowNum++)
                        row.createCell(0).setCellValue(transaction.id.toString())
                        row.createCell(1).setCellValue(transaction.title)
                        row.createCell(2).setCellValue(transaction.transactionType)
                        row.createCell(3).setCellValue(transaction.amount.toString())
                        row.createCell(4).setCellValue(getString(transaction.category.description))
                        row.createCell(5).setCellValue(transaction.date)
                        row.createCell(6).setCellValue(transaction.note ?: "")
                        row.createCell(7).setCellValue(transaction.receiptUri ?: "")
                    }

                    workbook.write(file.outputStream())
                    workbook.close()
                    Toast.makeText(activity, getString(R.string.export_success, file.absolutePath), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, getString(R.string.export_error, e.message), Toast.LENGTH_LONG).show()
        }
    }
}