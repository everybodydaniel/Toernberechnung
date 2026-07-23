package com.example.trnberechnung.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
object RetrofitInstance {

    private const val BSH_BASE_URL = "https://gdi.bsh.de/"
    private const val DWD_BASE_URL = "https://api.brightsky.dev/"
    const val SOCIAL_FEED_BASE_URL = "http://131.173.65.118:8080/"
    private const val FIREBASE_AUTH_BASE_URL = "https://identitytoolkit.googleapis.com/"
    const val FIREBASE_API_KEY = "AIzaSyBW1sOPCwQ82XzOA5kdKveULlFqy3VTKP0"

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

    val firebaseAuthApi: FirebaseAuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(FIREBASE_AUTH_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirebaseAuthApiService::class.java)
    }

    val socialFeedApi: SocialFeedApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SOCIAL_FEED_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SocialFeedApiService::class.java)
    }
}