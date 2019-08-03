package com.topsmarteye.warehousepicking.backOrder

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.ActivityBackOrderBinding
import com.topsmarteye.warehousepicking.hideSystemUI

class BackOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackOrderBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: BackOrderViewModel
//    private lateinit var gestureDetector: GestureDetector
    private var isStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_back_order)
        viewModel = ViewModelProviders.of(this).get(BackOrderViewModel::class.java)

        navController = findNavController(R.id.backOrderNavHostFragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isStart = destination.id == navController.graph.startDestination
        }

        binding.upButton.setOnClickListener {
            onUpButtonPressed()
        }

//        gestureDetector = GestureDetector(this, FlingGestureListener())
    }

//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        gestureDetector.onTouchEvent(event)
//        return super.onTouchEvent(event)
//    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStart && keyCode == KeyEvent.KEYCODE_STAR) {
            viewModel.onKeyboardScan()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


    override fun onResume() {
        super.onResume()
        // Enter the sticky immersive mode
        hideSystemUI()
    }


//    private inner class FlingGestureListener : GestureDetector.SimpleOnGestureListener() {
//        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
//            viewModel.onKeyboardScan()
//            return true
//        }
//
//    }

    private fun onUpButtonPressed() {
        if (isStart) {
            finish()
        } else {
            navController.navigateUp()
        }
    }
}