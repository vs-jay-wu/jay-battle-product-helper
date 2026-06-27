package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viewsonic.classswift.databinding.FragmentUnderMaintenanceBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UnderMaintenanceFragment: Fragment() {
    private lateinit var binding: FragmentUnderMaintenanceBinding
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentUnderMaintenanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            tvTitle.text = loginViewModel.getUnderMaintenanceTitle()
            tvDescription.text = loginViewModel.getUnderMaintenanceDescription()
        }
        initClickAction()
    }

    private fun initClickAction() {
        with(binding) {
            ivClose.setOnClickListener {
                loginViewModel.quitApp()
            }
            cslbGotIt.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    loginViewModel.quitApp()
                }
            })
        }
    }
}