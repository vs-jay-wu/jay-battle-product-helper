package com.viewsonic.classswift.ui.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentUpdateAvailableBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class UpdateAvailableFragment : Fragment() {
    private lateinit var binding: FragmentUpdateAvailableBinding
    private val accountManager: AccountManager by inject()
    private val networkManager: NetworkManager by inject()
    private val appUpdateManager: AppUpdateManager by inject()
    private val args: UpdateAvailableFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUpdateAvailableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (args.isUpdateFailed) {
            showErrorToast()
        }
        binding.cslbQuitApp.setEnable()
        binding.cslbUpdateNow.setEnable()
        initClickAction()
    }

    private fun initClickAction() {
        binding.cslbQuitApp.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                accountManager.quitApp(false)
            }
        })

        binding.cslbUpdateNow.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                if (!networkManager.isNetworkAvailable()) {
                    showErrorToast()
                    return
                }

                if (!appUpdateManager.canRequestPackageInstalls()) {
                    guideToUnknownAppSourcesSetting()
                    return
                }

                findNavController().navigate(R.id.action_to_download_apk_fragment)
            }
        })
    }

    private fun guideToUnknownAppSourcesSetting() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${requireContext().packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            requireContext().startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            requireContext().startActivity(intent)
        }
    }

    private fun showErrorToast() {
        lifecycleScope.launch {
            if (!binding.cstToast.isVisible) {
                binding.cstToast.visibility = View.VISIBLE
                withContext(Dispatchers.IO) {
                    delay(3000)
                }
                binding.cstToast.visibility = View.GONE
            }
        }
    }
}
