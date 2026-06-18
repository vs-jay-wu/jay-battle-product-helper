package com.viewsonic.classswift.manager

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * VSFT-8429: state-machine contract for [PendingClassEntryWindowManager].
 *
 * The manager is a one-slot mailbox for an opener closure produced by
 * `MyViewBoardMessageHandler` and consumed by
 * `SelectOrgAndSelectClassWindowModel` when class entry succeeds. These
 * tests pin the edge behaviour callers rely on:
 *
 * - consume returns null when nothing was set,
 * - consume empties the slot (it is take, not peek),
 * - set overwrites a previous unconsumed action,
 * - clear empties the slot.
 */
class PendingClassEntryWindowManagerTest {

    @Test
    fun `consume returns null when nothing has been set`() {
        val manager = PendingClassEntryWindowManager()

        assertNull(manager.consume())
    }

    @Test
    fun `consume returns the previously set action and invokes it`() = runTest {
        val manager = PendingClassEntryWindowManager()
        var ran = false
        manager.set { ran = true }

        val action = manager.consume()

        assertNotNull("set then consume should return the action", action)
        action!!.invoke()
        assertEquals(true, ran)
    }

    @Test
    fun `consume empties the slot so a second consume returns null`() {
        val manager = PendingClassEntryWindowManager()
        manager.set { /* no-op */ }

        manager.consume()

        assertNull("consume should empty the slot", manager.consume())
    }

    @Test
    fun `set overwrites a previously unconsumed action`() = runTest {
        val manager = PendingClassEntryWindowManager()
        val log = mutableListOf<String>()
        manager.set { log += "first" }
        manager.set { log += "second" }

        manager.consume()!!.invoke()

        assertEquals(listOf("second"), log)
    }

    @Test
    fun `clear empties the slot so consume returns null`() {
        val manager = PendingClassEntryWindowManager()
        manager.set { /* no-op */ }

        manager.clear()

        assertNull(manager.consume())
    }
}
