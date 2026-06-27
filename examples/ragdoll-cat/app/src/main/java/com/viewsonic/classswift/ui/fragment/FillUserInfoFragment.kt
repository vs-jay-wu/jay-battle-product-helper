package com.viewsonic.classswift.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.SuperscriptSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UserInfoBody
import com.viewsonic.classswift.constant.AppConstants.THREE_SEC_DELAY
import com.viewsonic.classswift.data.info.FillUserInfo
import com.viewsonic.classswift.databinding.FragmentFillUserBinding
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.ui.fragment.adapter.CountryInfoAdapter
import com.viewsonic.classswift.ui.fragment.adapter.StringInfoItemAdapter
import com.viewsonic.classswift.ui.viewmodel.LoginViewModel
import com.viewsonic.classswift.ui.widget.CSSpinner
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.utils.extension.removeSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import java.util.Locale


class FillUserInfoFragment : Fragment() {

    private lateinit var binding: FragmentFillUserBinding
    private val accountManager: AccountManager by inject()
    private val loginViewModel: LoginViewModel by viewModel(ownerProducer = { requireActivity() })

    private val articleBaseURL = BuildConfig.ARTICLE_URL
    private lateinit var fillUserInfo: FillUserInfo
    private var isKeyboardVisible = false
    private val hasStateCountryCode = listOf("AU", "BR", "CA", "CN", "IN", "IE", "IT", "MX", "US")
    private lateinit var countryAdapter: CountryInfoAdapter

    //ifFillInfo means, user filled user info before, so don't need to fill again.
    private var isFillInfo = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFillUserBinding.inflate(inflater, container, false).apply {
            //keyboard display set view up translationY keyboard half height
            root.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = root.height
                val keyboardHeight = screenHeight - rect.bottom
                val isNowVisible = keyboardHeight > screenHeight * 0.15
                if (isNowVisible != isKeyboardVisible) {
                    // click spinner will hide keyboard
                    if (!isNowVisible) {
                        clearEditTextFocus()
                    }
                    isKeyboardVisible = isNowVisible
                    clFillUserView.translationY = if (isKeyboardVisible) -(keyboardHeight.toFloat() / 2) else 0F
                }
            }
            //when button is loading, avoid any click behavior
            viewMask.setOnTouchListener { _, _ ->
                Timber.d("LOADING viewMask setOnTouchListener")
                true
            }

            acetDisplayName.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    hideKeyboard(v)
                }
            }

            acetSchool.setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    hideKeyboard(v)
                }
            }
            cslbSubmit.setDisable()
        }
        fillUserInfo = accountManager.fillUserInfo
        isFillInfo = fillUserInfo.isNeedFillInfoUI
        setUI()
        return binding.root
    }

    private fun setUI() {
        binding.actvTitle.text =
            if (isFillInfo) getString(R.string.userinfo_title) else getString(R.string.userinfo_terms_update_title)
        binding.actvSubtitle.text =
            if (isFillInfo) getString(R.string.userinfo_sub_title) else getString(R.string.userinfo_terms_update_sub_title)

        // 設置fill account area 相關事件
        binding.clFillArea.isVisible = isFillInfo
        if (isFillInfo) {
            setCountryAdapter()
            setRoleAdapter()
            binding.acetDisplayName.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // 當字串改變前會執行這裡的程式碼
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // 當字串改變時會執行這裡的程式碼
                }

                override fun afterTextChanged(s: Editable?) {
                    // 當字串改變後會執行這裡的程式碼
                    setLoadingButtonStatus()
                }
            })
            binding.acetSchool.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // 當字串改變前會執行這裡的程式碼
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // 當字串改變時會執行這裡的程式碼
                }

                override fun afterTextChanged(s: Editable?) {
                    // 當字串改變後會執行這裡的程式碼
                    setLoadingButtonStatus()
                }
            })
        }

        binding.clEulaArea.isVisible = fillUserInfo.isNeedConsentUI
        binding.clPrivacyArea.isVisible = fillUserInfo.isNeedConsentUI
        // 設置eula area和 promo area 相關事件
        if (fillUserInfo.isNeedConsentUI) {
            setEULAUrlText()
            setPrivacyUrlText()
            binding.csacbEula.setOnClickListener {
                clearEditTextFocus()
                setLoadingButtonStatus()
            }
            binding.csacbPrivacy.setOnClickListener {
                clearEditTextFocus()
            }
        }

        binding.clAiArea.isVisible = fillUserInfo.isNeedAIConsentUI
        if (fillUserInfo.isNeedAIConsentUI) {
            binding.csacbAi.setOnClickListener {
                clearEditTextFocus()
                setLoadingButtonStatus()
            }
        }

        binding.cslbSubmit.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onStateChange(state: LoadingButtonState) {
                when (state) {
                    LoadingButtonState.LOADING -> binding.viewMask.visibility = View.VISIBLE
                    else -> binding.viewMask.visibility = View.GONE
                }
            }

            override fun onEnableClicked() {
                binding.cslbSubmit.setLoading()
                sendUserInfoByWeb()
                lifecycleScope.launch {
                    accountManager.sendUserInfo(generateRequestBody())
                    updateSuccessUI()
                    withContext(Dispatchers.IO) {
                        delay(THREE_SEC_DELAY)
                    }
                    loginViewModel.showSelectOrgWindow()
                }
            }
        })
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun clearEditTextFocus() {
        binding.acetDisplayName.clearFocus()
        binding.acetSchool.clearFocus()
    }

    private fun sendUserInfoByWeb() {
        val url =
            "${BuildConfig.USER_INFO_URL}/l/927653/2023-06-05/6ky2mf?firstName=${accountManager.userInfo.firstName}&lastName=${accountManager.userInfo.lastName}" +
                    "&country=${getCountry()}&state=${getState()}&organization=${getOrg()}&identity=${getRole()}&email=${accountManager.userInfo.email}&newsletters=${getNewsletters()}"
        Timber.d("url address: $url")
        binding.wvSendInfo.loadUrl(url)
    }

    private fun getCountry(): CharSequence {
        return binding.cssCountry.displayString.removeSpace()
    }

    private fun getState(): CharSequence {
        return if (binding.llcState.isVisible) binding.cssState.displayString.removeSpace() else ""
    }

    private fun getOrg(): CharSequence {
        return binding.acetSchool.text ?: ""
    }

    private fun getRole(): CharSequence {
        return binding.cssRole.displayString.removeSpace()
    }

    private fun getNewsletters(): Boolean {
        return binding.csacbPrivacy.isChecked()
    }

    private fun setEULAUrlText() {
        // 獲取文字和對應的 URL
        val eulaString = getString(R.string.userinfo_eula_string)
        val serviceText = getString(R.string.userinfo_service_string)
        val policyText = getString(R.string.userinfo_privacy_string)

        // 根據語言獲取帶佔位符的字串，並替換 %s 為對應的文本
        val textWithLinks = getString(R.string.userinfo_eula_message, serviceText, eulaString, policyText) + "*"
        // 創建 SpannableString 以便設置 URLSpan
        val spannableString = SpannableString(textWithLinks)
        // 找到 service 文字的位置並設置 URLSpan
        val startService = textWithLinks.indexOf(serviceText)
        val endService = startService + serviceText.length
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 點擊 service 時開啟 Custom Tab
                openCustomTab("$articleBaseURL/service?lang=${LanguageUtils.webLanguageCode}")
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, startService, endService, 0)
        // 找到 eula 文字的位置並設置 URLSpan
        val startEULA = textWithLinks.indexOf(eulaString)
        val endEULA = startEULA + eulaString.length
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 點擊 eula 時開啟 Custom Tab
                openCustomTab("$articleBaseURL/eula?lang=${LanguageUtils.webLanguageCode}")
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, startEULA, endEULA, 0)
        // 找到 Privacy Policy 文字的位置並設置 URLSpan
        val startPolicy = textWithLinks.indexOf(policyText)
        val endPolicy = startPolicy + policyText.length
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 點擊 eula 時開啟 Custom Tab
                openCustomTab("$articleBaseURL/policy?lang=${LanguageUtils.webLanguageCode}")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, startPolicy, endPolicy, 0)
        // 將星號設置為上標
        spannableString.setSpan(
            SuperscriptSpan(),
            spannableString.length - 1, spannableString.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        context?.let {
            // 設定顏色
            spannableString.setSpan(
                ForegroundColorSpan(it.getColor(R.color.brand_blue)),  // 設定顏色為藍色
                startService,                 // 起始位置
                endService,                   // 結束位置
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(it.getColor(R.color.brand_blue)),  // 設定顏色為藍色
                startEULA,                 // 起始位置
                endEULA,                   // 結束位置
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(it.getColor(R.color.brand_blue)),  // 設定顏色為藍色
                startPolicy,                 // 起始位置
                endPolicy,                   // 結束位置
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(it.getColor(R.color.starred_text_view_red)),
                spannableString.length - 1, spannableString.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.actvEula.text = spannableString
        binding.actvEula.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setPrivacyUrlText() {
        // 獲取文字和對應的 URL
        val policyText = getString(R.string.userinfo_privacy_string)
        // 根據語言獲取帶佔位符的字串，並替換 %s 為對應的文本
        val textWithLinks = getString(R.string.userinfo_privacy_message, policyText)
        // 創建 SpannableString 以便設置 URLSpan
        val spannableString = SpannableString(textWithLinks)
        // 找到 Policy 文字的位置並設置 URLSpan
        val startPolicy = textWithLinks.indexOf(policyText)
        val endPolicy = startPolicy + policyText.length
        spannableString.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                // 點擊 Description 時開啟 Custom Tab
                openCustomTab("$articleBaseURL/policy?lang=${LanguageUtils.webLanguageCode}")
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false
            }
        }, startPolicy, endPolicy, 0)
        // 設定顏色
        context?.let {
            // 設定顏色
            spannableString.setSpan(
                ForegroundColorSpan(it.getColor(R.color.brand_blue)),  // 設定顏色為藍色
                startPolicy,                 // 起始位置
                endPolicy,                   // 結束位置
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        binding.actvPrivacy.text = spannableString
        binding.actvPrivacy.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openCustomTab(url: String) {
        Timber.d("openCustomTab url: $url")
        context?.let {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true) // 顯示標題
                .setStartAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // 進入動畫
                .setExitAnimations(it, android.R.anim.slide_in_left, android.R.anim.slide_out_right) // 退出動畫
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(it, Uri.parse(url))
        }
    }

    private fun setCountryAdapter() {
        this.context?.let { context ->
            val countryList = getSortedCountryNames()
            countryAdapter = CountryInfoAdapter(context, countryList)
            binding.cssCountry.displayString = countryList[0].second
            countryAdapter.setSelectedPosition(0)
            setStateUI(countryList[0].first)
            binding.cssCountry.setAdapter(countryAdapter, object : CSSpinner.OnSpinnerItemSelectedListener {
                override fun onItemSelected(position: Int) {
                    countryAdapter.setSelectedPosition(position)
                    setStateUI(countryList[position].first)
                    binding.cssCountry.displayString = countryList[position].second
                }
            })
        }
    }

    private fun setStateUI(countryCode: String) {
        if (hasStateCountryCode.contains(countryCode)) {
            setStateAdapter(countryCode)
            binding.llcState.isVisible = true
        } else {
            binding.llcState.visibility = View.GONE
        }
    }

    private fun setStateAdapter(countryCode: String) {
        this.context?.let { context ->
            val stateList =
                when (countryCode) {
                    "AU" -> resources.getStringArray(R.array.au_states).toList()
                    "BR" -> resources.getStringArray(R.array.br_states).toList()
                    "CA" -> resources.getStringArray(R.array.ca_states).toList()
                    "CN" -> resources.getStringArray(R.array.cn_states).toList()
                    "IN" -> resources.getStringArray(R.array.in_states).toList()
                    "IE" -> resources.getStringArray(R.array.ie_states).toList()
                    "IT" -> resources.getStringArray(R.array.it_states).toList()
                    "MX" -> resources.getStringArray(R.array.mx_states).toList()
                    "US" -> resources.getStringArray(R.array.us_states).toList()
                    else -> {
                        binding.llcState.visibility = View.GONE
                        listOf("")
                    }
                }
            val stateAdapter = StringInfoItemAdapter(context, stateList)
            binding.cssState.displayString = stateList[0]
            stateAdapter.setSelectedPosition(0)
            binding.cssState.setAdapter(stateAdapter, object : CSSpinner.OnSpinnerItemSelectedListener {
                override fun onItemSelected(position: Int) {
                    stateAdapter.setSelectedPosition(position)
                    binding.cssState.displayString = stateList[position]
                }
            })
        }
    }

    private fun setRoleAdapter() {
        this.context?.let { context ->
            val roleList = resources.getStringArray(R.array.fill_user_role_list).toList()
            val roleAdapter = StringInfoItemAdapter(context, roleList)
            binding.cssRole.displayString = roleList[0]
            roleAdapter.setSelectedPosition(0)
            binding.cssRole.setAdapter(roleAdapter, object : CSSpinner.OnSpinnerItemSelectedListener {
                override fun onItemSelected(position: Int) {
                    roleAdapter.setSelectedPosition(position)
                    binding.cssRole.displayString = roleList[position]
                }
            })
        }
    }

    private fun generateRequestBody(): UserInfoBody {
        val body = UserInfoBody()
        // fill account 畫面有展現
        if (isFillInfo) {
            body.defaultDisplayName = binding.acetDisplayName.text.toString()
            body.country = binding.cssCountry.displayString.toString()
        } else {
            body.country = accountManager.country
        }
        body.eulaID = accountManager.getArticleInfo().eulaID
        body.serviceID = accountManager.getArticleInfo().serviceID
        body.privacyID = accountManager.getArticleInfo().privacyID
        body.slaID = accountManager.getArticleInfo().slaID
        body.chirpAIID = if (binding.csacbAi.isChecked()) accountManager.getArticleInfo().chirpAIID else null
        Timber.d("requestBody: $body")
        return body
    }

    private fun updateSuccessUI() {
        binding.clFillUserView.visibility = View.GONE
        binding.clSuccessView.visibility = View.VISIBLE
    }

    private fun setLoadingButtonStatus() {
        if (checkFillInfoComplete() && checkEulaComplete() && checkAIComplete()) {
            binding.cslbSubmit.setEnable()
        } else {
            binding.cslbSubmit.setDisable()
        }

    }

    private fun checkFillInfoComplete(): Boolean {
        val flag =
            //isNeedFillInfoUI is false 就不用檢查是否有填寫資料了
            !fillUserInfo.isNeedFillInfoUI
                    // display Name和 school不能為空或者空白
                    || ((!binding.acetDisplayName.text.isNullOrBlank() && !binding.acetSchool.text.isNullOrBlank())
                    // llcState 沒顯示 或者 state spinner 字串不能空或者空白
                    && (!binding.llcState.isVisible || binding.cssState.displayString.isNotBlank()))
        //isNeedFillInfoUI is false 就不用檢查是否有填寫資料了
        return flag
    }

    private fun checkEulaComplete(): Boolean {
        //isNeedConsentUI is false 就不用檢查是否有打勾
        val flag = !fillUserInfo.isNeedConsentUI || binding.csacbEula.isChecked()
        return flag
    }

    private fun checkAIComplete(): Boolean {
        //isChirpAIConsent is false 就不用檢查是否有打勾
        val flag = !fillUserInfo.isNeedAIConsentUI || binding.csacbAi.isChecked()
        return flag
    }

    //todo 語系要改成系統或者app設置
    //first 是countryCode, second是country Name
    private fun getSortedCountryNames(): List<Pair<String, String>> {
        // 優先國家代碼列表（確保順序：美國第一，台灣第二）
        val priorityCountryCodes = listOf("US", "TW")
        // 動態生成優先國家名稱
        val priorityCountries = priorityCountryCodes.map { countryCode ->
            countryCode to Locale("", countryCode).getDisplayCountry(Locale.ENGLISH)
        }
        // 獲取所有國家名稱
        val countries = Locale.getISOCountries().map { countryCode ->
            countryCode to Locale("", countryCode).getDisplayCountry(Locale.ENGLISH)
        }
        // 分離優先國家和其他國家，並確保順序
        val remainingCountries = countries.filterNot { priorityCountries.contains(it) }.sortedBy { it.second }

        // 優先國家 + 其他國家
        return priorityCountries + remainingCountries
    }
}