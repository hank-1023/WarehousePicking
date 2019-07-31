package com.topsmarteye.warehousepicking

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.hideSystemUI() {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            // Set the content to appear under the system bars so that the
            // content doesn't resize when the system bars hide and show.
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            // Hide the nav bar and status bar
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
}


const val RESTOCK_DIALOG_REQUEST_CODE = 0
const val OUT_OF_STOCK_DIALOG_REQUEST_CODE = 1
const val RESET_ORDER_DIALOG_REQUEST_CODE = 2
const val BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE = 3
const val BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE = 4
const val RETRY_DIALOG_REQUEST_CODE = 5
const val LOGIN_ACTIVITY_REQUEST_CODE = 6
