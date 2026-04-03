package com.epic.aiexpensevoice.data.remote.network

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.epic.aiexpensevoice.BuildConfig
import com.epic.aiexpensevoice.core.common.Constants
import com.epic.aiexpensevoice.data.local.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class BaseUrlHolder(defaultUrl: String) {
    @Volatile
    var baseUrl: String = defaultUrl
}

class DynamicBaseUrlInterceptor(
    private val holder: BaseUrlHolder,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val overrideUrl = holder.baseUrl.toHttpUrlOrNull() ?: return chain.proceed(request)
        val newUrl = request.url.newBuilder()
            .scheme(overrideUrl.scheme)
            .host(overrideUrl.host)
            .port(overrideUrl.port)
            .build()
        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}

class AuthInterceptor(sessionManager: SessionManager) : Interceptor {
    @Volatile
    private var token: String? = null

    init {
        sessionManager.session
            .onEach { token = it.accessToken }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val builder = chain.request().newBuilder()
        token?.takeIf(String::isNotBlank)?.let { builder.addHeader("Authorization", "Bearer $it") }
        return chain.proceed(builder.build())
    }
}

class ApiDebugLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val requestBody = runCatching {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
        }.getOrNull().orEmpty()

        Log.d(
            Constants.NetworkLogTag,
            buildString {
                append("REQUEST ${request.method} ${request.url}")
                if (requestBody.isNotBlank()) {
                    append("\nBody: ")
                    append(requestBody)
                }
            },
        )

        return try {
            val response = chain.proceed(request)
            val responseBody = runCatching { response.peekBody(Long.MAX_VALUE).string() }.getOrNull().orEmpty()
            Log.d(
                Constants.NetworkLogTag,
                buildString {
                    append("RESPONSE ${response.code} ${response.request.url}")
                    if (response.message.isNotBlank()) {
                        append(" (${response.message})")
                    }
                    if (responseBody.isNotBlank()) {
                        append("\nBody: ")
                        append(responseBody)
                    }
                },
            )
            response
        } catch (exception: IOException) {
            Log.e(
                Constants.NetworkLogTag,
                "FAILED ${request.method} ${request.url}: ${exception.message}",
                exception,
            )
            throw exception
        }
    }
}

object NetworkModule {
    fun createApiService(
        context: Context,
        sessionManager: SessionManager,
        baseUrlHolder: BaseUrlHolder,
    ): ApiService {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor(baseUrlHolder))
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logger)

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(ApiDebugLoggingInterceptor())
            clientBuilder.addInterceptor(
                ChuckerInterceptor.Builder(context)
                    .collector(ChuckerCollector(context))
                    .alwaysReadResponseBody(true)
                    .build(),
            )
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(Constants.PlaceholderBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
