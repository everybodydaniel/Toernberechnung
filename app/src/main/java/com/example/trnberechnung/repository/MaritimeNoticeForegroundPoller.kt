package com.example.trnberechnung.repository

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Starts the 15-minute notice refresh only while at least one app activity is
 * visible. This avoids background polling without coupling the repository to a
 * particular Compose screen or navigation route.
 */
class MaritimeNoticeForegroundPoller(
    private val application: Application,
    private val repository: MaritimeNoticeRepository,
    private val scope: CoroutineScope,
) : Application.ActivityLifecycleCallbacks {
    private var visibleActivityCount = 0
    private var pollingJob: Job? = null
    private var isRegistered = false

    fun start() {
        if (isRegistered) return
        isRegistered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    fun stop() {
        if (!isRegistered) return
        isRegistered = false
        application.unregisterActivityLifecycleCallbacks(this)
        pollingJob?.cancel()
        pollingJob = null
        visibleActivityCount = 0
    }

    override fun onActivityStarted(activity: Activity) {
        visibleActivityCount += 1
        if (visibleActivityCount == 1) beginPolling()
    }

    override fun onActivityStopped(activity: Activity) {
        visibleActivityCount = (visibleActivityCount - 1).coerceAtLeast(0)
        if (visibleActivityCount == 0) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    private fun beginPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob =
            scope.launch {
                while (isActive) {
                    try {
                        repository.refresh(force = false)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Log.w("MaritimeNotices", "Foreground refresh failed", error)
                    }
                    delay(MaritimeNoticeRepository.DEFAULT_CACHE_TTL_MILLIS)
                }
            }
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
