package com.example.bookswapkz

import android.os.Bundle
import android.util.Log
// import android.widget.TextView // Не используется явно, можно удалить
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
// FirebaseDatabase импорт и использование удалены
import com.example.bookswapkz.R

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    // Переменная database удалена
    private val TAG = "MainActivity"
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        // Инициализация database удалена

        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        Log.d(TAG, "Starting anonymous sign-in")
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Sign-in successful")
                    // Убираем Toast, так как вход анонимный и невидим для пользователя
                    // Toast.makeText(this, "Firebase подключен!", Toast.LENGTH_SHORT).show()
                    setupNavigation()
                } else {
                    Log.e(TAG, "Anonymous sign-in failed: ${task.exception?.message}")
                    // Показываем ошибку только если она важна для пользователя
                    // Toast.makeText(this, "Ошибка подключения: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    // Возможно, здесь нужно обработать ситуацию, когда анонимный вход не удался
                }
            }
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)

        // Убедитесь, что ID здесь соответствуют вашему nav_graph и nav_menu
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.myBooksFragment, R.id.profileFragment
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}