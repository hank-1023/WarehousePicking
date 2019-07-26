package com.topsmarteye.warehousepicking.dialog

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.DialogYesnoBinding
import com.topsmarteye.warehousepicking.hideSystemUI

class YesNoDialogActivity : AppCompatActivity() {
    private lateinit var binding: DialogYesnoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.dialog_yesno)
        binding.titleTextView.text = intent.extras!!["dialogTitle"] as String

        binding.yesButton.setOnClickListener {
            onOK()
        }

        binding.noButton.setOnClickListener {
            onCancelled()
        }

        hideSystemUI()

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_1 -> {
                binding.noButton.requestFocus()
                onCancelled()
            }
            KeyEvent.KEYCODE_2 -> {
                binding.yesButton.requestFocus()
                onOK()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        onCancelled()
    }

    private fun onOK() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onCancelled() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

}