package com.topsmarteye.warehousepicking.dialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.DialogRetryBinding
import com.topsmarteye.warehousepicking.hideSystemUI

class RetryDialogActivity : AppCompatActivity() {
    private lateinit var binding: DialogRetryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.dialog_retry)
        binding.lifecycleOwner = this

        binding.titleTextView.text = intent.extras!!["dialogTitle"] as String
        binding.retryButton.text = intent.extras!!["buttonTitle"] as String
        binding.retryButton.setOnClickListener {
            onSubmit()
        }

        hideSystemUI()

    }

    override fun onBackPressed() {
        finish()
    }

    private fun onSubmit() {
        finish()
    }
}