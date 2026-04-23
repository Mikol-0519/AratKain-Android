package com.aratkain.core.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aratkain.AratKainApp
import retrofit2.Response
import com.google.gson.Gson

// ── View extensions ───────────────────────────────────────────
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.enable() { isEnabled = true }
fun View.disable() { isEnabled = false }

// ── EditText extensions ───────────────────────────────────────
fun EditText.value(): String = text.toString().trim()
fun EditText.clear() { setText("") }
fun EditText.isEmpty(): Boolean = value().isEmpty()

// ── String validation extensions ──────────────────────────────
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.isValidUsername(): Boolean =
    matches(Regex("^[a-zA-Z0-9_]{3,50}$"))

// ── Context extensions ────────────────────────────────────────
fun Context.toast(msg: String, long: Boolean = false) {
    Toast.makeText(
        this, msg,
        if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    ).show()
}

// ── Activity extension to get AratKainApp ─────────────────────
fun AppCompatActivity.getApp(): AratKainApp =
    application as AratKainApp

// ── Retrofit error body parser ────────────────────────────────
data class SupabaseError(
    val error: String?,
    val error_description: String?,
    val message: String?,
    val msg: String?
)

fun Response<*>.parseErrorMessage(): String {
    return try {
        val json = errorBody()?.string() ?: ""
        val err  = Gson().fromJson(json, SupabaseError::class.java)
        
        // Priority list of error fields from Supabase
        val message = err?.error_description ?: err?.message ?: err?.msg ?: err?.error
        
        when (code()) {
            400  -> message ?: "Invalid request. Please check your credentials."
            401  -> message ?: "Invalid email or password."
            404  -> "Service not found. Check your Supabase URL."
            409  -> message ?: "Account already exists."
            422  -> message ?: "Invalid data. Please check your input."
            500  -> "Server error. Please try again later."
            else -> message ?: "Error ${code()}. Please try again."
        }
    } catch (e: Exception) {
        when (code()) {
            401  -> "Invalid email or password."
            500  -> "Server error. Please try again later."
            else -> "Error ${code()}. Please try again."
        }
    }
}

fun Throwable.toNetworkMessage(): String = when {
    message?.contains("Unable to resolve host") == true ->
        "No internet connection. Please check your network."
    message?.contains("timeout") == true ->
        "Connection timed out. Please try again."
    message?.contains("Connection refused") == true ->
        "Cannot connect to server. Please try again later."
    else -> "Network error. Please try again."
}