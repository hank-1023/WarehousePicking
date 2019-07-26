package com.topsmarteye.warehousepicking.dialog

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.DialogReloginBinding
import com.topsmarteye.warehousepicking.hideSystemUI

class ReloginDialogActivity : AppCompatActivity() {
    private lateinit var binding: DialogReloginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.dialog_relogin)
        binding.titleTextView.text = intent.extras!!["dialogTitle"] as String
        binding.reloginButton.setOnClickListener {
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