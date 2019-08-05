package com.topsmarteye.warehousepicking.backOrder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.topsmarteye.warehousepicking.R
import com.topsmarteye.warehousepicking.databinding.FragmentBackOrderSubmitBinding

class BackOrderSubmitFragment : Fragment() {

    private lateinit var binding: FragmentBackOrderSubmitBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_back_order_submit, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

}