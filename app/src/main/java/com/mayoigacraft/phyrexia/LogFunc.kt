package com.mayoigacraft.phyrexia

import android.util.Log
import java.lang.Exception

fun logV(tag: String, msg: String) {
    if(BuildConfig.DEBUG) Log.v(tag, msg)
}

fun logV(tag: String, msg: String, e: Exception) {
    if(BuildConfig.DEBUG) Log.v(tag, msg, e)
}

fun logD(tag: String, msg: String) {
    if(BuildConfig.DEBUG) Log.d(tag, msg)
}

fun logD(tag: String, msg: String, e: Exception) {
    if(BuildConfig.DEBUG) Log.d(tag, msg, e)
}

fun logI(tag: String, msg: String) {
    if(BuildConfig.DEBUG) Log.i(tag, msg)
}

fun logI(tag: String, msg: String, e: Exception) {
    if(BuildConfig.DEBUG) Log.i(tag, msg, e)
}

fun logW(tag: String, msg: String) {
    if(BuildConfig.DEBUG) Log.w(tag, msg)
}

fun logW(tag: String, msg: String, e: Exception) {
    if(BuildConfig.DEBUG) Log.w(tag, msg, e)
}

fun logE(tag: String, msg: String) {
    if(BuildConfig.DEBUG) Log.e(tag, msg)
}

fun logE(tag: String, msg: String, e: Exception) {
    if(BuildConfig.DEBUG) Log.e(tag, msg, e)
}
