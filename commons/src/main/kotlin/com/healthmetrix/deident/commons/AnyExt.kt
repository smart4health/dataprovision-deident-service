package com.healthmetrix.deident.commons

// not sure why catching ClassCastException doesn't work
inline fun <reified U> Any?.checkedCast(): U? = if (this is U?) this else null
