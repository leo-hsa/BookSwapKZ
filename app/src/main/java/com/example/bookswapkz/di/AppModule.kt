package com.example.bookswapkz.di

// import com.example.bookswapkz.data.FirebaseRepository // FirebaseRepository будет использовать @Inject constructor
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

    // FirebaseRepository будет предоставлен Hilt автоматически через @Inject constructor(),
    // если его зависимости (firestore, auth, storage) также предоставлены в этом модуле.
    // Поэтому provideFirebaseRepository не нужен.
}