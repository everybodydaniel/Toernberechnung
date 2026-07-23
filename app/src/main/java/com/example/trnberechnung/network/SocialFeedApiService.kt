package com.example.trnberechnung.network

import retrofit2.Response
import retrofit2.http.*

// ══════════════════════════════════════════════════════════════
// Firebase Identity Toolkit (Login / Signup)
// ══════════════════════════════════════════════════════════════

interface FirebaseAuthApiService {
    @POST("v1/accounts:signUp")
    suspend fun signUp(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): Response<FirebaseAuthResponse>

    @POST("v1/accounts:signInWithPassword")
    suspend fun signInWithPassword(
        @Query("key") apiKey: String,
        @Body request: FirebaseAuthRequest
    ): Response<FirebaseAuthResponse>
}

// ══════════════════════════════════════════════════════════════
// Social Feed + Crewspace API (Go Server @ 131.173.65.118:8080)
// Routen gemäß main.go und schema.sql
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
    // Crewspace – Skipper Lookup
    // ══════════════════════════════════════════════════════════

    @GET("crewspace/skippers/{id}")
    suspend fun getSkipper(
        @Header("Authorization") authHeader: String,
        @Path("id") skipperId: String
    ): Response<ApiSkipperProfile>

    // ══════════════════════════════════════════════════════════
    // Crewspace – Conversations
    // ══════════════════════════════════════════════════════════

    @GET("crewspace/conversations")
    suspend fun getConversations(
        @Header("Authorization") authHeader: String
    ): Response<List<ApiCrewspaceConversation>>

    @POST("crewspace/direct")
    suspend fun createDirectChat(
        @Header("Authorization") authHeader: String,
        @Body request: ApiCreateDirectChatRequest
    ): Response<ApiCrewspaceConversation>

    @POST("crewspace/conversations")
    suspend fun createGroupChat(
        @Header("Authorization") authHeader: String,
        @Body request: ApiCreateGroupChatRequest
    ): Response<ApiCrewspaceConversation>

    @GET("crewspace/conversations/{id}/info")
    suspend fun getGroupInfo(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String
    ): Response<ApiCrewspaceGroupInfo>

    @PUT("crewspace/conversations/{id}")
    suspend fun updateGroup(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String,
        @Body request: ApiUpdateGroupRequest
    ): Response<Unit>

    @DELETE("crewspace/conversations/{id}")
    suspend fun leaveConversation(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String
    ): Response<Unit>

    // ══════════════════════════════════════════════════════════
    // Crewspace – Messages
    // ══════════════════════════════════════════════════════════

    @GET("crewspace/conversations/{id}")
    suspend fun getMessages(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String
    ): Response<List<ApiCrewspaceMessage>>

    @POST("crewspace/conversations/{id}")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String,
        @Body request: ApiCreateMessageRequest
    ): Response<ApiCrewspaceMessage>

    // ══════════════════════════════════════════════════════════
    // Crewspace – Members
    // ══════════════════════════════════════════════════════════

    @POST("crewspace/conversations/{id}/members")
    suspend fun addMember(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String,
        @Body request: ApiCreateDirectChatRequest // reuses skipper_id
    ): Response<Unit>

    @PUT("crewspace/conversations/{id}/members")
    suspend fun updateMember(
        @Header("Authorization") authHeader: String,
        @Path("id") conversationId: String,
        @Body request: ApiUpdateMemberRequest
    ): Response<Unit>

    // ══════════════════════════════════════════════════════════
    // Crewspace – Events
    // ══════════════════════════════════════════════════════════

    @GET("crewspace/events")
    suspend fun getEvents(
        @Header("Authorization") authHeader: String
    ): Response<List<ApiCrewspaceEvent>>

    @POST("crewspace/events")
    suspend fun createEvent(
        @Header("Authorization") authHeader: String,
        @Body request: ApiCreateEventRequest
    ): Response<ApiCrewspaceEvent>

    @PUT("crewspace/events/{id}")
    suspend fun updateEvent(
        @Header("Authorization") authHeader: String,
        @Path("id") eventId: String,
        @Body request: ApiCreateEventRequest
    ): Response<ApiCrewspaceEvent>

    @DELETE("crewspace/events/{id}")
    suspend fun deleteEvent(
        @Header("Authorization") authHeader: String,
        @Path("id") eventId: String
    ): Response<Unit>

    @POST("crewspace/events/{id}/share")
    suspend fun shareEvent(
        @Header("Authorization") authHeader: String,
        @Path("id") eventId: String,
        @Body request: ApiShareEventRequest
    ): Response<Unit>

    // ══════════════════════════════════════════════════════════
    // Crewspace – Polls
    // ══════════════════════════════════════════════════════════

    @POST("crewspace/polls/{conversationId}")
    suspend fun createPoll(
        @Header("Authorization") authHeader: String,
        @Path("conversationId") conversationId: String,
        @Body request: ApiCreatePollRequest
    ): Response<ApiCrewspaceMessage>

    @POST("crewspace/polls/{pollId}/vote")
    suspend fun votePoll(
        @Header("Authorization") authHeader: String,
        @Path("pollId") pollId: String,
        @Body request: ApiVotePollRequest
    ): Response<ApiCrewspacePoll>

    // ══════════════════════════════════════════════════════════
    // Uploads
    // ══════════════════════════════════════════════════════════

    @POST("uploads/presign")
    suspend fun presignUpload(
        @Header("Authorization") authHeader: String,
        @Body request: ApiPresignUploadRequest
    ): Response<ApiPresignUploadResponse>

    @POST("crewspace/uploads/presign")
    suspend fun crewspacePresignUpload(
        @Header("Authorization") authHeader: String,
        @Body request: ApiPresignUploadRequest
    ): Response<ApiPresignUploadResponse>

    // ══════════════════════════════════════════════════════════
    // Maritime Notices
    // ══════════════════════════════════════════════════════════

    @GET("maritime-notices")
    suspend fun listMaritimeNotices(
        @Query("state") state: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<List<ApiMaritimeNotice>>

    @GET("maritime-notices/{id}")
    suspend fun getMaritimeNotice(
        @Path("id") noticeId: String
    ): Response<ApiMaritimeNotice>
}
