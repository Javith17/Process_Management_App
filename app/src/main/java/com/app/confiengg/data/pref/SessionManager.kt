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
        const val OFFICE_LAT = "office_lat"
        const val OFFICE_LONG = "office_long"
        const val OFFICE_PERIMETER = "office_perimeter"
        const val IS_CHECKED_IN = "is_checked_in"
        const val CHECK_IN_TIME = "check_in_time"
        const val HAS_QUOTATIONS = "has_quotations"
    }

    fun setCheckedIn(checkedIn: Boolean, timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().apply {
            putBoolean(IS_CHECKED_IN, checkedIn)
            if (checkedIn) {
                putLong(CHECK_IN_TIME, timestamp)
            } else {
                remove(CHECK_IN_TIME)
            }
        }.apply()
    }

    fun getCheckInTime(): Long = prefs.getLong(CHECK_IN_TIME, 0L)

    fun isCheckedInState(): Boolean {
        return prefs.getBoolean(IS_CHECKED_IN, false)
    }

    fun saveAuthToken(token: String) {
        val editor = prefs.edit()
        editor.putString(ACCESS_TOKEN, token)
        editor.apply()
    }

    fun fetchAuthToken(): String? {
        return prefs.getString(ACCESS_TOKEN, null)
    }

    fun saveOfficeConfigs(lat: String?, long: String?, perimeter: Int?) {
        val editor = prefs.edit()
        editor.putString(OFFICE_LAT, lat)
        editor.putString(OFFICE_LONG, long)
        editor.putInt(OFFICE_PERIMETER, perimeter ?: 100)
        editor.apply()
    }

    fun getOfficeLat(): String? = prefs.getString(OFFICE_LAT, null)
    fun getOfficeLong(): String? = prefs.getString(OFFICE_LONG, null)
    fun getOfficePerimeter(): Int = prefs.getInt(OFFICE_PERIMETER, 100)

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

    fun saveHasQuotations(hasQuotations: Boolean) {
        prefs.edit().putBoolean(HAS_QUOTATIONS, hasQuotations).apply()
    }

    fun hasQuotations(): Boolean {
        return prefs.getBoolean(HAS_QUOTATIONS, false)
    }

    fun logout() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
