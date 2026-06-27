package com.viewsonic.classswift.ui.viewmodel

import com.viewsonic.classswift.ui.viewmodel.LoginViewModel.Companion.resolveLoginAccountInfoSuccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginAccountInfoSuccessResolverTest {

    @Test
    fun `MVB bound clears remember flag and skips save-login dialog regardless of prior remember`() {
        val whenRemembered = resolveLoginAccountInfoSuccess(isMyViewBoardBound = true, isRememberLogin = true)
        assertTrue(whenRemembered.shouldClearRememberLoginInStore)
        assertEquals(LoginViewModel.LoginState.CHECK_FILL_USER_INFO, whenRemembered.nextState)

        val whenNotRemembered = resolveLoginAccountInfoSuccess(isMyViewBoardBound = true, isRememberLogin = false)
        assertTrue(whenNotRemembered.shouldClearRememberLoginInStore)
        assertEquals(LoginViewModel.LoginState.CHECK_FILL_USER_INFO, whenNotRemembered.nextState)
    }

    @Test
    fun `standalone remember true proceeds without clearing store`() {
        val r = resolveLoginAccountInfoSuccess(isMyViewBoardBound = false, isRememberLogin = true)
        assertFalse(r.shouldClearRememberLoginInStore)
        assertEquals(LoginViewModel.LoginState.CHECK_FILL_USER_INFO, r.nextState)
    }

    @Test
    fun `standalone remember false shows save-login dialog`() {
        val r = resolveLoginAccountInfoSuccess(isMyViewBoardBound = false, isRememberLogin = false)
        assertFalse(r.shouldClearRememberLoginInStore)
        assertEquals(LoginViewModel.LoginState.SHOW_REMEMBER_DIALOG, r.nextState)
    }
}
