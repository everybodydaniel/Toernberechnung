package com.example.trnberechnung.repository

import com.example.trnberechnung.database.MaritimeNoticeSyncDao
import com.example.trnberechnung.database.MaritimeNoticeSyncEntity
import com.example.trnberechnung.database.SeafarerMessageDao
import com.example.trnberechnung.database.SeafarerMessageEntity
import com.example.trnberechnung.network.MaritimeNoticeListResponseDto
import com.example.trnberechnung.network.SocialFeedApiService
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class MaritimeNoticeRepositoryTest {
    private val noticeDao = mockk<SeafarerMessageDao>(relaxed = true)
    private val syncDao = mockk<MaritimeNoticeSyncDao>(relaxed = true)
    private val api = mockk<SocialFeedApiService>()
    private var clock = 1_000_000L

    @Before
    fun setUp() {
        coEvery { noticeDao.getAllOnce() } returns emptyList()
        coEvery { syncDao.get() } returns null
        every { syncDao.observe() } returns flowOf(null)
        every { noticeDao.observeAll() } returns flowOf(emptyList())
        every { noticeDao.observeCurrent(any()) } returns flowOf(emptyList())
        every { noticeDao.observeUnread(any()) } returns flowOf(emptyList())
        every { noticeDao.observeArchive(any()) } returns flowOf(emptyList())
        every { noticeDao.getUnreadCount() } returns flowOf(0)
    }

    @Test
    fun refreshUsesExactListContractAndStoresRevisionSafeSummary() =
        runTest {
            val payload =
                Gson().fromJson(
                    """
                    {
                      "notices": [{
                        "id": "n-1",
                        "bfs_number": "BfS 1/2026",
                        "is_temporary": false,
                        "publisher": "WSA",
                        "title": "Hinweis",
                        "region_path": "Deutschland.Nordsee",
                        "published_at": "2026-07-22T00:00:00Z",
                        "publication_state": "updated",
                        "revision": 3,
                        "updated_at": "2026-07-22T01:00:00Z",
                        "parse_status": "parsed"
                      }],
                      "last_ingested_at": "2026-07-22T01:01:00Z",
                      "is_stale": false
                    }
                    """.trimIndent(),
                    MaritimeNoticeListResponseDto::class.java,
                )
            coEvery {
                api.listMaritimeNotices("all", 100, null)
            } returns Response.success(payload, Headers.headersOf("ETag", "\"etag-1\""))
            coEvery { noticeDao.getAllOnce() } returns
                listOf(
                    cachedNotice().copy(
                        id = "n-1",
                        revision = 2,
                        readRevision = 2,
                        detailRevision = 2,
                        body = "Alte Revision",
                    ),
                )
            val inserted = slot<List<SeafarerMessageEntity>>()
            coEvery { noticeDao.insertAll(capture(inserted)) } returns Unit
            val metadata = slot<MaritimeNoticeSyncEntity>()
            coEvery { syncDao.upsert(capture(metadata)) } returns Unit

            val result = repository().refresh()

            assertEquals(MaritimeNoticeSource.NETWORK, result.source)
            assertEquals(3, inserted.captured.single().revision)
            assertEquals(2, inserted.captured.single().readRevision)
            assertTrue(inserted.captured.single().readRevision < inserted.captured.single().revision)
            assertEquals(0, inserted.captured.single().detailRevision)
            assertEquals("updated", inserted.captured.single().publicationState)
            assertEquals("\"etag-1\"", metadata.captured.etag)
            assertFalse(metadata.captured.isStale)
            coVerify(exactly = 1) { api.listMaritimeNotices("all", 100, null) }
        }

    @Test
    fun notModifiedAdvancesTtlWithoutReplacingNotices() =
        runTest {
            val metadata =
                MaritimeNoticeSyncEntity(
                    fetchedAt = clock - 20L * 60L * 1_000L,
                    lastIngestedAt = 123L,
                    etag = "\"etag\"",
                    isStale = true,
                )
            val cached = cachedNotice()
            coEvery { syncDao.get() } returns metadata
            coEvery { noticeDao.getAllOnce() } returns listOf(cached)
            val rawResponse =
                okhttp3.Response
                    .Builder()
                    .request(Request.Builder().url("https://example.invalid/maritime-notices").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(304)
                    .message("Not Modified")
                    .build()
            coEvery {
                api.listMaritimeNotices("all", 100, "\"etag\"")
            } returns Response.error("".toResponseBody(null), rawResponse)

            val result = repository().refresh()

            assertEquals(MaritimeNoticeSource.NOT_MODIFIED, result.source)
            assertEquals(listOf(cached), result.notices)
            coVerify(exactly = 0) { noticeDao.insertAll(any()) }
            coVerify {
                syncDao.upsert(
                    match {
                        it.fetchedAt == clock &&
                            !it.isStale &&
                            it.etag == "\"etag\""
                    },
                )
            }
        }

    @Test
    fun failedRefreshReturnsSevenDayCacheAsStale() =
        runTest {
            val cached = cachedNotice()
            val metadata =
                MaritimeNoticeSyncEntity(
                    fetchedAt = clock - 60_000L,
                    etag = "\"etag\"",
                )
            coEvery { syncDao.get() } returns metadata
            coEvery { noticeDao.getAllOnce() } returns listOf(cached)
            coEvery {
                api.listMaritimeNotices("all", 100, "\"etag\"")
            } throws IOException("offline")

            val result = repository().refresh(force = true)

            assertEquals(MaritimeNoticeSource.OFFLINE_CACHE, result.source)
            assertTrue(result.isStale)
            assertEquals(listOf(cached), result.notices)
            coVerify {
                syncDao.upsert(
                    match { it.fetchedAt == metadata.fetchedAt && it.isStale },
                )
            }
        }

    @Test(expected = MaritimeNoticeUnavailableException::class)
    fun failedRefreshRejectsCacheOlderThanSevenDays() =
        runTest {
            coEvery { syncDao.get() } returns
                MaritimeNoticeSyncEntity(
                    fetchedAt =
                        clock -
                            MaritimeNoticeRepository.DEFAULT_OFFLINE_MAXIMUM_AGE_MILLIS -
                            1,
                )
            coEvery { noticeDao.getAllOnce() } returns listOf(cachedNotice())
            coEvery { api.listMaritimeNotices(any(), any(), any()) } throws
                IOException("offline")

            repository().refresh(force = true)
        }

    private fun repository() =
        MaritimeNoticeRepository(
            noticeDao = noticeDao,
            syncDao = syncDao,
            api = api,
            now = { clock },
        )

    private fun cachedNotice() =
        SeafarerMessageEntity(
            id = "cached",
            bfsNumber = "BfS 1/2026",
            publisher = "WSA",
            title = "Hinweis",
            regionPath = "Deutschland.Nordsee",
            updatedAt = clock - 60_000L,
            cachedAt = clock - 60_000L,
        )
}
