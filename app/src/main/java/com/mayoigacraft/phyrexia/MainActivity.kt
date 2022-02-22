package com.mayoigacraft.phyrexia

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

/**
 * 最初のアクティビティ
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onStartButtonClicked(view: View) {
        val intent = Intent(applicationContext, RealtimeOcrActivity::class.java)
        startActivity(intent)
    }
}