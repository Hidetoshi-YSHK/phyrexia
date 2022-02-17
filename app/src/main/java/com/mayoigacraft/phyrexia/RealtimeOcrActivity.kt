package com.mayoigacraft.phyrexia

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mayoigacraft.phyrexia.databinding.ActivityRealtimeOcrBinding

class RealtimeOcrActivity : AppCompatActivity() {

    // View binding
    private lateinit var viewBinding: ActivityRealtimeOcrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityRealtimeOcrBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
    }
}