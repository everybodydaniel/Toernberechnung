package com.example.trnberechnung.messaging

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trnberechnung.TideNodeApplication
import com.example.trnberechnung.repository.AccountNotActiveException
import com.example.trnberechnung.repository.ChatApiException
import com.example.trnberechnung.repository.ChatUnavailableException
import java.io.IOException

class ChatOutboxWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val ownerId = inputData.getString(KEY_OWNER_ID) ?: return Result.failure()
        val localId = inputData.getString(KEY_LOCAL_ID) ?: return Result.failure()
        val repository = (applicationContext as TideNodeApplication).chatRepository

        return try {
            repository.flushQueuedMessage(ownerId, localId)
            Result.success()
        } catch (_: AccountNotActiveException) {
            // Keep the account-scoped outbox entry. It is rescheduled when that
            // Firebase account becomes active again.
            Result.success()
        } catch (error: ChatApiException) {
            if (error.statusCode in 400..499) Result.failure() else retryOrFail(ownerId, localId, error)
        } catch (error: ChatUnavailableException) {
            repository.markMessageFailed(ownerId, localId, error.message.orEmpty())
            Result.failure()
        } catch (error: IOException) {
            retryOrFail(ownerId, localId, error)
        } catch (error: Throwable) {
            retryOrFail(ownerId, localId, error)
        }
    }

    private suspend fun retryOrFail(
        ownerId: String,
        localId: String,
        error: Throwable,
    ): Result {
        if (runAttemptCount < MAX_RETRIES) return Result.retry()
        (applicationContext as TideNodeApplication).chatRepository.markMessageFailed(
            ownerId = ownerId,
            localId = localId,
            reason = error.localizedMessage ?: "Nachricht konnte nicht gesendet werden.",
        )
        return Result.failure()
    }

    companion object {
        const val KEY_OWNER_ID = "owner_id"
        const val KEY_LOCAL_ID = "local_id"
        private const val MAX_RETRIES = 5
    }
}
