package com.viewsonic.classswift.ui.widget.fastscroll

fun interface Predicate<T> {
    fun test(t: T?): Boolean
}