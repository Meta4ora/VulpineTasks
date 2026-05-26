package com.example.vulpinetasks

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.vulpinetasks.utils.SettingsManager

class SplashActivity : AppCompatActivity() {

    private lateinit var logo: ImageView
    private lateinit var appName: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        logo = findViewById(R.id.logo)
        appName = findViewById(R.id.app_name)

        animateLogo()
    }

    private fun animateLogo() {
        // Начальное состояние
        logo.apply {
            scaleX = 0.1f
            scaleY = 0.1f
            alpha = 0f
        }
        appName.alpha = 0f

        // Параллельная анимация масштаба и прозрачности
        val animatorSet = android.animation.AnimatorSet()

        val scaleXAnim = android.animation.ObjectAnimator.ofFloat(logo, "scaleX", 0.1f, 1f)
        val scaleYAnim = android.animation.ObjectAnimator.ofFloat(logo, "scaleY", 0.1f, 1f)
        val alphaAnim = android.animation.ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f)

        scaleXAnim.duration = 1400
        scaleYAnim.duration = 1400
        alphaAnim.duration = 1400

        scaleXAnim.interpolator = android.view.animation.DecelerateInterpolator(1.5f)
        scaleYAnim.interpolator = android.view.animation.DecelerateInterpolator(1.5f)
        alphaAnim.interpolator = android.view.animation.DecelerateInterpolator()

        animatorSet.playTogether(scaleXAnim, scaleYAnim, alphaAnim)
        animatorSet.startDelay = 100
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Анимация текста
                appName.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()

                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@SplashActivity, LauncherActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }, 600)
            }
        })

        animatorSet.start()
    }
}