package com.viewsonic.classswift.ui.webInterface

import android.content.Context
import android.webkit.JavascriptInterface
import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.data.spinner.PickupStudentInfo
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import timber.log.Timber

class SpinnerWebInterface(private val context: Context) {
    private var spinnerListener: SpinnerListener? = null

    private val studentAdapter = MoshiProvider.moshiNormal.adapter(
        SpinnerStudentInfo::class.java
    )

    private val pickupStudentAdapter = MoshiProvider.moshiNormal.adapter(
        PickupStudentInfo::class.java
    )

    interface SpinnerListener {
        fun onGetStudentList(): SpinnerStudentInfo
        fun onStudentPicked(studentId: String)
        fun onStudentRemoved(studentId: String)
    }

    fun setSpinnerCallbackListener(listener: SpinnerListener) {
        spinnerListener = listener
    }

    @JavascriptInterface
    fun getStudentList(): String {
        Timber.Forest.d("[SpinnerWebInterface]: getStudentList")
        val students = spinnerListener?.onGetStudentList()
        return studentAdapter.toJson(students)
    }

    @JavascriptInterface
    fun studentPicked(data: String): Boolean {
        val student = pickupStudentAdapter.fromJson(data)
        val studentId = student?.studentId ?: ""

        if (studentId.isEmpty()) {
            return false
        } else {
            spinnerListener?.onStudentPicked(studentId = studentId)
            Timber.Forest.d("[SpinnerWebInterface]: Student picked: $studentId")
            return true
        }
    }

    @JavascriptInterface
    fun studentRemoved(data: String): Boolean {
        val student = pickupStudentAdapter.fromJson(data)
        val studentId = student?.studentId ?: ""

        if (studentId.isEmpty()) {
            return false
        } else {
            spinnerListener?.onStudentRemoved(studentId = studentId)
            Timber.Forest.d("[SpinnerWebInterface]: Student removed: $studentId")
            return true
        }
    }
}