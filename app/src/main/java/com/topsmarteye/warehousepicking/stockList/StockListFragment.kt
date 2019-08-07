package com.topsmarteye.warehousepicking.stockList

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.*
import com.topsmarteye.warehousepicking.dialog.RestockDialogActivity
import com.topsmarteye.warehousepicking.dialog.YesNoDialogActivity
import com.topsmarteye.warehousepicking.databinding.FragmentStockListBinding
import com.topsmarteye.warehousepicking.dialog.RetryDialogActivity
import com.topsmarteye.warehousepicking.network.ApiStatus


class StockListFragment : Fragment() {

    private lateinit var binding: FragmentStockListBinding
    private lateinit var stockListViewModel: StockListViewModel
    private lateinit var integrator: IntentIntegrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get shared viewModel
        stockListViewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        integrator = setupBarcodeIntegrator()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list, container, false)
        binding.lifecycleOwner = this

        binding.stockListViewModel = stockListViewModel

        setupListeners()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Start marquee of textView when resume
        binding.nameTextView.isSelected = true
    }

    private fun setupListeners() {

        stockListViewModel.isLastItem.observe(viewLifecycleOwner, Observer { isLastItem ->
            if (isLastItem) {
                binding.nextItemCardView.visibility = View.GONE
            } else {
                binding.nextItemCardView.visibility = View.VISIBLE
            }
        })

        // Doesn't need to think about clickable, since keyboard will be disabled when appropriate

        stockListViewModel.eventNext.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.nextButton.requestFocus()
                integrator.setRequestCode(STOCK_LIST_NEXT_SCAN_REQUEST_CODE)
                integrator.initiateScan()
            }
        })

        stockListViewModel.eventRestock.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.needsRestockingButton.requestFocus()
                integrator.setRequestCode(STOCK_LIST_RESTOCK_SCAN_REQUEST_CODE)
                integrator.initiateScan()
            }
        })

        stockListViewModel.eventOutOfStock.observe(viewLifecycleOwner, Observer {
            if (it) {
                binding.outOfStockButton.requestFocus()
                popOutOfStock()
            }
        })

        stockListViewModel.eventResetOrder.observe(viewLifecycleOwner, Observer {
            if (it) {
                popResetOrder()
            }
        })

        stockListViewModel.currentIndex.observe(viewLifecycleOwner, Observer {
            // Whenever item changes, marquee for current item textView will start
            binding.nameTextView.isSelected = true
        })

        stockListViewModel.eventFinishActivity.observe(viewLifecycleOwner, Observer {
            if (it) {
                stockListViewModel.onFinishActivityComplete()
                activity?.finish()
            }
        })

        stockListViewModel.eventDisableControl.observe(viewLifecycleOwner, Observer {
            if (it) {
                disableControlButtons()
            } else {
                enableControlButtons()
            }
        })

        stockListViewModel.eventDateFormatError.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                popDateFormatError()
                stockListViewModel.onDateFormatErrorComplete()
            }
        })

        stockListViewModel.listFragmentApiStatus.observe(viewLifecycleOwner, Observer {
            when (it!!) {
                ApiStatus.LOADING -> {
                    startAnimation()
                    stockListViewModel.onDisableControl()
                }
                ApiStatus.ERROR -> {
                    popUpdateError()
                }
                ApiStatus.DONE -> {
                    return@Observer
                }
                ApiStatus.NONE -> {
                    stopAnimation()
                    stockListViewModel.onDisableControlComplete()
                }
            }
        })

        stockListViewModel.eventBarcodeConfirmError.observe(viewLifecycleOwner, Observer {
            if (it) {
                popBarcodeConfirmError()
                // Don't care about the result, so complete here
                stockListViewModel.onBarcodeConfirmErrorComplete()
            }
        })

        stockListViewModel.eventOrderReloaded.observe(viewLifecycleOwner, Observer {
            if (it) {
                Toast.makeText(context, getString(R.string.order_reloaded), Toast.LENGTH_SHORT).show()
                stockListViewModel.onOrderReloadedComplete()
            }
        })
    }

    private fun disableControlButtons() {
        binding.nextButton.isEnabled = false
        binding.needsRestockingButton.isClickable = false
        binding.outOfStockButton.isClickable = false
    }

    private fun enableControlButtons() {
        binding.nextButton.isEnabled = true
        binding.needsRestockingButton.isClickable = true
        binding.outOfStockButton.isClickable = true
    }

    private fun startAnimation() {
        binding.submitProgressBar.visibility = View.VISIBLE
        binding.submitTextView.visibility = View.VISIBLE
    }

    private fun stopAnimation() {
        binding.submitProgressBar.visibility = View.GONE
        binding.submitTextView.visibility = View.GONE
    }

    private fun popRestock() {
        val intent = Intent(context, RestockDialogActivity::class.java).apply {
            putExtra("maxQuantity", stockListViewModel.currentItem.value!!.quantity)
        }
        startActivityForResult(intent, RESTOCK_DIALOG_REQUEST_CODE)
    }

    private fun popOutOfStock() {
        val intent = Intent(context, YesNoDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.confirm_out_of_stock))
        }
        startActivityForResult(intent, OUT_OF_STOCK_DIALOG_REQUEST_CODE)
    }

    private fun popResetOrder() {
        val intent = Intent(context, YesNoDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.confirm_reset_order))
        }
        startActivityForResult(intent, RESET_ORDER_DIALOG_REQUEST_CODE)
    }

    private fun popUpdateError() {
        val intent = Intent(context, RetryDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.network_error_message))
            putExtra("buttonTitle", getString(R.string.retry))
        }
        startActivity(intent)
    }

    private fun popDateFormatError() {
        val intent = Intent(context, RetryDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.date_formatting_error_message))
            putExtra("buttonTitle", getString(R.string.ignore))
        }
        startActivity(intent)
    }

    private fun popBarcodeConfirmError() {
        val intent = Intent(context, RetryDialogActivity::class.java).apply {
            putExtra("dialogTitle", getString(R.string.barcode_confirm_error_message))
            putExtra("buttonTitle", getString(R.string.retry))
        }
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            STOCK_LIST_NEXT_SCAN_REQUEST_CODE -> {
                val result = IntentIntegrator.parseActivityResult(resultCode, data)
                stockListViewModel.onNextComplete(result)
            }
            STOCK_LIST_RESTOCK_SCAN_REQUEST_CODE -> {
                val result = IntentIntegrator.parseActivityResult(resultCode, data)
                if (stockListViewModel.confirmBarcodeFromScanResult(result)) {
                    popRestock()
                } else {
                    stockListViewModel.onBarcodeConfirmError()
                    stockListViewModel.onRestockComplete(null)
                }
            }

            RESTOCK_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    val quantity = data!!.extras!!["restockQuantity"] as Int
                    stockListViewModel.onRestockComplete(quantity)
                } else {
                    stockListViewModel.onRestockComplete(null)
                }
            }
            OUT_OF_STOCK_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    stockListViewModel.onOutOfStockComplete(false)
                } else {
                    stockListViewModel.onOutOfStockComplete(true)
                }
            }
            RESET_ORDER_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    stockListViewModel.onResetOrderComplete(false)
                } else {
                    stockListViewModel.onResetOrderComplete(true)
                }
            }
        }
    }

}