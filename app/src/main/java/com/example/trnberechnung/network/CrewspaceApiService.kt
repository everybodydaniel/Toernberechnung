package com.example.trnberechnung.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Canonical v1 Crewspace contract shared by Android, iOS and the Go server.
 * Identity always comes from the Firebase bearer token, never from request IDs.
 */
interface CrewspaceApiService {
    @PUT("crewspace/me")
    suspend fun updateMe(
        @Header("Authorization") authorization: String,
        @Body request: ApiUpdateMeRequest,
    ): Response<ApiSkipperProfile>

    @GET("crewspace/conversations")
    suspend fun conversations(
        @Header("Authorization") authorization: String,
    ): Response<List<ApiCrewspaceConversation>>

    @POST("crewspace/direct")
    suspend fun direct(
        @Header("Authorization") authorization: String,
        @Body request: ApiCreateDirectChatRequest,
    ): Response<ApiCrewspaceConversation>

    @GET("crewspace/conversations/{id}/messages/page")
    suspend fun messagePage(
        @Header("Authorization") authorization: String,
        @Path("id") conversationId: String,
        @Query("before") before: String? = null,
        @Query("after") after: String? = null,
        @Query("limit") limit: Int = 100,
    ): Response<ApiMessagePage>

    @POST("crewspace/conversations/{id}/messages")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Path("id") conversationId: String,
        @Body request: ApiCreateMessageRequest,
    ): Response<ApiCrewspaceMessage>

    @POST("crewspace/conversations/{id}/read")
    suspend fun markRead(
        @Header("Authorization") authorization: String,
        @Path("id") conversationId: String,
    ): Response<Unit>

    @POST("crewspace/uploads/presign")
    suspend fun presignUpload(
        @Header("Authorization") authorization: String,
        @Body request: ApiPresignUploadRequest,
    ): Response<ApiPresignUploadResponse>

    @PUT("crewspace/devices")
    suspend fun registerDevice(
        @Header("Authorization") authorization: String,
        @Body request: ApiDeviceRegistrationRequest,
    ): Response<Unit>

    @DELETE("crewspace/devices/{installationId}")
    suspend fun deleteDevice(
        @Header("Authorization") authorization: String,
        @Path(value = "installationId", encoded = false) installationId: String,
    ): Response<Unit>

    @GET("crewspace/blocks")
    suspend fun blocks(
        @Header("Authorization") authorization: String,
    ): Response<ApiBlocksResponse>

    @PUT("crewspace/blocks/{uid}")
    suspend fun block(
        @Header("Authorization") authorization: String,
        @Path(value = "uid", encoded = false) uid: String,
    ): Response<Unit>

    @DELETE("crewspace/blocks/{uid}")
    suspend fun unblock(
        @Header("Authorization") authorization: String,
        @Path(value = "uid", encoded = false) uid: String,
    ): Response<Unit>

    @GET("crewspace/events")
    suspend fun events(
        @Header("Authorization") authorization: String,
    ): Response<List<ApiCrewspaceEvent>>
}
