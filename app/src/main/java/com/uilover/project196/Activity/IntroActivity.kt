package com.uilover.project196.Activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.uilover.project196.databinding.ActivityIntroBinding

// KRITERIA: Multiple Activity (3/8) - Activity intro/splash screen
// KRITERIA WAJIB: Multiple Activity (3/8) - Activity intro/splash screen
class IntroActivity : AppCompatActivity() {
    lateinit var binding: ActivityIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.goBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.putExtra("SOURCE_SCREEN", "intro")
            startActivity(intent)
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

    }
}