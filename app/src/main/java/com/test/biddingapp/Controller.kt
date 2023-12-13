package com.test.onlinestoreapp

import android.app.ProgressDialog
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object Controller {

    private var pDialog: ProgressDialog? = null

    fun show_loader(context: Context, message: String) {
        pDialog?.dismiss()
        pDialog = ProgressDialog(context)
        pDialog?.setMessage(message)
        pDialog?.setCancelable(false)
        pDialog?.show()
    }

    fun hide_loader() {
        pDialog?.dismiss()
        pDialog = null
    }

    private const val PREFS_NAME = "prefs"
    private const val USER_ID = "userId"
    private const val USER_NAME = "userName"
    private const val IS_LOGGED_IN = "isLoggedIn"

    fun saveUserId(context: Context, userId: String) {
        saveValue(context, USER_ID, userId)
    }

    fun saveUserName(context: Context, userName: String) {
        saveValue(context, USER_NAME, userName)
    }

    fun getUserId(context: Context): String {
        return getValue(context, USER_ID, "") as String
    }

    fun getUserName(context: Context): String {
        return getValue(context, USER_NAME, "") as String
    }

    fun saveLoginState(context: Context, isLoggedIn: Boolean) {
        saveValue(context, IS_LOGGED_IN, isLoggedIn)
    }

    fun getLoginState(context: Context): Boolean {
        return getValue(context, IS_LOGGED_IN, false) as Boolean
    }

    private fun saveValue(context: Context, key: String, value: Any) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()

        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            else -> throw IllegalArgumentException("Unsupported value type")
        }

        editor.apply()
    }

    private fun getValue(context: Context, key: String, defaultValue: Any): Any {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        return when (defaultValue) {
            is String -> prefs.getString(key, defaultValue) ?: defaultValue
            is Boolean -> prefs.getBoolean(key, defaultValue)
            else -> throw IllegalArgumentException("Unsupported value type")
        }
    }


    fun calculateTimeUntilExpiry(expiryTimestamp: Long): String {
     //   val currentTime = System.currentTimeMillis()
        val remainingTime = expiryTimestamp

        return if (remainingTime > 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(remainingTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60
            "$hours hours, $minutes minutes, and $seconds seconds remaining"
        } else {
            "Bidding time is over"
        }
    }


    fun convertLongToTime(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

}
