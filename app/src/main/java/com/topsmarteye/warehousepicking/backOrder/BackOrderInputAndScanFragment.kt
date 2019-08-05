package com.topsmarteye.warehousepicking.backOrder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentBackOrderInputAndScanBinding


class BackOrderInputAndScanFragment : Fragment() {

    private lateinit var binding: FragmentBackOrderInputAndScanBinding
    private lateinit var viewModel: BackOrderViewModel
    private lateinit var integrator: IntentIntegrator


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_back_order_input_and_scan, container, false)
        binding.lifecycleOwner = this

        setupViewModel()
        setViewListeners()

        return binding.root
    }

    private fun setViewListeners() {
        binding.orderNumberScanButton.setOnClickListener {
            initiateOrderNumberScan()
        }

        binding.stockNumberScanButton.setOnClickListener {
            initiateStockNumberScan()
        }
    }

    private fun setupViewModel() {
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(BackOrderViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        setupIntegrator()

        viewModel.eventScan.observe(this, Observer {
            if (it) {
                if (activity?.currentFocus == binding.orderNumberEditText
                    || activity?.currentFocus == binding.orderNumberScanButton
                ) {
                    binding.orderNumberScanButton.requestFocus()
                    binding.orderNumberScanButton.performClick()
                } else if (activity?.currentFocus == binding.stockNumberEditText
                    || activity?.currentFocus == binding.stockNumberScanButton
                ) {
                    binding.stockNumberScanButton.requestFocus()
                    binding.stockNumberScanButton.performClick()
                }
            }
        })
    }

    private fun setupIntegrator() {
        integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
        integrator.setPrompt(getString(R.string.please_scan_barcode))
    }

    private fun initiateOrderNumberScan() {
        integrator.setRequestCode(BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
        viewModel.onKeyboardScanComplete()
    }

    private fun initiateStockNumberScan() {
        integrator.setRequestCode(BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
        viewModel.onKeyboardScanComplete()
    }

    private fun clearText(view: View) {
        val editText = view as EditText
        editText.text.clear()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(resultCode, data)
        if (result == null || result.contents == null) {return}

        when (requestCode) {
            BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE -> {
                binding.orderNumberEditText.setText(result.contents)
                binding.stockNumberEditText.requestFocus()
            }
            BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE -> {
                binding.stockNumberEditText.setText(result.contents)
                binding.stockNumberEditText.requestFocus()
            }
        }
    }
}