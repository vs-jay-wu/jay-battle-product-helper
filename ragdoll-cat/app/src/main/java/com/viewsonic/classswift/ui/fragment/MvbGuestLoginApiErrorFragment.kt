package com.viewsonic.classswift.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.FragmentMvbApiFailedBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class MvbGuestLoginApiErrorFragment: Fragment() {
    private lateinit var binding: FragmentMvbApiFailedBinding
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMvbApiFailedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClickAction()
        binding.buttonEnterClass.setDisable()
    }

    private fun initClickAction() {
        with(binding) {
            buttonRetry.setOnClickListener {
                loginViewModel.setLoginState(LoginViewModel.LoginState.GUEST_GET_CLASSROOM_LIST)
                lifecycleScope.launch(Dispatchers.Main) {
                    findNavController().navigate(R.id.action_to_mvb_activate_fragment)
                }
            }
            ibClose.setOnClickListener {
                loginViewModel.quitApp()
            }
            buttonExit.setOnClickListener {
                loginViewModel.quitApp()
            }
        }
    }
}