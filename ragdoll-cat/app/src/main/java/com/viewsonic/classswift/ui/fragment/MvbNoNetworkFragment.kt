package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viewsonic.classswift.databinding.FragmentMvbNoNetworkBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MvbNoNetworkFragment : Fragment() {

    private lateinit var binding: FragmentMvbNoNetworkBinding
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMvbNoNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickAction()
        binding.buttonEnterClass.setDisable()
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
}