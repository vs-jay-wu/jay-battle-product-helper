package com.viewsonic.classswift.ui.fragment

import com.viewsonic.classswift.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.viewsonic.classswift.databinding.FragmentNoNetworkBinding
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AccessRestrictedFragment : Fragment() {

    private lateinit var binding: FragmentNoNetworkBinding
    private val loginViewModel: LoginViewModel by activityViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNoNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            tvTitle.text = getString(R.string.autoupdate_access_restricted_title)
            tvDescription.text = getString(R.string.autoupdate_access_restricted_msg)
        }
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