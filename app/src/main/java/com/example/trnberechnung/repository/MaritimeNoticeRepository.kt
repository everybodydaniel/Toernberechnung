package com.example.trnberechnung.repository

import com.example.trnberechnung.database.MaritimeNoticeSyncDao
import com.example.trnberechnung.database.MaritimeNoticeSyncEntity
import com.example.trnberechnung.database.SeafarerMessageDao
import com.example.trnberechnung.database.SeafarerMessageEntity
import com.example.trnberechnung.network.MaritimeNoticeCoordinateDto
import com.example.trnberechnung.network.MaritimeNoticeDetailDto
import com.example.trnberechnung.network.MaritimeNoticeSummaryDto
import com.example.trnberechnung.network.RetrofitInstance
import com.example.trnberechnung.network.SocialFeedApiService
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.time.Instant
import java.time.OffsetDateTime

data class MaritimeNoticeSnapshot(
    val notices: List<SeafarerMessageEntity>,
    val isStale: Boolean,
    val fetchedAt: Long?,
    val lastIngestedAt: Long?,
    val source: MaritimeNoticeSource,
)

enum class MaritimeNoticeSource {
    NETWORK,
    NOT_MODIFIED,
    FRESH_CACHE,
    OFFLINE_CACHE,
}

class MaritimeNoticeUnavailableException(
    message: String = "Nachrichten für Seefahrer sind gerade nicht erreichbar.",
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

/**
 * Offline-first ELWIS notice repository.
 *
 * The list cache is fresh for 15 minutes. Failed requests may fall back to a
 * cache no older than seven days. Server revisions and local read revisions
 * remain independent.
 */
class MaritimeNoticeRepository(
    private val noticeDao: SeafarerMessageDao,
    private val syncDao: MaritimeNoticeSyncDao,
    private val api: SocialFeedApiService = RetrofitInstance.socialFeedApi,
    private val now: () -> Long = System::currentTimeMillis,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
    private val offlineMaximumAgeMillis: Long = DEFAULT_OFFLINE_MAXIMUM_AGE_MILLIS,
) {
    private val refreshMutex = Mutex()

    val observeAll: Flow<List<SeafarerMessageEntity>> = noticeDao.observeAll()
    val observeCurrent: Flow<List<SeafarerMessageEntity>>
        get() = noticeDao.observeCurrent(now())
    val observeUnread: Flow<List<SeafarerMessageEntity>>
        get() = noticeDao.observeUnread(now())
    val observeArchive: Flow<List<SeafarerMessageEntity>>
        get() = noticeDao.observeArchive(now())
    val unreadCount: Flow<Int> = noticeDao.getUnreadCount()
    val syncMetadata: Flow<MaritimeNoticeSyncEntity?> = syncDao.observe()

    fun search(query: String): Flow<List<SeafarerMessageEntity>> =
        if (query.isBlank()) observeAll else noticeDao.search(query.trim())

    suspend fun refresh(force: Boolean = false): MaritimeNoticeSnapshot =
        refreshMutex.withLock { refreshUnlocked(force) }

    private suspend fun refreshUnlocked(force: Boolean): MaritimeNoticeSnapshot {
        val requestStartedAt = now()
        val metadata = syncDao.get()
        if (
            !force &&
            metadata != null &&
            requestStartedAt - metadata.fetchedAt in 0 until cacheTtlMillis
        ) {
            return snapshot(metadata, MaritimeNoticeSource.FRESH_CACHE)
        }

        try {
            val response =
                api.listMaritimeNotices(
                    status = "all",
                    limit = 100,
                    ifNoneMatch = metadata?.etag,
                )
            if (response.code() == HTTP_NOT_MODIFIED) {
                val refreshed =
                    (metadata ?: MaritimeNoticeSyncEntity(fetchedAt = requestStartedAt)).copy(
                        fetchedAt = requestStartedAt,
                        isStale = false,
                        lastError = null,
                    )
                syncDao.upsert(refreshed)
                return snapshot(refreshed, MaritimeNoticeSource.NOT_MODIFIED)
            }
            if (!response.isSuccessful) throw HttpException(response)

            val payload =
                response.body()
                    ?: throw MaritimeNoticeUnavailableException(
                        "Der Meldungsdienst hat leer geantwortet.",
                    )
            val cachedById = noticeDao.getAllOnce().associateBy(SeafarerMessageEntity::id)
            val entities =
                payload.notices.map { summary ->
                    summary.toEntity(
                        existing = cachedById[summary.id],
                        cachedAt = requestStartedAt,
                    )
                }
            if (entities.isNotEmpty()) noticeDao.insertAll(entities)

            val refreshed =
                MaritimeNoticeSyncEntity(
                    fetchedAt = requestStartedAt,
                    lastIngestedAt = payload.lastIngestedAt.toEpochMillisOrNull(),
                    etag = response.headers()["ETag"],
                    isStale = payload.isStale,
                    lastError = null,
                )
            syncDao.upsert(refreshed)
            return snapshot(refreshed, MaritimeNoticeSource.NETWORK)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val cached = noticeDao.getAllOnce()
            val cacheAge = metadata?.let { requestStartedAt - it.fetchedAt }
            if (
                metadata != null &&
                cacheAge != null &&
                cacheAge in 0..offlineMaximumAgeMillis
            ) {
                val stale =
                    metadata.copy(
                        isStale = true,
                        lastError = error.message,
                    )
                syncDao.upsert(stale)
                return MaritimeNoticeSnapshot(
                    notices = cached,
                    isStale = true,
                    fetchedAt = stale.fetchedAt,
                    lastIngestedAt = stale.lastIngestedAt,
                    source = MaritimeNoticeSource.OFFLINE_CACHE,
                )
            }
            throw MaritimeNoticeUnavailableException(cause = error)
        }
    }

    suspend fun detail(
        noticeId: String,
        revision: Int,
    ): SeafarerMessageEntity {
        val cached = noticeDao.getById(noticeId)
        if (cached != null && cached.detailRevision >= revision) {
            noticeDao.markAsRead(noticeId)
            return cached.copy(readRevision = maxOf(cached.readRevision, cached.revision))
        }

        val requestedAt = now()
        try {
            val response = api.getMaritimeNotice(noticeId)
            if (!response.isSuccessful) throw HttpException(response)
            val detail =
                response.body()
                    ?: throw MaritimeNoticeUnavailableException(
                        "Der Meldungsdienst hat leer geantwortet.",
                    )
            if (detail.id != noticeId || detail.revision < revision) {
                throw MaritimeNoticeUnavailableException(
                    "Der Meldungsdienst hat eine veraltete Meldungsrevision geliefert.",
                )
            }
            val entity = detail.toEntity(cached, requestedAt)
            noticeDao.insert(entity)
            noticeDao.markAsRead(noticeId)
            return entity.copy(readRevision = maxOf(entity.readRevision, entity.revision))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (
                cached != null &&
                cached.detailRevision > 0 &&
                cached.detailFetchedAt?.let {
                    requestedAt - it in 0..offlineMaximumAgeMillis
                } == true
            ) {
                noticeDao.markAsRead(noticeId)
                return cached.copy(readRevision = maxOf(cached.readRevision, cached.revision))
            }
            throw MaritimeNoticeUnavailableException(cause = error)
        }
    }

    suspend fun markRead(noticeId: String) {
        noticeDao.markAsRead(noticeId)
    }

    suspend fun markAllRead() {
        noticeDao.markAllAsRead()
    }

    suspend fun archiveLocally(noticeId: String) {
        noticeDao.archive(noticeId)
    }

    private suspend fun snapshot(
        metadata: MaritimeNoticeSyncEntity,
        source: MaritimeNoticeSource,
    ) = MaritimeNoticeSnapshot(
        notices = noticeDao.getAllOnce(),
        isStale = metadata.isStale,
        fetchedAt = metadata.fetchedAt,
        lastIngestedAt = metadata.lastIngestedAt,
        source = source,
    )

    companion object {
        const val DEFAULT_CACHE_TTL_MILLIS = 15L * 60L * 1_000L
        const val DEFAULT_OFFLINE_MAXIMUM_AGE_MILLIS = 7L * 24L * 60L * 60L * 1_000L
        private const val HTTP_NOT_MODIFIED = 304
    }
}

private fun MaritimeNoticeSummaryDto.toEntity(
    existing: SeafarerMessageEntity?,
    cachedAt: Long,
): SeafarerMessageEntity {
    val remoteRevision = revision.coerceAtLeast(1)
    if (existing != null && existing.revision > remoteRevision) {
        return existing.copy(cachedAt = cachedAt)
    }
    val keepDetail = existing?.detailRevision?.let { it >= remoteRevision } == true
    return SeafarerMessageEntity(
        id = id,
        bfsNumber = bfsNumber,
        isTemporary = isTemporary,
        publisher = publisher,
        title = title,
        regionPath = regionPath,
        location = location,
        body = if (keepDetail) existing?.body.orEmpty() else "",
        publishedAt = publishedAt.toEpochMillisOrNull(),
        validFrom = validFrom.toEpochMillisOrNull(),
        validUntil = validUntil.toEpochMillisOrNull(),
        publicationState = publicationState.validPublicationState(),
        revision = remoteRevision,
        updatedAt = updatedAt.toEpochMillisOrNull() ?: cachedAt,
        sourceUrl = sourceUrl,
        chartReferencesJson = if (keepDetail) existing?.chartReferencesJson.orEmptyJson() else "[]",
        coordinatesJson = if (keepDetail) existing?.coordinatesJson.orEmptyJson() else "[]",
        previousNoticesJson = if (keepDetail) existing?.previousNoticesJson.orEmptyJson() else "[]",
        parseStatus = parseStatus.validParseStatus(),
        readRevision = existing?.readRevision ?: 0,
        detailRevision = if (keepDetail) existing?.detailRevision ?: 0 else 0,
        detailFetchedAt = if (keepDetail) existing?.detailFetchedAt else null,
        cachedAt = cachedAt,
        locallyArchived = existing?.locallyArchived ?: false,
        latitude = if (keepDetail) existing?.latitude else null,
        longitude = if (keepDetail) existing?.longitude else null,
    )
}

private fun MaritimeNoticeDetailDto.toEntity(
    existing: SeafarerMessageEntity?,
    cachedAt: Long,
): SeafarerMessageEntity {
    val remoteRevision = revision.coerceAtLeast(1)
    val primaryCoordinate = coordinates.firstOrNull()
    return SeafarerMessageEntity(
        id = id,
        bfsNumber = bfsNumber,
        isTemporary = isTemporary,
        publisher = publisher,
        title = title,
        regionPath = regionPath,
        location = location,
        body = body,
        publishedAt = publishedAt.toEpochMillisOrNull(),
        validFrom = validFrom.toEpochMillisOrNull(),
        validUntil = validUntil.toEpochMillisOrNull(),
        publicationState = publicationState.validPublicationState(),
        revision = remoteRevision,
        updatedAt = updatedAt.toEpochMillisOrNull() ?: cachedAt,
        sourceUrl = sourceUrl,
        chartReferencesJson = GsonHolder.gson.toJson(chartReferences),
        coordinatesJson = GsonHolder.gson.toJson(coordinates),
        previousNoticesJson = GsonHolder.gson.toJson(previousNotices),
        parseStatus = parseStatus.validParseStatus(),
        readRevision = existing?.readRevision ?: 0,
        detailRevision = remoteRevision,
        detailFetchedAt = cachedAt,
        cachedAt = cachedAt,
        locallyArchived = existing?.locallyArchived ?: false,
        latitude = primaryCoordinate?.latitude,
        longitude = primaryCoordinate?.longitude,
    )
}

fun SeafarerMessageEntity.chartReferences(): List<String> = GsonHolder.stringList(chartReferencesJson)

fun SeafarerMessageEntity.coordinates(): List<MaritimeNoticeCoordinateDto> = GsonHolder.coordinateList(coordinatesJson)

fun SeafarerMessageEntity.previousNotices(): List<String> = GsonHolder.stringList(previousNoticesJson)

private object GsonHolder {
    val gson = Gson()

    fun stringList(json: String): List<String> =
        runCatching {
            gson.fromJson(json, Array<String>::class.java)?.toList().orEmpty()
        }.getOrDefault(emptyList())

    fun coordinateList(json: String): List<MaritimeNoticeCoordinateDto> =
        runCatching {
            gson
                .fromJson(json, Array<MaritimeNoticeCoordinateDto>::class.java)
                ?.toList()
                .orEmpty()
        }.getOrDefault(emptyList())
}

private fun String?.orEmptyJson(): String = if (this.isNullOrBlank()) "[]" else this

private fun String.validPublicationState(): String =
    takeIf {
        it == SeafarerMessageEntity.PUBLICATION_CURRENT ||
            it == SeafarerMessageEntity.PUBLICATION_UPDATED ||
            it == SeafarerMessageEntity.PUBLICATION_REVOKED ||
            it == SeafarerMessageEntity.PUBLICATION_EXPIRED
    } ?: SeafarerMessageEntity.PUBLICATION_CURRENT

private fun String.validParseStatus(): String =
    takeIf {
        it == SeafarerMessageEntity.PARSE_PARSED ||
            it == SeafarerMessageEntity.PARSE_PARTIAL ||
            it == SeafarerMessageEntity.PARSE_FAILED
    } ?: SeafarerMessageEntity.PARSE_PARTIAL

internal fun String?.toEpochMillisOrNull(): Long? {
    val value = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrNull()
}
