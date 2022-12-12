package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_tela_login.*

class tela_login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_tela_login)

        bottom_menu.setOnClickListener { view ->
            menu()
        }
    }

    private fun menu() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}