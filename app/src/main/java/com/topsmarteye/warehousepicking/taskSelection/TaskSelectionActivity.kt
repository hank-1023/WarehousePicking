package com.topsmarteye.warehousepicking.taskSelection

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.integration.android.IntentIntegrator
import com.topsmarteye.warehousepicking.LOGIN_ACTIVITY_REQUEST_CODE
import com.topsmarteye.warehousepicking.backOrder.BackOrderActivity
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.RETRY_DIALOG_REQUEST_CODE
import com.topsmarteye.warehousepicking.stockList.StockListActivity
import com.topsmarteye.warehousepicking.databinding.ActivityTaskSelectionBinding
import com.topsmarteye.warehousepicking.dialog.RetryDialogActivity
import com.topsmarteye.warehousepicking.hideSystemUI
import com.topsmarteye.warehousepicking.network.ApiStatus
import com.topsmarteye.warehousepicking.network.LoginApi
import java.lang.Exception

class TaskSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskSelectionBinding
    private lateinit var integrator: IntentIntegrator
    private lateinit var viewModel: TaskSelectionViewModel
    private lateinit var dialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_task_selection)

        viewModel = ViewModelProviders.of(this)
            .get(TaskSelectionViewModel::class.java)

        setupProgressDialog()
        setupIntegrator()
        setupViewListener()
        setupViewModelListener()
    }


    private fun setupProgressDialog() {
        dialog = ProgressDialog(this)
        dialog.setMessage(getString(R.string.logging_in))
        dialog.setCancelable(false)
    }

    private fun setupIntegrator() {
        integrator = IntentIntegrator(this)
        integrator.captureActivity = LoginBarcodeScanActivity::class.java
        integrator.setRequestCode(LOGIN_ACTIVITY_REQUEST_CODE)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
    }

    private fun setupViewListener() {
        // set the stock picking card as the first focus point
        binding.stockPickingCardViewLayout.requestFocus()

        binding.stockPickingCardViewLayout.setOnClickListener {
            val intent = Intent(this, StockListActivity::class.java)
            startActivity(intent)
        }

        binding.backOrderCardViewLayout.setOnClickListener {
            val intent = Intent(this, BackOrderActivity::class.java)
            startActivity(intent)
        }

        binding.doneLayout.setOnClickListener {
            viewModel.onLogOut()
        }
    }

    private fun setupViewModelListener() {
//        viewModel.isLoggedIn.observe(this, Observer {
//            if (!it) {
//                integrator.initiateScan()
//            }
//        })

        viewModel.displayName.observe(this, Observer {
            binding.staffIDTextView.text = it
        })


        viewModel.apiStatus.observe(this, Observer { status ->
            when (status) {
                ApiStatus.LOADING -> dialog.show()
                ApiStatus.ERROR -> {
                    dismissLoadingDialog()
                    popDialogWithMessage(getString(R.string.network_error_message))
                }
                ApiStatus.DONE -> dismissLoadingDialog()
                else -> return@Observer
            }
        })

        viewModel.eventQRCodeError.observe(this, Observer {
            if (it) {
                popDialogWithMessage(getString(R.string.qrcode_error_message))
                viewModel.onQRCodeErrorComplete()
            }
        })


        LoginApi.isLoggedIn.observe(this, Observer {
            if (!it) {
                integrator.initiateScan()
            }
        })

    }

    private fun popDialogWithMessage(message: String) {
        val intent = Intent(this, RetryDialogActivity::class.java).apply {
            putExtra("dialogTitle", message)
            putExtra("buttonTitle", getString(R.string.relogin))
        }
        startActivityForResult(intent, RETRY_DIALOG_REQUEST_CODE)
    }

    private fun dismissLoadingDialog() {
        dialog.dismiss()
        hideSystemUI()
    }

    override fun onResume() {
        super.onResume()
        // Enter the sticky immersive mode
        hideSystemUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            // Dialog dismissed and should perform logout on viewModel
            RETRY_DIALOG_REQUEST_CODE -> viewModel.onLogOut()
            LOGIN_ACTIVITY_REQUEST_CODE -> parseScannerResult(resultCode, data)
        }
    }

    private fun parseScannerResult(resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(resultCode, data)
        if (result != null) {
            try {
                val loginInfo = result.contents.toString().split(",")
                if (loginInfo.size == 2) {
                    viewModel.getTokenAndLogin(loginInfo[0], loginInfo[1].toInt())
                } else {
                    viewModel.onQRCodeError()
                }
            } catch (e: Exception) {
                Log.d("TaskSelectionActivity", "QR code not well formatted")
                viewModel.onQRCodeError()
            }
        } else {
            viewModel.onQRCodeError()
        }
    }
}