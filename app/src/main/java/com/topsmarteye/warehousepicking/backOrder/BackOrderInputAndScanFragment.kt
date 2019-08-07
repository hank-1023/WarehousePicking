package com.topsmarteye.warehousepicking.backOrder

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentBackOrderInputAndScanBinding
import com.topsmarteye.warehousepicking.setupBarcodeIntegrator


class BackOrderInputAndScanFragment : Fragment() {

    private lateinit var binding: FragmentBackOrderInputAndScanBinding
    private lateinit var viewModel: BackOrderViewModel
    private lateinit var integrator: IntentIntegrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(BackOrderViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        integrator = setupBarcodeIntegrator()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_back_order_input_and_scan, container, false)
        binding.lifecycleOwner = this


        binding.stockIDGroup.visibility = View.GONE

        setListeners()

        return binding.root
    }

    private fun setListeners() {
        binding.orderIDScanButton.setOnClickListener {
            viewModel.onOrderIDEdit()
            initiateOrderNumberScan()
        }

        binding.stockIDScanButton.setOnClickListener {
            viewModel.onStockIDEdit()
            initiateStockNumberScan()
        }

        // hide the stockNumberGroup when editing order number
        binding.orderIDEditText.setOnClickListener {
            viewModel.onOrderIDEdit()
        }

        binding.orderIDEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (textView.text.isEmpty() || textView.text == null) {
                false
            } else if (i == EditorInfo.IME_ACTION_DONE
                || (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                viewModel.onSetOrderNumber(textView.text.toString())
                viewModel.onOrderIDEditComplete()
                true
            } else {
                false
            }
        }

        viewModel.eventKeyboardScan.observe(viewLifecycleOwner, Observer {
            if (it) {
                if (activity?.currentFocus == binding.orderIDEditText
                    || activity?.currentFocus == binding.orderIDScanButton
                ) {
                    binding.orderIDScanButton.requestFocus()
                    binding.orderIDScanButton.performClick()
                    viewModel.onKeyboardScanComplete()
                } else if (activity?.currentFocus == binding.stockIDEditText
                    || activity?.currentFocus == binding.stockIDScanButton
                ) {
                    binding.stockIDScanButton.requestFocus()
                    binding.stockIDScanButton.performClick()
                    viewModel.onKeyboardScanComplete()
                }
            }
        })

        // hide the stockNumberGroup whenever edit begins
        viewModel.eventOrderIDEdit.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.stockIDGroup.visibility = View.GONE
            }
        })

        viewModel.eventLoadOrderSuccess.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.stockIDEditText.text.clear()
                binding.stockIDGroup.visibility = View.VISIBLE
                viewModel.onLoadOrderSuccessComplete()
            }
        })

    }

    private fun initiateOrderNumberScan() {
        integrator.setRequestCode(BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
    }

    private fun initiateStockNumberScan() {
        integrator.setRequestCode(BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(resultCode, data)
        // doesn't do anything if result is empty
        if (result == null || result.contents.isNullOrEmpty()) {return}

        when (requestCode) {
            BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE -> {
                binding.orderIDEditText.setText(result.contents)
                binding.stockIDEditText.requestFocus()
                viewModel.onSetOrderNumber(result.contents)
                viewModel.onOrderIDEditComplete()
            }
            BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE -> {
                binding.stockIDEditText.setText(result.contents)
                binding.stockIDEditText.requestFocus()
                viewModel.onSetStockNumber(result.contents)
                viewModel.onStockIDScanEditComplete()
            }
        }
    }
}