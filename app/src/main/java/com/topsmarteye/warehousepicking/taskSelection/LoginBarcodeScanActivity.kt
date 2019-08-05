package com.topsmarteye.warehousepicking.taskSelection

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.ActivityBarcodeScanBinding
import com.topsmarteye.warehousepicking.hideSystemUI
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView

class LoginBarcodeScanActivity : AppCompatActivity() {

    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView

    private lateinit var binding: ActivityBarcodeScanBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_barcode_scan)
        binding.lifecycleOwner = this

        barcodeScannerView = binding.decoratedBarcodeView

        capture = CaptureManager(this, barcodeScannerView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

}