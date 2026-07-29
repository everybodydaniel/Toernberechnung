package com.example.trnberechnung.network

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

class MaritimeNoticeContractTest {
    private val gson = Gson()

    @Test
    fun listEnvelopeDecodesGoContract() {
        val response =
            gson.fromJson(
                """
                {
                  "notices": [{
                    "id": "notice-1",
                    "bfs_number": "BfS (T) 202/2026",
                    "is_temporary": true,
                    "publisher": "WSA Ems-Nordsee",
                    "title": "Sperrung",
                    "region_path": "Deutschland.Nordsee",
                    "location": "Baltrum",
                    "published_at": "2026-07-22T00:00:00Z",
                    "valid_from": "2026-07-22T00:00:00Z",
                    "valid_until": "2026-08-03T00:00:00Z",
                    "publication_state": "current",
                    "revision": 2,
                    "updated_at": "2026-07-22T01:00:00.123Z",
                    "source_url": "https://www.elwis.de/example",
                    "parse_status": "parsed"
                  }],
                  "next_cursor": null,
                  "last_ingested_at": "2026-07-23T14:07:00Z",
                  "is_stale": false
                }
                """.trimIndent(),
                MaritimeNoticeListResponseDto::class.java,
            )

        assertEquals(1, response.notices.size)
        assertEquals("notice-1", response.notices.single().id)
        assertEquals(2, response.notices.single().revision)
        assertEquals("2026-07-23T14:07:00Z", response.lastIngestedAt)
        assertFalse(response.isStale)
        assertNull(response.nextCursor)
    }

    @Test
    fun omittedDetailArraysDecodeAsEmpty() {
        val detail =
            gson.fromJson(
                """
                {
                  "id": "notice-1",
                  "bfs_number": "BfS 1/2026",
                  "publisher": "WSA",
                  "title": "Hinweis",
                  "region_path": "Deutschland.Nordsee",
                  "body": "Text",
                  "publication_state": "current",
                  "revision": 1,
                  "updated_at": "2026-07-22T01:00:00Z",
                  "parse_status": "partial"
                }
                """.trimIndent(),
                MaritimeNoticeDetailDto::class.java,
            )

        assertTrue(detail.chartReferences.isEmpty())
        assertTrue(detail.coordinates.isEmpty())
        assertTrue(detail.previousNotices.isEmpty())
    }

    @Test
    fun serviceUsesStatusAndConditionalRequestHeaders() {
        val method =
            SocialFeedApiService::class.java.methods.single {
                it.name == "listMaritimeNotices"
            }
        assertEquals(
            "maritime-notices",
            requireNotNull(method.getAnnotation(GET::class.java)).value,
        )
        val annotations = method.parameterAnnotations
        assertEquals("status", annotations[0].filterIsInstance<Query>().single().value)
        assertEquals("limit", annotations[1].filterIsInstance<Query>().single().value)
        assertEquals(
            "If-None-Match",
            annotations[2].filterIsInstance<Header>().single().value,
        )
    }
}
