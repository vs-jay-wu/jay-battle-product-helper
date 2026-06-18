package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viewsonic.classswift.databinding.FragmentMaintenanceWarningBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MaintenanceApiErrorFragment: Fragment() {
    private lateinit var binding: FragmentMaintenanceWarningBinding
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMaintenanceWarningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickAction()
    }

    private fun initClickAction() {
        binding.cslbButton.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                loginViewModel.quitApp()
            }
        })
    }
}