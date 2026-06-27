package com.viewsonic.classswift.ui.window.adapter

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-function coverage for SelectOrgAndClassAdapter's position math after the create-class
 * button moved from the footer (last position) to the header (position 0). The helpers under
 * test are the single source of truth for adapter/data-position translation and selection
 * bookkeeping; the instance methods (selectItem, removeItem, getSelectedItem, etc.) delegate
 * to them.
 */
class SelectOrgAndClassAdapterPositionTest {

    // ── computeItemCount ─────────────────────────────────────────────────────

    @Test
    fun `computeItemCount adds one for header when list is empty and not loading`() {
        assertEquals(1, SelectOrgAndClassAdapter.computeItemCount(dataSize = 0, showLoadingPlaceholder = false))
    }

    @Test
    fun `computeItemCount adds one for header and one for loading placeholder`() {
        assertEquals(2, SelectOrgAndClassAdapter.computeItemCount(dataSize = 0, showLoadingPlaceholder = true))
    }

    @Test
    fun `computeItemCount with three classes and no loading equals four`() {
        assertEquals(4, SelectOrgAndClassAdapter.computeItemCount(dataSize = 3, showLoadingPlaceholder = false))
    }

    @Test
    fun `computeItemCount with three classes and loading equals five`() {
        assertEquals(5, SelectOrgAndClassAdapter.computeItemCount(dataSize = 3, showLoadingPlaceholder = true))
    }

    // ── resolveViewType ──────────────────────────────────────────────────────

    @Test
    fun `resolveViewType returns HEADER at position zero`() {
        val viewType = SelectOrgAndClassAdapter.resolveViewType(
            position = 0,
            itemCount = 4,
            showLoadingPlaceholder = false
        )
        assertEquals(SelectOrgAndClassAdapter.TYPE_HEADER, viewType)
    }

    @Test
    fun `resolveViewType returns CLASS for middle positions`() {
        val viewType = SelectOrgAndClassAdapter.resolveViewType(
            position = 2,
            itemCount = 4,
            showLoadingPlaceholder = false
        )
        assertEquals(SelectOrgAndClassAdapter.TYPE_CLASS, viewType)
    }

    @Test
    fun `resolveViewType returns LOADING for last position when loading placeholder is shown`() {
        val viewType = SelectOrgAndClassAdapter.resolveViewType(
            position = 4,
            itemCount = 5,
            showLoadingPlaceholder = true
        )
        assertEquals(SelectOrgAndClassAdapter.TYPE_LOADING, viewType)
    }

    @Test
    fun `resolveViewType returns CLASS for last position when loading placeholder is hidden`() {
        val viewType = SelectOrgAndClassAdapter.resolveViewType(
            position = 3,
            itemCount = 4,
            showLoadingPlaceholder = false
        )
        assertEquals(SelectOrgAndClassAdapter.TYPE_CLASS, viewType)
    }

    // ── adapter ↔ data position translation ──────────────────────────────────

    @Test
    fun `toAdapterPosition shifts data position by header offset`() {
        assertEquals(1, SelectOrgAndClassAdapter.toAdapterPosition(0))
        assertEquals(3, SelectOrgAndClassAdapter.toAdapterPosition(2))
    }

    @Test
    fun `toDataPosition is the inverse of toAdapterPosition`() {
        val dataPosition = 4
        val roundTrip = SelectOrgAndClassAdapter.toDataPosition(
            SelectOrgAndClassAdapter.toAdapterPosition(dataPosition)
        )
        assertEquals(dataPosition, roundTrip)
    }

    // ── computeSelectedAfterRemoval ──────────────────────────────────────────

    @Test
    fun `removal of item before selected shifts selected down by one`() {
        // [Header, A, B(selected), C] → delete A → [Header, B(still selected), C]
        // selectedPosition: 2 → 1
        val result = SelectOrgAndClassAdapter.computeSelectedAfterRemoval(
            selectedAdapterPosition = 2,
            removedDataPosition = 0
        )
        assertEquals(1, result)
    }

    @Test
    fun `removal of the selected item shifts selected down to the previous class`() {
        // [Header, A, B(selected), C] → delete B → [Header, A(now selected), C]
        // selectedPosition: 2 → 1
        val result = SelectOrgAndClassAdapter.computeSelectedAfterRemoval(
            selectedAdapterPosition = 2,
            removedDataPosition = 1
        )
        assertEquals(1, result)
    }

    @Test
    fun `removal of item after selected leaves selected unchanged`() {
        // [Header, A(selected), B, C] → delete C → [Header, A(still selected), B]
        // selectedPosition: 1 → 1
        val result = SelectOrgAndClassAdapter.computeSelectedAfterRemoval(
            selectedAdapterPosition = 1,
            removedDataPosition = 2
        )
        assertEquals(1, result)
    }

    @Test
    fun `selected never falls onto the header even when first class is removed`() {
        // [Header, A(selected), B] → delete A → [Header, B(now selected at position 1)]
        // selectedPosition must NOT decrement below HEADER_OFFSET (1).
        // Note: the UI prevents this state via the `isDeletable = currentList.size > 1` guard;
        // covered here as a helper-contract test so the lower-bound invariant is enforced
        // regardless of caller discipline.
        val result = SelectOrgAndClassAdapter.computeSelectedAfterRemoval(
            selectedAdapterPosition = 1,
            removedDataPosition = 0
        )
        assertEquals(1, result)
    }
}
