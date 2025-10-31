package com.raamgroup.salesrecorder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            if (name.isNotEmpty()) {
                // Save the name to SharedPreferences
                val sharedPrefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    putString("USER_NAME", name)
                    apply()
                }

                // Go to the main activity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Close this activity so the user can't go back to it
            }
        }
    }
}
