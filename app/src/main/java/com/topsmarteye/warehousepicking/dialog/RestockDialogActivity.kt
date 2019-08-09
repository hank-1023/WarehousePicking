package com.topsmarteye.warehousepicking.dialog

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.DialogRestockBinding
import com.topsmarteye.warehousepicking.hideSystemUI
import java.lang.Exception

class RestockDialogActivity : AppCompatActivity() {
    private lateinit var binding: DialogRestockBinding
    private var maxQuantity: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        maxQuantity = intent.extras!!["maxQuantity"] as Int

        binding = DataBindingUtil.setContentView(this, R.layout.dialog_restock)
        binding.lifecycleOwner = this
        binding.quantityEditText.hint = getString(R.string.maximum_quantity_format, maxQuantity)

        binding.submitButton.setOnClickListener {
            onSubmit()
        }

        binding.quantityEditText.setOnClickListener {
            hideErrorTextView()
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

        try {
            val quantity = binding.quantityEditText.text.toString().toInt()
            if (quantity <= 0 || quantity > maxQuantity) {
                showErrorTextView()
                return
            }

            val intent = Intent().apply {
                putExtra("restockQuantity", quantity)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        } catch (e: Exception) {
            showErrorTextView()
        }
    }

    private fun showErrorTextView() {
        binding.errorMessageTextView.visibility = View.VISIBLE
    }

    private fun hideErrorTextView() {
        binding.errorMessageTextView.visibility = View.GONE
    }

}