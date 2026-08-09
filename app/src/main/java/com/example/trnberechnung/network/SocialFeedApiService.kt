package com.example.trnberechnung.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// ══════════════════════════════════════════════════════════════
// Social Feed API. Crewspace uses the dedicated CrewspaceApiService.
// ══════════════════════════════════════════════════════════════

interface SocialFeedApiService {
    // ── Health ──
    @GET("healthz")
    suspend fun healthCheck(): Response<Unit>

    // ══════════════════════════════════════════════════════════
    // Posts / Social Feed
    // ══════════════════════════════════════════════════════════

    @GET("posts")
    suspend fun listPosts(
        @Header("Authorization") authHeader: String,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<ApiPost>>

    @POST("posts")
    suspend fun createPost(
        @Header("Authorization") authHeader: String,
        @Body request: ApiCreatePostRequest
    ): Response<ApiPost>

    @DELETE("posts/{id}")
    suspend fun deletePost(
        @Header("Authorization") authHeader: String,
        @Path("id") postId: String
    ): Response<Unit>

    @POST("posts/{id}/like")
    suspend fun likePost(
        @Header("Authorization") authHeader: String,
        @Path("id") postId: String,
        @Body request: ApiLikePostRequest
    ): Response<ApiPost>

    @POST("posts/{id}/comments")
    suspend fun createComment(
        @Header("Authorization") authHeader: String,
        @Path("id") postId: String,
        @Body request: ApiCreateCommentRequest
    ): Response<ApiComment>

    @GET("posts/{id}/comments")
    suspend fun getComments(
        @Header("Authorization") authHeader: String,
        @Path("id") postId: String
    ): Response<List<ApiComment>>

    // ══════════════════════════════════════════════════════════
    // Profiles (skipper_profiles)
    // ══════════════════════════════════════════════════════════

    @GET("profiles/{id}")
    suspend fun getProfile(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String
    ): Response<ApiSkipperProfile>

    @PUT("profiles/{id}")
    suspend fun updateProfile(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String,
        @Body request: ApiUpdateProfileRequest
    ): Response<ApiSkipperProfile>

    @PATCH("profiles/{id}/boat-type")
    suspend fun updateBoatType(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String,
        @Body request: ApiUpdateBoatTypeRequest
    ): Response<Unit>

    @POST("profiles/{id}/follow")
    suspend fun followProfile(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String
    ): Response<Unit>

    @POST("profiles/{id}/unfollow")
    suspend fun unfollowProfile(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String
    ): Response<Unit>

    // ══════════════════════════════════════════════════════════
    // Uploads
    // ══════════════════════════════════════════════════════════

    @POST("uploads/presign")
    suspend fun presignUpload(
        @Header("Authorization") authHeader: String,
        @Body request: ApiPresignUploadRequest
    ): Response<ApiPresignUploadResponse>
}
