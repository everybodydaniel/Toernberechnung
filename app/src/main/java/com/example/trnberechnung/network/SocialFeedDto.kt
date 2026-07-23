package com.example.trnberechnung.network

import com.google.gson.annotations.SerializedName

// ══════════════════════════════════════════════════════════════
// Firebase Auth DTOs
// ══════════════════════════════════════════════════════════════

data class FirebaseAuthRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)

data class FirebaseAuthResponse(
    val idToken: String?,
    val email: String?,
    val refreshToken: String?,
    val expiresIn: String?,
    val localId: String?,
    val error: FirebaseErrorDetails? = null
)

data class FirebaseErrorDetails(
    val code: Int?,
    val message: String?
)

// ══════════════════════════════════════════════════════════════
// Skipper Profile (schema: skipper_profiles)
// ══════════════════════════════════════════════════════════════

data class ApiSkipperProfile(
    val id: String,
    val name: String,
    @SerializedName("boat_type") val boatType: String = "Unbekannt",
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("home_harbour") val homeHarbour: String? = null,
    val bio: String? = null,
    @SerializedName("post_ids") val postIds: List<String>? = null,
    @SerializedName("follower_count") val followerCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_followed_by_current_skipper") val isFollowed: Boolean = false
)

data class ApiUpdateProfileRequest(
    @SerializedName("skipper_id") val skipperId: String,
    val name: String,
    @SerializedName("boat_type") val boatType: String,
    @SerializedName("home_harbour") val homeHarbour: String? = null,
    val bio: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)

data class ApiUpdateBoatTypeRequest(
    @SerializedName("boat_type") val boatType: String
)

// ══════════════════════════════════════════════════════════════
// Posts / Social Feed (schema: posts, post_likes, comments)
// ══════════════════════════════════════════════════════════════

data class ApiPost(
    val id: String,
    @SerializedName("skipper_id") val skipperId: String,
    @SerializedName("skipper_name") val skipperName: String,
    @SerializedName("skipper_profile_image_url") val skipperProfileImageUrl: String? = null,
    val text: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("liked_by_skipper_ids") val likedBySkipperIds: List<String>? = null,
    @SerializedName("comment_ids") val commentIds: List<String>? = null,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    val profile: ApiSkipperProfile? = null
)

data class ApiCreatePostRequest(
    @SerializedName("skipper_id") val skipperId: String,
    @SerializedName("skipper_name") val skipperName: String,
    @SerializedName("skipper_profile_image_url") val skipperProfileImageUrl: String? = null,
    val text: String,
    @SerializedName("image_url") val imageUrl: String? = null
)

data class ApiComment(
    val id: String,
    @SerializedName("post_id") val postId: String,
    @SerializedName("skipper_id") val skipperId: String,
    @SerializedName("skipper_name") val skipperName: String,
    val text: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ApiCreateCommentRequest(
    @SerializedName("skipper_id") val skipperId: String,
    @SerializedName("skipper_name") val skipperName: String,
    val text: String
)

data class ApiLikePostRequest(
    @SerializedName("skipper_id") val skipperId: String
)

// ══════════════════════════════════════════════════════════════
// Crewspace Conversations (schema: crewspace_conversations + crewspace_members)
// ══════════════════════════════════════════════════════════════

data class ApiCrewspaceConversation(
    val id: String,
    val title: String,
    val kind: String, // "direct" or "group"
    @SerializedName("member_ids") val memberIds: List<String>?,
    @SerializedName("member_names") val memberNames: List<String>?,
    @SerializedName("last_message") val lastMessage: String?,
    @SerializedName("last_message_at") val lastMessageAt: String?,
    @SerializedName("unread_count") val unreadCount: Int = 0,
    @SerializedName("updated_at") val updatedAt: String?
)

data class ApiCrewspaceGroupInfo(
    val id: String,
    val title: String,
    val info: String = "",
    @SerializedName("created_by") val createdBy: String,
    @SerializedName("can_manage") val canManage: Boolean = false,
    val members: List<ApiCrewspaceGroupMember>? = null
)

data class ApiCrewspaceGroupMember(
    @SerializedName("skipper_id") val skipperId: String,
    val name: String,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("permission_role") val permissionRole: String = "member",
    @SerializedName("crew_role") val crewRole: String = "Crew",
    @SerializedName("is_on_board") val isOnBoard: Boolean = false,
    @SerializedName("joined_at") val joinedAt: String? = null
)

data class ApiCreateDirectChatRequest(
    @SerializedName("skipper_id") val skipperId: String
)

data class ApiCreateGroupChatRequest(
    val title: String,
    val info: String = "",
    @SerializedName("member_ids") val memberIds: List<String>
)

data class ApiUpdateGroupRequest(
    val title: String,
    val info: String = ""
)

data class ApiUpdateMemberRequest(
    @SerializedName("skipper_id") val skipperId: String,
    @SerializedName("crew_role") val crewRole: String,
    @SerializedName("is_on_board") val isOnBoard: Boolean
)

// ══════════════════════════════════════════════════════════════
// Crewspace Messages (schema: crewspace_messages)
// ══════════════════════════════════════════════════════════════

data class ApiCrewspaceMessage(
    val id: String,
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_name") val senderName: String,
    val text: String,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("media_type") val mediaType: String? = null, // "image" | "audio"
    @SerializedName("media_duration_seconds") val mediaDurationSeconds: Double? = null,
    val poll: ApiCrewspacePoll? = null,
    val event: ApiCrewspaceEvent? = null,
    @SerializedName("created_at") val createdAt: String?
)

data class ApiCreateMessageRequest(
    val text: String,
    @SerializedName("media_url") val mediaUrl: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("media_duration_seconds") val mediaDurationSeconds: Double? = null
)

// ══════════════════════════════════════════════════════════════
// Crewspace Events (schema: crewspace_events)
// ══════════════════════════════════════════════════════════════

data class ApiCrewspaceEvent(
    val id: String,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("conversation_title") val conversationTitle: String? = null,
    @SerializedName("creator_id") val creatorId: String,
    @SerializedName("creator_name") val creatorName: String,
    val title: String,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    val location: String? = null,
    val notes: String? = null,
    @SerializedName("attachment_url") val attachmentUrl: String? = null,
    @SerializedName("attachment_name") val attachmentName: String? = null,
    @SerializedName("attachment_content_type") val attachmentContentType: String? = null
)

data class ApiCreateEventRequest(
    @SerializedName("conversation_id") val conversationId: String? = null,
    val title: String,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    val location: String? = null,
    val notes: String? = null,
    @SerializedName("attachment_url") val attachmentUrl: String? = null,
    @SerializedName("attachment_name") val attachmentName: String? = null,
    @SerializedName("attachment_content_type") val attachmentContentType: String? = null
)

data class ApiShareEventRequest(
    @SerializedName("conversation_id") val conversationId: String
)

// ══════════════════════════════════════════════════════════════
// Crewspace Polls (schema: crewspace_polls, crewspace_poll_options, crewspace_poll_votes)
// ══════════════════════════════════════════════════════════════

data class ApiCrewspacePoll(
    val id: String,
    val question: String,
    @SerializedName("allows_multiple") val allowsMultiple: Boolean = false,
    @SerializedName("closes_at") val closesAt: String? = null,
    @SerializedName("total_votes") val totalVotes: Int = 0,
    val options: List<ApiCrewspacePollOption> = emptyList()
)

data class ApiCrewspacePollOption(
    val id: String,
    val label: String,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("is_selected") val isSelected: Boolean = false
)

data class ApiCreatePollRequest(
    val question: String,
    val options: List<String>,
    @SerializedName("allows_multiple") val allowsMultiple: Boolean = false,
    @SerializedName("closes_at") val closesAt: String? = null
)

data class ApiVotePollRequest(
    @SerializedName("option_ids") val optionIds: List<String>
)

// ══════════════════════════════════════════════════════════════
// Uploads (Presign)
// ══════════════════════════════════════════════════════════════

data class ApiPresignUploadRequest(
    @SerializedName("content_type") val contentType: String,
    @SerializedName("file_extension") val fileExtension: String
)

data class ApiPresignUploadResponse(
    @SerializedName("upload_url") val uploadUrl: String,
    @SerializedName("public_url") val publicUrl: String,
    val method: String,
    val headers: Map<String, String>? = null
)

// ══════════════════════════════════════════════════════════════
// Maritime Notices (schema: maritime_notices)
// ══════════════════════════════════════════════════════════════

data class ApiMaritimeNotice(
    val id: String,
    @SerializedName("bfs_number") val bfsNumber: String,
    @SerializedName("is_temporary") val isTemporary: Boolean = false,
    val publisher: String,
    val title: String,
    @SerializedName("region_path") val regionPath: String,
    val location: String? = null,
    val body: String,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("valid_from") val validFrom: String? = null,
    @SerializedName("valid_until") val validUntil: String? = null,
    @SerializedName("publication_state") val publicationState: String,
    val revision: Int,
    @SerializedName("source_url") val sourceUrl: String? = null
)

// ══════════════════════════════════════════════════════════════
// Backwards-compatible alias (used in existing code)
// ══════════════════════════════════════════════════════════════

/** @deprecated Use ApiSkipperProfile instead */
typealias ApiCrewspaceSkipper = ApiSkipperProfile
