package com.app.confiengg.data.pref

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("confi_engg_prefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_ID = "user_id"
        const val EMP_NAME = "emp_name"
        const val ROLE = "role"
        const val ACCESS_TOKEN = "access_token"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(ACCESS_TOKEN, token)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    fun saveUserSession(userId: String, empName: String, role: String) {
        val editor = prefs.edit()
        editor.putString(USER_ID, userId)
        editor.putString(EMP_NAME, empName)
        editor.putString(ROLE, role)
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.apply()
    }

    fun getUserRole(): String? {
        return prefs.getString(ROLE, null)
    }

    fun getEmpName(): String? {
        return prefs.getString(EMP_NAME, null)
    }

    fun getUserId(): String? {
        return prefs.getString(USER_ID, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
