package com.example.expensetracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.expensetracker.R
import com.example.expensetracker.databinding.ActivityMainBinding
import com.example.expensetracker.db.TransactionsDatabase
import com.example.expensetracker.repository.TransactionsRepository
import com.example.expensetracker.viewmodel.TransactionsViewModel
import com.example.expensetracker.viewmodel.TransactionsViewModelProvFactory
import com.google.android.material.navigation.NavigationView
import androidx.navigation.ui.NavigationUI
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    lateinit var viewModel: TransactionsViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val repository = TransactionsRepository(TransactionsDatabase.getInstance(this))

        // Запрос разрешения на запись во внешнее хранилище
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }

        val viewModelProviderFactory = TransactionsViewModelProvFactory(repository, application)
        viewModel = ViewModelProvider(this, viewModelProviderFactory)[TransactionsViewModel::class.java]
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.findNavController()
        drawerLayout = binding.drawerLayout
        navView = binding.drawerNavView // Исправлено с binding.drawerNavView
        navView.setupWithNavController(navController)
        setSupportActionBar(binding.toolbar)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.dashboardFragment, R.id.reportsFragment, R.id.settingsFragment, R.id.filterFragment), drawerLayout)

        // Настройка обработки пунктов меню
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.reportsFragment -> {
                    navController.navigate(R.id.reportsFragment)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.settingsFragment -> {
                    navController.navigate(R.id.settingsFragment)
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_filter -> {
                    navController.navigate(R.id.filterFragment) // Исправлено на filterFragment
                    drawerLayout.closeDrawers()
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(menuItem, navController)
            }
        }

        binding.toolbar.setupWithNavController(navController, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Исправленная обработка заголовка
        navController.addOnDestinationChangedListener { _, destination, _ ->
            title = when (destination.id) {
                R.id.dashboardFragment -> getString(R.string.dashboard)
                R.id.reportsFragment -> getString(R.string.reports)
                R.id.settingsFragment -> getString(R.string.settings)
                R.id.filterFragment -> getString(R.string.filter)
                R.id.addTransactionFragment -> getString(R.string.add_new_transaction)
                R.id.transactionDetailFragment -> getString(R.string.transaction_details)
                R.id.editTransactionFragment -> getString(R.string.edit_transaction)
                else -> getString(R.string.app_name)
            }
        }

        lifecycleScope.launchWhenCreated {
            if (viewModel.readUIPreference("night_mode") == true) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Разрешение на запись предоставлено", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Разрешение на запись не предоставлено", Toast.LENGTH_SHORT).show()
        }
    }
}