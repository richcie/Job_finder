package com.uilover.project196.Activity

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.uilover.project196.Fragment.LoginFragment
import com.uilover.project196.Fragment.SignupFragment
import com.uilover.project196.R
import com.uilover.project196.Utils.UserSession
import com.uilover.project196.databinding.ActivityLoginBinding

// KRITERIA: Multiple Activity (2/8) - Activity untuk login pengguna
// KRITERIA WAJIB: Multiple Activity (2/8) - Activity untuk login dan autentikasi pengguna
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        UserSession.init(this)


        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )


        if (savedInstanceState == null) {
            val startWithSignup = intent.getBooleanExtra("START_WITH_SIGNUP", false)
            val sourceScreen = intent.getStringExtra("SOURCE_SCREEN") ?: "intro"

            val fragment = if (startWithSignup) {
                SignupFragment.newInstance(sourceScreen)
            } else {
                LoginFragment.newInstance(sourceScreen)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }
}