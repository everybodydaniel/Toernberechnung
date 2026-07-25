package com.example.trnberechnung.network

import com.example.trnberechnung.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val BSH_BASE_URL = "https://gdi.bsh.de/"
    private const val DWD_BASE_URL = "https://api.brightsky.dev/"
    val SOCIAL_FEED_BASE_URL: String = secureHttpsBaseUrl(BuildConfig.CREWSPACE_BASE_URL)

    val crewspaceHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
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

    val socialFeedApi: SocialFeedApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SOCIAL_FEED_BASE_URL)
            .client(crewspaceHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SocialFeedApiService::class.java)
    }

    val crewspaceApi: CrewspaceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SOCIAL_FEED_BASE_URL)
            .client(crewspaceHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CrewspaceApiService::class.java)
    }

    val elwisApi: ElwisApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BSH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ElwisApiService::class.java)
    }
}

internal fun secureHttpsBaseUrl(candidate: String): String {
    val parsed = runCatching { URI(candidate.trim()) }.getOrNull()
    if (
        parsed?.scheme?.equals("https", ignoreCase = true) != true ||
        parsed.host.isNullOrBlank() ||
        parsed.userInfo != null ||
        parsed.rawQuery != null ||
        parsed.rawFragment != null
    ) {
        return "https://example.invalid/"
    }
    val ascii = parsed.toASCIIString()
    val normalized = "https://${ascii.substringAfter("://")}"
    return if (normalized.endsWith("/")) normalized else "$normalized/"
}
