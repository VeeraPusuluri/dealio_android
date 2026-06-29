package com.dealio.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dealio.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ServerStatusViewModel(app: Application) : AndroidViewModel(app) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val healthUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/health"

    private val _isDown = MutableStateFlow(false)
    val isDown: StateFlow<Boolean> = _isDown.asStateFlow()

    private val _checking = MutableStateFlow(true)
    val checking: StateFlow<Boolean> = _checking.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                ping()
                delay(15_000)
            }
        }
    }

    fun retry() {
        viewModelScope.launch { ping() }
    }

    private suspend fun ping() {
        _checking.value = true
        val ok = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(healthUrl).build()
                val response = client.newCall(request).execute()
                response.close()
                response.isSuccessful
            } catch (_: Exception) {
                false
            }
        }
        _isDown.value = !ok
        _checking.value = false
    }
}
