package com.topsmarteye.warehousepicking.dialog

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.DialogRestockBinding
import com.topsmarteye.warehousepicking.hideSystemUI

class RestockDialogActivity : AppCompatActivity() {
    private lateinit var binding: DialogRestockBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.dialog_restock)
        binding.lifecycleOwner = this

        binding.submitButton.setOnClickListener {
            onSubmit()
        }


        hideSystemUI()

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_POUND -> {
                binding.submitButton.requestFocus()
                onSubmit()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    private fun onSubmit() {
        setResult(Activity.RESULT_OK)
        finish()
    }

}