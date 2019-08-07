package com.topsmarteye.warehousepicking.stockList

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.topsmarteye.warehousepicking.setupBarcodeIntegrator


class StockListInputAndScanFragment : Fragment() {
    private lateinit var binding: FragmentStockListInputAndScanBinding
    private lateinit var integrator: IntentIntegrator
    private lateinit var viewModel: StockListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        integrator = setupBarcodeIntegrator()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list_input_and_scan, container, false)
        binding.lifecycleOwner = this

        setupViewModelListeners()
        setupViewListeners()

        return binding.root
    }

    private fun setupViewModelListeners() {

        viewModel.eventInputFragmentKeyboardScan.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.scanButton.performClick()
                viewModel.onInputFragmentKeyboardScanComplete()
            }
        })

        viewModel.inputFragmentApiStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                ApiStatus.LOADING -> {
                    disableInteraction()
                    startLoadingAnimation()
                }
                ApiStatus.DONE -> {
                    stopLoadingAnimation()
                    viewModel.onNavigationToList()
                }
                ApiStatus.ERROR -> {
                    stopLoadingAnimation()
                    enableInteraction()
                    popDialogWithMessage(getString(R.string.get_order_error_message))
                }
                else -> return@Observer
            }
        })

        viewModel.eventNavigateToList.observe(viewLifecycleOwner, Observer {
            if (it) {
                findNavController().navigate(StockListInputAndScanFragmentDirections
                    .actionStockListInputAndScanFragmentToStockListFragment())
                viewModel.onNavigationToListComplete()
            }
        })
    }

    private fun startLoadingAnimation() {
        binding.loadingProgressGroup.visibility = View.VISIBLE
    }

    private fun stopLoadingAnimation() {
        binding.loadingProgressGroup.visibility = View.GONE
    }

    private fun disableInteraction() {
        binding.orderIDEditText.isEnabled = false
        binding.scanButton.isClickable = false
    }

    private fun enableInteraction() {
        binding.orderIDEditText.isEnabled = true
        binding.scanButton.isClickable = true
    }

    private fun setupViewListeners() {
        binding.scanButton.setOnClickListener {
            if (it.isClickable) {
                it.requestFocus()
                integrator.initiateScan()
            }
        }


        binding.orderIDEditText.setOnEditorActionListener { textView, i, keyEvent ->
            Log.d("orderIDEditText", "$i")


            if (textView.text.isNullOrEmpty()) {
                false
            } else if (i == EditorInfo.IME_ACTION_DONE ||
                (keyEvent.action == KeyEvent.ACTION_DOWN && keyEvent.keyCode == KeyEvent.KEYCODE_ENTER)) {
                viewModel.loadStockList(textView.text.toString())
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
            binding.orderIDEditText.setText(result.contents)
            viewModel.loadStockList(result.contents)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}