package com.example.bookswapkz.di

// Удаляем импорт старого или переименованного репозитория, если был
// import com.example.bookswapkz.FirebaseRepository
import com.example.bookswapkz.data.FirebaseRepository // Используем репозиторий из пакета data
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }

    // --- УДАЛИТЬ ЭТИ МЕТОДЫ ---
    // Hilt найдет FirebaseRepository через @Inject constructor()
    /*
    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository { // УДАЛИТЬ ЭТОТ МЕТОД
        return FirebaseRepository()
    }
    */
    /*
    @Provides
    @Singleton
    fun provideNewFirebaseRepository(firestore: FirebaseFirestore): NewFirebaseRepository { // УДАЛИТЬ ЭТОТ МЕТОД (если это тот же репозиторий)
        return NewFirebaseRepository(firestore)
    }
    */
    // --- КОНЕЦ УДАЛЕНИЯ ---
}