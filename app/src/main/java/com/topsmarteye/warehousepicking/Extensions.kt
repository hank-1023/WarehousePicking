package com.topsmarteye.warehousepicking

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.lang.Exception
import java.util.*

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

fun String.formatToStandardDateString(): String? {
    var formattedString: String? = null

    val fmtWithSeconds = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
        .withLocale(Locale.CHINA)
    val fmtNoSecond = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.CHINA)

    try {
        val dt = fmtWithSeconds.parseDateTime(this)
            .withZone(DateTimeZone.forID("Asia/Shanghai"))
        formattedString = dt.toString()
    } catch (e: Exception) {
        try {
            val dt = fmtNoSecond.parseDateTime(this)
                .withZone(DateTimeZone.forID("Asia/Shanghai"))
            formattedString = dt.toString()
        } catch (e: Exception) {
            Log.d("formatDateString", "Unknown date format ${e.message}")
        }
    } finally {
        return formattedString
    }
}

fun getCurrentTimeString(): String {
    val dt = DateTime()
    return dt.toString()
}


const val RESTOCK_DIALOG_REQUEST_CODE = 0
const val OUT_OF_STOCK_DIALOG_REQUEST_CODE = 1
const val RESET_ORDER_DIALOG_REQUEST_CODE = 2
const val BACK_ORDER_ORDER_NUMBER_SCAN_REQUEST_CODE = 3
const val BACK_ORDER_STOCK_NUMBER_SCAN_REQUEST_CODE = 4
const val RETRY_DIALOG_REQUEST_CODE = 5
const val LOGIN_ACTIVITY_REQUEST_CODE = 6
