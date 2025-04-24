package com.example.bookswapkz

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.bookswapkz.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start animations
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // Book icon animations
        val bookScaleX = ObjectAnimator.ofFloat(binding.bookIcon, "scaleX", 0.3f, 1f)
        val bookScaleY = ObjectAnimator.ofFloat(binding.bookIcon, "scaleY", 0.3f, 1f)
        val bookAlpha = ObjectAnimator.ofFloat(binding.bookIcon, "alpha", 0f, 1f)
        val bookRotation = ObjectAnimator.ofFloat(binding.bookIcon, "rotation", -30f, 0f)

        // Text animations
        val textAlpha = ObjectAnimator.ofFloat(binding.appNameText, "alpha", 0f, 1f)
        val textTranslationY = ObjectAnimator.ofFloat(binding.appNameText, "translationY", 50f, 0f)

        // Combine all animations
        AnimatorSet().apply {
            playTogether(bookScaleX, bookScaleY, bookAlpha, bookRotation, textAlpha, textTranslationY)
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 300

            // Add listener for animation end
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    startMainActivity()
                }
            })
            
            start()
        }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
} 