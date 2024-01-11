package org.lynxz.mlkit.base.util

import android.util.Log

object LogWrapper {
    private const val TAG = "LogWrapper"

    private var _enable = true
    private const val VERBOSE = 0 // 所有日志
    private const val DEBUG = 1
    private const val INFO = 2
    private const val WARN = 3
    private const val ERROR = 4
    private const val NONE = 10 // 不打印任何级别日志

    fun enable(b: Boolean) {
        _enable = b
    }

    private fun logImpl(level: Int, tag: String = TAG, msg: String, throwable: Throwable? = null) {
        if (!_enable) {
            return
        }

        val tip = if (throwable == null) msg else "$msg\n${Log.getStackTraceString(throwable)}"
        when (level) {
            VERBOSE -> Log.v(tag, tip)
            DEBUG -> Log.d(tag, tip)
            INFO -> Log.i(tag, tip)
            WARN -> Log.w(tag, tip)
            ERROR -> Log.e(tag, tip)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun v(tag: String, msg: String, tr: Throwable? = null) = logImpl(VERBOSE, tag, msg, tr)

    @JvmStatic
    @JvmOverloads
    fun d(tag: String, msg: String, tr: Throwable? = null) = logImpl(DEBUG, tag, msg, tr)

    @JvmStatic
    @JvmOverloads
    fun i(tag: String, msg: String, tr: Throwable? = null) = logImpl(INFO, tag, msg, tr)

    @JvmStatic
    @JvmOverloads
    fun w(tag: String, msg: String, tr: Throwable? = null) = logImpl(WARN, tag, msg, tr)

    @JvmStatic
    @JvmOverloads
    fun e(tag: String, msg: String, tr: Throwable? = null) = logImpl(ERROR, tag, msg, tr)
}