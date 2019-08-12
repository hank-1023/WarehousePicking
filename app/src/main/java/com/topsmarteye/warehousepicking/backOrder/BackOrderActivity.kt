package com.topsmarteye.warehousepicking.backOrder

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.ActivityBackOrderBinding
import com.topsmarteye.warehousepicking.hideSystemUI
import com.topsmarteye.warehousepicking.network.networkServices.LoginService

class BackOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackOrderBinding
    private lateinit var navController: NavController
    private lateinit var viewModel: BackOrderViewModel
    private var isStart = true
    private var isInteractionDisabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_back_order)
        binding.lifecycleOwner = this

        viewModel = ViewModelProviders.of(this).get(BackOrderViewModel::class.java)

        navController = findNavController(R.id.backOrderNavHostFragment)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isStart = destination.id == navController.graph.startDestination
        }

        binding.upButton.setOnClickListener {
            if (isStart) {
                finish()
            } else {
                navController.navigateUp()
                // This will be handled in input fragment
                viewModel.onNavigateToInput()
            }
        }

        viewModel.eventDisableInteraction.observe(this, Observer {
            isInteractionDisabled = it
        })

        // Finish the activity if user is not logged in
        LoginService.isLoggedIn.observe(this, Observer {
            if (!it) {
                finish()
            }
        })

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (isStart && !isInteractionDisabled && keyCode == KeyEvent.KEYCODE_STAR) {
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

    override fun onBackPressed() {
        binding.upButton.performClick()
    }

}