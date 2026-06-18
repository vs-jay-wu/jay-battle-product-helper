package com.viewsonic.classswift.ui.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentAskOverlayPermissionBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import androidx.core.net.toUri

class AskOverlayPermissionFragment : Fragment() {

    private lateinit var binding: FragmentAskOverlayPermissionBinding
    private val loginViewModel: LoginViewModel by viewModel(ownerProducer = { requireActivity() })

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            val isGranted = Settings.canDrawOverlays(this.activity)
            if (isGranted) {
                Timber.d("need go selectOrgWindow: ${loginViewModel.goSelectOrgWindow}")
                lifecycleScope.launch {
                    if (loginViewModel.goSelectOrgWindow) {
                        //disable click button, before select org window show.
                        disableButtonClick()
                        loginViewModel.showSelectOrgWindow()
                    } else {
                        findNavController().navigate(R.id.action_back_login_fragment)
                    }
                }
            } else {
                lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        toRetryPage()
                    }
                })
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAskOverlayPermissionBinding.inflate(inflater, container, false)
        binding.cslbDeny.setEnable()
        binding.cslbAllow.setEnable()
        initClickAction()
        return binding.root
    }

    private fun initClickAction() {
        binding.cslbDeny.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                toRetryPage()
            }
        })

        binding.cslbAllow.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${this@AskOverlayPermissionFragment.activity?.packageName}".toUri()
                )
                overlayPermissionLauncher.launch(intent)
            }
        })
    }

    private fun toRetryPage() {
        lifecycleScope.launch {
            findNavController().navigate(R.id.action_to_retry_fragment)
        }
    }

    private fun disableButtonClick() {
        binding.cslbAllow.setDisable()
        binding.cslbDeny.setDisable()
    }
}