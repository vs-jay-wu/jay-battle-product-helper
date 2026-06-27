package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentRegionDetectBinding
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class RegionDetectFragment : Fragment() {

    private lateinit var binding: FragmentRegionDetectBinding
    private val loginViewModel: LoginViewModel by activityViewModel()
    private val networkManager: NetworkManager by inject()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentRegionDetectBinding.inflate(inflater, container, false)
        initCollection()
        checkNetworkStatus()
        return binding.root
    }

    private fun checkNetworkStatus() {
        if (networkManager.isNetworkAvailable()) {
            lifecycleScope.launch {
                loginViewModel.getRegion()
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                findNavController().navigate(R.id.action_to_no_network_fragment)
            }
        }
    }

    private fun initCollection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.regionDetectFlow.collect {
                    when (it) {
                        LoginViewModel.DetectRegionEvent.GetRegionApiError -> {
                            //todo ui/ux didn't define yet
                            lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_to_no_network_fragment)
                            }
                        }
                        is LoginViewModel.DetectRegionEvent.IsAvailableRegion -> {
                            if (it.isAvailable) {
                                loginViewModel.setLoginState(LoginViewModel.LoginState.CHECK_OVERLAY_PERMISSION)
                                lifecycleScope.launch(Dispatchers.Main) {
                                    findNavController().navigate(R.id.action_to_login_fragment)
                                }
                            } else {
                                setNotAvailableRegionUI()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setNotAvailableRegionUI() {
        binding.apply {
            ivBigIcon.isVisible = false
            cpiLoadingProgress.isVisible = false
            ivIcon.isVisible = true
            tvTitle.isVisible = true
            tvDescription.isVisible = true
            cswLbGotIt.isVisible = true
            cswLbGotIt.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    loginViewModel.quitApp()
                }
            })
        }
    }
}