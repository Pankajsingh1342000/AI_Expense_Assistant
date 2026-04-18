package com.epic.aiexpensevoice.data.remote.network

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.epic.aiexpensevoice.BuildConfig
import com.epic.aiexpensevoice.core.common.Constants
import com.epic.aiexpensevoice.data.local.SessionManager
import com.epic.aiexpensevoice.data.remote.dto.AuthResponseDto
import com.epic.aiexpensevoice.data.remote.dto.RefreshTokenRequestDto
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.Response

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

class TokenAuthenticator(
    private val sessionManager: SessionManager,
    private val baseUrlHolder: BaseUrlHolder,
    private val gson: Gson = Gson(),
) : Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.startsWith("/api/v1/auth/")) return null
        if (responseCount(response) >= 2) {
            runBlocking { sessionManager.clearSession() }
            return null
        }

        val session = runBlocking { sessionManager.getSessionSnapshot() }
        val refreshToken = session.refreshToken.orEmpty()
        if (refreshToken.isBlank()) {
            runBlocking { sessionManager.clearSession() }
            return null
        }

        val latestAccessToken = session.accessToken.orEmpty()
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim().orEmpty()
        if (latestAccessToken.isNotBlank() && latestAccessToken != requestToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $latestAccessToken")
                .build()
        }

        val refreshUrl = "${baseUrlHolder.baseUrl.trimEnd('/')}/api/v1/auth/refresh"
        val requestBody = gson.toJson(RefreshTokenRequestDto(refreshToken))
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post(requestBody)
            .build()

        val refreshResponse = runCatching { refreshClient.newCall(refreshRequest).execute() }.getOrNull()
        if (refreshResponse == null) {
            return null
        }
        if (!refreshResponse.isSuccessful) {
            val shouldLogout = refreshResponse.code == 401 || refreshResponse.code == 400
            refreshResponse.close()
            if (shouldLogout) {
                runBlocking { sessionManager.clearSession() }
            }
            return null
        }

        val authBody = runCatching {
            refreshResponse.body?.string()?.let { gson.fromJson(it, AuthResponseDto::class.java) }
        }.getOrNull()
        refreshResponse.close()

        if (authBody == null || authBody.access_token.isBlank() || authBody.refresh_token.isBlank()) {
            runBlocking { sessionManager.clearSession() }
            return null
        }

        runBlocking {
            sessionManager.updateTokens(
                accessToken = authBody.access_token,
                refreshToken = authBody.refresh_token,
                tokenType = authBody.token_type,
            )
        }

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${authBody.access_token}")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

class ApiDebugLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val redactedHeaders = request.headers.toMultimap().mapValues { (_, values) ->
            values.map { value ->
                if (value.startsWith("Bearer ", ignoreCase = true)) "Bearer [REDACTED]" else value
            }
        }
        val requestBody = runCatching {
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
        }.getOrNull().orEmpty().sanitizeSensitiveContent()

        Log.d(
            Constants.NetworkLogTag,
            buildString {
                append("REQUEST ${request.method} ${request.url}")
                if (redactedHeaders.isNotEmpty()) {
                    append("\nHeaders: ")
                    append(redactedHeaders)
                }
                if (requestBody.isNotBlank()) {
                    append("\nBody: ")
                    append(requestBody)
                }
            },
        )

        return try {
            val response = chain.proceed(request)
            val responseBody = runCatching { response.peekBody(Long.MAX_VALUE).string() }.getOrNull().orEmpty().sanitizeSensitiveContent()
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
        val logger = HttpLoggingInterceptor { message ->
            Log.d(Constants.NetworkLogTag, message.sanitizeSensitiveContent())
        }.apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
            redactHeader("Authorization")
        }
        val clientBuilder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor(baseUrlHolder))
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(logger)
            .authenticator(TokenAuthenticator(sessionManager, baseUrlHolder))

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

private fun String.sanitizeSensitiveContent(): String {
    var sanitized = this
    val replacements = listOf(
        Regex("""(Authorization:\s*Bearer\s+)[^\s]+""", RegexOption.IGNORE_CASE) to "$1[REDACTED]",
        Regex("""("access_token"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE) to "$1[REDACTED]$2",
        Regex("""("refresh_token"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE) to "$1[REDACTED]$2",
        Regex("""([?&]refresh_token=)[^&\s]+""", RegexOption.IGNORE_CASE) to "$1[REDACTED]",
        Regex("""([?&]password=)[^&\s]+""", RegexOption.IGNORE_CASE) to "$1[REDACTED]",
        Regex("""([?&]username=)[^&\s]+""", RegexOption.IGNORE_CASE) to "$1[REDACTED]",
        Regex("""("password"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE) to "$1[REDACTED]$2",
        Regex("""("email"\s*:\s*")[^"]+(")""", RegexOption.IGNORE_CASE) to "$1[REDACTED]$2",
    )
    replacements.forEach { (regex, replacement) ->
        sanitized = sanitized.replace(regex, replacement)
    }
    return sanitized
}
