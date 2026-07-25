package com.example.trnberechnung.network

import org.junit.Assert.assertEquals
import org.junit.Test

class SecureBaseUrlTest {
    @Test
    fun acceptsOnlyHttpsWithHostAndNormalizesTrailingSlash() {
        assertEquals(
            "https://chat.example.de/api/",
            secureHttpsBaseUrl("HTTPS://chat.example.de/api"),
        )
        assertEquals(
            "https://example.invalid/",
            secureHttpsBaseUrl("http://192.0.2.10:8080/"),
        )
        assertEquals(
            "https://example.invalid/",
            secureHttpsBaseUrl("https:///missing-host"),
        )
        assertEquals(
            "https://example.invalid/",
            secureHttpsBaseUrl("not a URL"),
        )
    }
}
