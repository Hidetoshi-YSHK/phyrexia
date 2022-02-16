package com.mayoigacraft.phyrexia

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onStartButtonClicked(view: View) {
        toast = Toast.makeText(this, "Hello", Toast.LENGTH_SHORT)
        toast?.show()
    }

    private var toast:Toast? = null
}