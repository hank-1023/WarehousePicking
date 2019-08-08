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
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentBackOrderInputAndScanBinding
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.popRetryDialog


class BackOrderInputAndScanFragment : Fragment() {

    private lateinit var binding: FragmentBackOrderInputAndScanBinding
    private lateinit var viewModel: BackOrderViewModel
    private lateinit var integrator: IntentIntegrator


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(BackOrderViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        setupBarcodeIntegrator()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_back_order_input_and_scan, container, false)
        binding.lifecycleOwner = this

        setListeners()

        return binding.root
    }

    private fun setupBarcodeIntegrator() {
        integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
    }

    private fun setListeners() {

        binding.orderIDEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus)
                hideStockIDEditGroup()
        }

        binding.orderIDScanButton.setOnClickListener {
            hideStockIDEditGroup()
            initiateOrderNumberScan()
        }

        binding.stockIDScanButton.setOnClickListener {
            initiateStockNumberScan()
        }

        binding.orderIDEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (textView.text.isEmpty() || textView.text == null) {
                false
            } else if (i == EditorInfo.IME_ACTION_DONE
                || (keyEvent.action == KeyEvent.ACTION_DOWN
                        && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {

                viewModel.setOrderNumber(textView.text.toString())
                false
            } else {
                false
            }
        }

        binding.stockIDEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (textView.text.isEmpty() || textView.text == null) {
                false
            } else if (i == EditorInfo.IME_ACTION_DONE
                || (keyEvent.action == KeyEvent.ACTION_DOWN
                        && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                viewModel.setStockBarcode(textView.text.toString())
                false
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

        viewModel.eventLoadOrderSuccess.observe(viewLifecycleOwner, Observer {
            if (it) {
                showStockIDEditGroup()
                // change focus to stockIDEditText
                binding.stockIDEditText.requestFocus()
                viewModel.onLoadOrderSuccessComplete()
            }
        })

        viewModel.inputApiStatus.observe(viewLifecycleOwner, Observer {status ->
            when(status) {
                ApiStatus.LOADING -> binding.progressBar.visibility = View.VISIBLE
                ApiStatus.DONE -> binding.progressBar.visibility = View.GONE
                ApiStatus.ERROR -> {
                    popRetryDialog(getString(R.string.get_order_error_message), getString(R.string.retry))
                    binding.progressBar.visibility = View.GONE
                }
                else -> return@Observer
            }
        })

        viewModel.eventStockBarcodeNotFound.observe(viewLifecycleOwner, Observer {
            if (it) {
                popRetryDialog(getString(R.string.stock_barcode_not_found_message), getString(R.string.retry))
                viewModel.onStockBarcodeNotFoundComplete()
            }
        })

        viewModel.eventNavigateToSubmit.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(BackOrderInputAndScanFragmentDirections
                    .actionBackOrderInputAndScanFragmentToBackOrderSubmitFragment())
                viewModel.onNavigateToSubmitComplete()
            }
        })

    }

    private fun initiateOrderNumberScan() {
        integrator.setPrompt(getString(R.string.please_scan_order_barcode))
        integrator.setRequestCode(BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
    }

    private fun initiateStockNumberScan() {
        integrator.setPrompt(getString(R.string.please_scan_stock_barcode))
        integrator.setRequestCode(BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE)
        integrator.initiateScan()
    }

    private fun hideStockIDEditGroup() {
        //clear the text on hide
        binding.stockIDEditText.text.clear()

        binding.stockIDLabel.visibility = View.GONE
        binding.stockIDEditText.visibility = View.GONE
        binding.stockIDScanButton.visibility = View.GONE
    }

    private fun showStockIDEditGroup() {
        binding.stockIDLabel.visibility = View.VISIBLE
        binding.stockIDEditText.visibility = View.VISIBLE
        binding.stockIDScanButton.visibility = View.VISIBLE
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(resultCode, data)
        // doesn't do anything if result is empty
        if (result == null || result.contents.isNullOrEmpty()) {return}

        when (requestCode) {
            BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE -> {
                binding.orderIDEditText.setText(result.contents)
                viewModel.setOrderNumber(result.contents)
            }
            BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE -> {
                binding.stockIDEditText.setText(result.contents)
                viewModel.setStockBarcode(result.contents)
            }
        }
    }
}