package com.viewsonic.classswift.data.clientapp.myviewboard

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class MyViewBoardLaunchStateTest {

    @After
    fun tearDown() {
        MyViewBoardLaunchState.updateToken("")
    }

    @Test
    fun `updateToken updates current mvbToken state`() {
        MyViewBoardLaunchState.updateToken("")
        assertEquals("", MyViewBoardLaunchState.mvbToken.value)

        MyViewBoardLaunchState.updateToken("token_abc")
        assertEquals("token_abc", MyViewBoardLaunchState.mvbToken.value)

        MyViewBoardLaunchState.updateToken("token_xyz")
        assertEquals("token_xyz", MyViewBoardLaunchState.mvbToken.value)
    }
}
