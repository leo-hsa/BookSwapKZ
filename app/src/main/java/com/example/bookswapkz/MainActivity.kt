package com.example.bookswapkz

import android.os.Bundle
import android.util.Log
import android.widget.TextView // Добавьте этот импорт, если еще нет
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar // Добавьте этот импорт
import androidx.drawerlayout.widget.DrawerLayout // Добавьте этот импорт
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration // Добавьте этот импорт
import androidx.navigation.ui.navigateUp // Добавьте этот импорт
import androidx.navigation.ui.setupActionBarWithNavController // Добавьте этот импорт
import androidx.navigation.ui.setupWithNavController
// import com.google.android.material.bottomnavigation.BottomNavigationView // Этот импорт больше не нужен
import com.google.android.material.navigation.NavigationView // Добавьте этот импорт
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
// Другие ваши импорты...

import com.example.bookswapkz.R // Убедитесь, что R импортирован

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val TAG = "MainActivity"
    private lateinit var appBarConfiguration: AppBarConfiguration // Добавляем переменную для AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout // Добавляем переменную для DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Инициализируем DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout) // Находим DrawerLayout по ID

        // Находим Toolbar и устанавливаем его как ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Скрываем стандартный заголовок, так как у вас свой TextView

        Log.d(TAG, "Starting anonymous sign-in")
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Sign-in successful")
                    Toast.makeText(this, "Firebase подключен!", Toast.LENGTH_SHORT).show()

                    // Настройка навигации после успешной авторизации
                    setupNavigation()
                } else {
                    Log.e(TAG, "Sign-in failed: ${task.exception?.message}")
                    Toast.makeText(this, "Ошибка: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)

        // Определяем пункты меню верхнего уровня для DrawerLayout
        // Убедитесь, что ID в nav_graph.xml соответствуют ID в nav_menu.xml
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.myBooksFragment, R.id.profileFragment // Замените на реальные ID ваших фрагментов из nav_graph и nav_menu
            ), drawerLayout // Передаем DrawerLayout
        )

        // Настраиваем ActionBar (Toolbar) для работы с NavController и DrawerLayout
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Настраиваем NavigationView (боковое меню) для работы с NavController
        val navView: NavigationView = findViewById(R.id.nav_view) // Находим NavigationView по ID
        navView.setupWithNavController(navController)

        /* Удаляем код для BottomNavigationView:
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)
        */
    }

    // Переопределяем этот метод, чтобы кнопка "назад" и иконка гамбургера работали с NavController
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}