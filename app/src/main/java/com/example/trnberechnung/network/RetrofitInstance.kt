package com.example.trnberechnung.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BSH_BASE_URL = "https://gdi.bsh.de/"
    private const val DWD_BASE_URL = "https://api.brightsky.dev/"
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    /**
     * Model latency is different in kind from the plain REST calls above: a generous read timeout,
     * but still a finite one. The archived Gemini SDK defaulted to an
     * infinite timeout, which left the composer permanently disabled on a dropped offshore
     * connection with no way to cancel.
     *
     * Deliberately no `HttpLoggingInterceptor` here - it would write the API key header into logcat.
     */
    val geminiHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(geminiHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }

    val bshApi: BshApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BSH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BshApiService::class.java)
    }

    val dwdApi: DwdApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DWD_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DwdApiService::class.java)
    }
}
