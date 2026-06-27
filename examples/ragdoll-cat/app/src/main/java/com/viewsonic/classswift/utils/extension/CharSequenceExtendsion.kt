package com.viewsonic.classswift.utils.extension


fun CharSequence.removeSpace(): String {
   return this.toString().replace(" ", "")
}