package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.databinding.FragmentPermissionRetryBinding
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionRetryFragment:  Fragment() {
    private lateinit var binding: FragmentPermissionRetryBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPermissionRetryBinding.inflate(inflater, container, false)
        lifecycleScope.launch(Dispatchers.IO) {
            delay(THREE_SEC_DELAY)
            withContext(Dispatchers.Main) {
                binding.cstToast.isVisible = false
            }
        }
        initClickAction()
        return binding.root
    }

    private fun initClickAction() {
        binding.cslbRetry.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                lifecycleScope.launch {
                    findNavController().navigate(R.id.action_ask_overlay_permission_fragment)
                }
            }
        })
    }
}