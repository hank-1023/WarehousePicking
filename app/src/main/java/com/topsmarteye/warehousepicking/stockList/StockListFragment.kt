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
import com.topsmarteye.warehousepicking.OUT_OF_STOCK_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.RESET_ORDER_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.RESTOCK_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.dialog.RestockDialogActivity
import com.topsmarteye.warehousepicking.dialog.YesNoDialogActivity
import com.topsmarteye.warehousepicking.databinding.FragmentStockListBinding
import com.topsmarteye.warehousepicking.dialog.RetryDialogActivity
import com.topsmarteye.warehousepicking.network.ApiStatus


class StockListFragment : Fragment() {

    private lateinit var binding: FragmentStockListBinding
    private lateinit var stockListViewModel: StockListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_stock_list, container, false)
        binding.lifecycleOwner = this

        // Get shared viewModel
        stockListViewModel = activity?.run {
            ViewModelProviders.of(this).get(StockListViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

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

        stockListViewModel.isLastItem.observe(this, Observer { isLastItem ->
            if (isLastItem) {
                binding.nextItemCardView.visibility = View.GONE
                binding.finishOrderButton.visibility = View.VISIBLE
                binding.nextButton.visibility = View.GONE

                binding.finishOrderButton.requestFocus()
            }
        })

        // Doesn't need to think about clickable, since keyboard will be disabled when appropriate

        stockListViewModel.eventNext.observe(this, Observer {
            if (it) {
                binding.nextButton.requestFocus()
                stockListViewModel.onNextComplete()
            }
        })

        stockListViewModel.eventRestock.observe(this, Observer {
            if (it) {
                binding.needsRestockingButton.requestFocus()
                popRestock()
            }
        })

        stockListViewModel.eventOutOfStock.observe(this, Observer {
            if (it) {
                binding.outOfStockButton.requestFocus()
                popOutOfStock()
            }
        })

        stockListViewModel.eventResetOrder.observe(this, Observer {
            if (it) {
                popResetOrder()
            }
        })

        stockListViewModel.currentIndex.observe(this, Observer {
            // Whenever item changes, marquee for current item textView will start
            binding.nameTextView.isSelected = true
        })

        stockListViewModel.eventFinishOrder.observe(this, Observer {
            if (it) {
                if (binding.finishOrderButton.visibility == View.VISIBLE) {
                    binding.finishOrderButton.requestFocus()
                }
                stockListViewModel.onFinishOrderComplete()
            }
        })

        stockListViewModel.eventFinishActivity.observe(this, Observer {
            if (it) {
                stockListViewModel.onFinishActivityComplete()
                activity?.finish()
            }
        })

        stockListViewModel.eventDisableControl.observe(this, Observer {
            if (it) {
                disableControlButtons()
            } else {
                enableControlButtons()
            }
        })

        stockListViewModel.eventDateFormatError.observe(this, Observer {
            if (it != null) {
                popDateFormatError()
                stockListViewModel.onDateFormatErrorComplete()
            }
        })

        stockListViewModel.listFragmentApiStatus.observe(this, Observer {
            when (it!!) {
                ApiStatus.LOADING -> {
                    binding.submitGroup.visibility = View.VISIBLE
                    stockListViewModel.onDisableControl()
                }
                ApiStatus.ERROR -> {
                    binding.submitGroup.visibility = View.GONE
                    popUpdateError()
                }
                ApiStatus.DONE -> {
                    binding.submitGroup.visibility = View.GONE
                }
                ApiStatus.NONE -> stockListViewModel.onDisableControlComplete()
            }
        })
    }

    private fun disableControlButtons() {
        binding.nextButton.isEnabled = false
        binding.needsRestockingButton.isClickable = false
        binding.outOfStockButton.isClickable = false
        binding.finishOrderButton.isClickable = false
    }

    private fun enableControlButtons() {
        binding.nextButton.isEnabled = true
        binding.needsRestockingButton.isClickable = true
        binding.outOfStockButton.isClickable = true
        binding.finishOrderButton.isClickable = true
    }

    private fun popRestock() {
        val intent = Intent(context, RestockDialogActivity::class.java)
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
            putExtra("dialogTitle", getString(R.string.date_formatting_error))
            putExtra("buttonTitle", getString(R.string.ignore))
        }
        startActivity(intent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            RESTOCK_DIALOG_REQUEST_CODE -> stockListViewModel.onRestockComplete()
            OUT_OF_STOCK_DIALOG_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    stockListViewModel.onOutOfStockComplete(false)
                } else {
                    stockListViewModel.onOutOfStockComplete(true)
                }
            }
            RESET_ORDER_DIALOG_REQUEST_CODE -> stockListViewModel.onResetOrderComplete()
        }

        when (resultCode) {
            Activity.RESULT_CANCELED -> Toast.makeText(context, "cancelled", Toast.LENGTH_SHORT).show()
            Activity.RESULT_OK -> Toast.makeText(context, "confirmed", Toast.LENGTH_SHORT).show()
        }
    }

}