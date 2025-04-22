package com.example.bookswapkz

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController // Импортируем NavController
import androidx.navigation.fragment.NavHostFragment // Импортируем NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.bookswapkz.databinding.ActivityMainBinding // Используем ViewBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
// com.google.android.material.bottomnavigation.BottomNavigationView импорт не нужен, т.к. доступ через binding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Используем ViewBinding
    private lateinit var auth: FirebaseAuth
    private val TAG = "MainActivity"
    private lateinit var navController: NavController // Поле для NavController
    private lateinit var appBarConfiguration: AppBarConfiguration // Поле для AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Используем ViewBinding для установки макета
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Устанавливаем Toolbar из binding
        setSupportActionBar(binding.toolbar) // Используем binding.toolbar
        supportActionBar?.setDisplayShowTitleEnabled(false) // Скрываем стандартный заголовок

        // --- Получаем NavController ПРАВИЛЬНО ---
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment // Используем ID из макета
        navController = navHostFragment.navController
        // --- Конец получения NavController ---

        // --- Настраиваем AppBarConfiguration для Bottom Navigation ---
        // Перечисляем ID фрагментов верхнего уровня (те, что в нижнем меню)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.myBooksFragment,
                R.id.rentFragment, // Добавляем ID фрагмента аренды
                R.id.profileFragment
            )
            // DrawerLayout здесь не нужен
        )
        // --- Конец настройки AppBarConfiguration ---

        // --- Связываем ActionBar (Toolbar) с NavController ---
        setupActionBarWithNavController(navController, appBarConfiguration)
        // --- Конец связи ActionBar ---

        // --- Связываем BottomNavigationView (из binding) с NavController ---
        binding.bottomNavigation.setupWithNavController(navController) // Используем binding.bottomNavigation
        // --- Конец связи BottomNavigationView ---

        // Проверка статуса входа пользователя (опционально)
        if (auth.currentUser == null) {
            Log.d(TAG, "User not logged in initially.")
            // Примечание: Граф навигации должен сам обработать переход на логин, если startDestination - loginFragment
            // Если startDestination - homeFragment, то здесь может потребоваться навигация на логин
            // navController.navigate(R.id.loginFragment) // Пример перехода
        } else {
            Log.d(TAG, "User already logged in: ${auth.currentUser?.uid}")
        }
    }

    // Переопределяем onSupportNavigateUp для корректной работы кнопки "Назад" в Toolbar
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Метод onBackPressed() больше не нужен для DrawerLayout
}