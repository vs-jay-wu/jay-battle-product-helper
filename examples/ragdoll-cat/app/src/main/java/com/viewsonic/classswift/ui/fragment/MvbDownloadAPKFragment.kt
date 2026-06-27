package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentMvbDownloadApkBinding
import com.viewsonic.classswift.manager.AppUpdateManager
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import timber.log.Timber

class MvbDownloadAPKFragment : Fragment() {
    private lateinit var binding: FragmentMvbDownloadApkBinding
    private val appUpdateManager: AppUpdateManager by inject()
    private var progressBarWidth = 0
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMvbDownloadApkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!appUpdateManager.canRequestPackageInstalls()) {
            appUpdateManager.emitUnknownSourceInstallDisallowed()
            return
        }
        initClickAction()
        initCollection()
        binding.llProgressRoot.post {
            // * 2 is right and left margin
            progressBarWidth = binding.llProgressRoot.width - (resources.getDimension(R.dimen.download_progressbar).toInt() * 2)
            startDownload()
        }
    }

    private fun initClickAction() {
        with(binding) {
            ibClose.setOnClickListener {
                loginViewModel.quitApp()
            }
            buttonExit.setOnClickListener {
                loginViewModel.quitApp()
            }
        }
    }

    private fun initCollection() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appUpdateManager.updateSharedFlow.distinctUntilChanged().collect { updateState ->
                    Timber.d("[B][initCollection] : updateState = $updateState")
                    when (updateState) {
                        AppUpdateManager.UpdateState.Done -> {
                            binding.tvDownload.text = getString(R.string.common_done)
                        }
                        is AppUpdateManager.UpdateState.Downloading -> {
                            binding.tvDownload.text = getString(R.string.autoupdated_download_lastest_version)
                            updateDownloadProgress(updateState.progress)
                        }
                        is AppUpdateManager.UpdateState.Error -> {
                            lifecycleScope.launch(Dispatchers.Main) {
                                findNavController().navigate(R.id.action_to_mvb_no_network_fragment)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startDownload() {
        lifecycleScope.launch {
            appUpdateManager.executeApkUpdateFlow(loginViewModel.getServerLatestAppVersionName())
        }
    }

    private fun updateDownloadProgress(percent: Int) {
        binding.apply {
            val params = viewProgress.layoutParams
            val tempWidth = progressBarWidth * percent / 100
            params.width = tempWidth
            viewProgress.layoutParams = params
            tvProgress.text = getString(R.string.autoupdate_download_percent, percent)
        }
    }
}
