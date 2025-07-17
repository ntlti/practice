package com.example.expensetracker.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.expensetracker.R
import com.example.expensetracker.databinding.FragmentTransactionDetailsBinding
import com.example.expensetracker.util.currencyFormat
import com.example.expensetracker.viewmodel.TransactionsViewModel

class TransactionDetailFragment : Fragment(R.layout.fragment_transaction_details) {

    private lateinit var binding: FragmentTransactionDetailsBinding
    private val args: TransactionDetailFragmentArgs by navArgs()
    private lateinit var viewModel: TransactionsViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTransactionDetailsBinding.bind(view)
        viewModel = (activity as MainActivity).viewModel
        observeData()
    }

    private fun observeData() = with(binding) {
        val transactionItem = viewModel.getTransactionById(args.transaction.id)
        lifecycleScope.launchWhenStarted {
            transactionItem.collect { transaction ->
                tvDetailsTitle.text = transaction.title
                tvDetailsCategory.text = getString(transaction.category.description)
                tvDetailsNotes.text = transaction.note
                tvDetailsType.text = transaction.transactionType
                tvDetailsDate.text = transaction.date
                tvDetailsAmount.text = currencyFormat(transaction.amount, viewModel.selectedCurrency.value)

                // Отображение чека
                val receiptImageView: ImageView = ivReceipt
                if (!transaction.receiptUri.isNullOrEmpty()) {
                    Glide.with(this@TransactionDetailFragment)
                        .load(Uri.parse(transaction.receiptUri))
                        .placeholder(R.drawable.ic_image_placeholder) // Опционально: заглушка
                        .error(R.drawable.ic_image_error) // Опционально: ошибка загрузки
                        .into(receiptImageView)
                    receiptImageView.visibility = View.VISIBLE
                } else {
                    receiptImageView.visibility = View.GONE
                }

                val action =
                    TransactionDetailFragmentDirections.actionTransactionDetailFragmentToEditTransactionFragment(
                        transaction
                    )
                fabEditTransaction.setOnClickListener { findNavController().navigate(action) }
            }
        }
    }
}