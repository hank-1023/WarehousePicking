package com.topsmarteye.warehousepicking.stockList

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentStockListInputAndScanBinding
import com.topsmarteye.warehousepicking.dialog.RetryDialogActivity
import com.topsmarteye.warehousepicking.network.ApiStatus


class StockListInputAndScanFragment : Fragment() {
    private lateinit var binding: FragmentStockListInputAndScanBinding
    private lateinit var integrator: IntentIntegrator
    private lateinit var viewModel: StockListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list_input_and_scan, container, false)

        setupViewModel()
        setupIntegrator()
        setViewListeners()

        return binding.root
    }

    private fun setupViewModel() {
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.eventScan.observe(this, Observer {
            if (it) {
                binding.scanButton.requestFocus()
                binding.scanButton.performClick()
                viewModel.onScanComplete()
            }
        })

        viewModel.apiStatus.observe(this, Observer {
            when (it) {
                ApiStatus.LOADING -> startLoadingAnimation()
                ApiStatus.DONE -> {
                    stopLoadingAnimation()
                    viewModel.onNavigation()
                }
                ApiStatus.ERROR -> {
                    stopLoadingAnimation()
                    popDialogWithMessage(getString(R.string.get_order_error_message))
                }
                else -> return@Observer
            }
        })

        viewModel.eventNavigateToList.observe(this, Observer {
            if (it) {
                findNavController().navigate(StockListInputAndScanFragmentDirections
                    .actionStockListInputAndScanFragmentToStockListFragment())
                viewModel.onNavigationComplete()
            }
        })
    }

    private fun startLoadingAnimation() {
        binding.orderNumberEditText.isEnabled = false
        binding.scanButton.isEnabled = false
        binding.loadingProgressGroup.visibility = View.VISIBLE
    }

    private fun stopLoadingAnimation() {
        binding.orderNumberEditText.isEnabled = true
        binding.scanButton.isEnabled = true
        binding.loadingProgressGroup.visibility = View.GONE
    }

    private fun setupIntegrator() {
        integrator = IntentIntegrator.forSupportFragment(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES)
        integrator.setPrompt(getString(R.string.please_scan_barcode))
    }

    private fun setViewListeners() {
        binding.scanButton.setOnClickListener {
            integrator.initiateScan()
        }

        // EditText listener
        binding.orderNumberEditText.setOnEditorActionListener { textView, i, keyEvent ->
            if (textView.text.isEmpty() || textView.text == null) {
                false
            } else if (i == EditorInfo.IME_ACTION_DONE
                || (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
            ) {
                retrieveOrder(textView.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun popDialogWithMessage(message: String) {
        val intent = Intent(context, RetryDialogActivity::class.java).apply {
            putExtra("dialogTitle", message)
            putExtra("buttonTitle", getString(R.string.retry))
        }
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            binding.orderNumberEditText.setText(result.contents)
            retrieveOrder(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun retrieveOrder(orderNumber: String) {
        viewModel.loadStockList(orderNumber)
    }
}