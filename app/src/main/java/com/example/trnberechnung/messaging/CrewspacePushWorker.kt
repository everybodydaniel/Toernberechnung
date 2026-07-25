package com.example.trnberechnung.messaging

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.trnberechnung.MainActivity
import com.example.trnberechnung.R
import com.example.trnberechnung.TideNodeApplication
import com.example.trnberechnung.repository.AccountNotActiveException
import com.example.trnberechnung.repository.AuthenticationRequiredException
import com.example.trnberechnung.repository.ChatApiException
import java.util.concurrent.TimeUnit

class CrewspacePushWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? TideNodeApplication ?: return Result.failure()
        val expectedOwnerId = inputData.getString(KEY_OWNER_ID) ?: return Result.failure()
        val conversationId = inputData.getString(KEY_CONVERSATION_ID) ?: return Result.failure()
        val messageId = inputData.getString(KEY_MESSAGE_ID) ?: return Result.failure()
        inputData.getString(KEY_MESSAGE_TYPE) ?: return Result.failure()

        if (!isStillEligible(app, expectedOwnerId, conversationId)) return Result.success()

        val fallbackTitle =
            runCatching {
                app.chatRepository.localPushSender(
                    expectedOwnerId = expectedOwnerId,
                    conversationId = conversationId,
                    messageId = messageId,
                )
            }.getOrNull()
        val titleResult =
            runCatching {
                app.chatRepository.resolvePushSender(
                    expectedOwnerId = expectedOwnerId,
                    conversationId = conversationId,
                    messageId = messageId,
                )
            }
        val title =
            titleResult.getOrElse { error ->
                when {
                    error is AccountNotActiveException ||
                        error is AuthenticationRequiredException -> return Result.success()
                    error is ChatApiException &&
                        error.statusCode in listOf(401, 403, 404) -> return Result.success()
                    else -> fallbackTitle ?: "Crewspace"
                }
            } ?: return Result.success()

        if (!isStillEligible(app, expectedOwnerId, conversationId)) return Result.success()
        InstallationIdStore(applicationContext).runIfRegisteredFor(expectedOwnerId) {
            if (
                app.authRepository.isLoggedIn &&
                app.authRepository.skipperId == expectedOwnerId &&
                ChatNavigationState.activeConversationId.value != conversationId
            ) {
                CrewspacePushNotifications.show(
                    context = applicationContext,
                    ownerId = expectedOwnerId,
                    conversationId = conversationId,
                    messageId = messageId,
                    senderTitle = title,
                )
            }
        }
        return Result.success()
    }

    private fun isStillEligible(
        app: TideNodeApplication,
        expectedOwnerId: String,
        conversationId: String,
    ): Boolean =
        app.authRepository.isLoggedIn &&
            app.authRepository.skipperId == expectedOwnerId &&
            InstallationIdStore(applicationContext).isRegisteredFor(expectedOwnerId) &&
            ChatNavigationState.activeConversationId.value != conversationId

    companion object {
        private const val KEY_OWNER_ID = "owner_id"
        private const val KEY_CONVERSATION_ID = "conversation_id"
        private const val KEY_MESSAGE_ID = "message_id"
        private const val KEY_MESSAGE_TYPE = "message_type"

        internal fun enqueue(
            context: Context,
            ownerId: String,
            envelope: CrewspacePushEnvelope,
        ) {
            val data =
                Data.Builder()
                    .putString(KEY_OWNER_ID, ownerId)
                    .putString(KEY_CONVERSATION_ID, envelope.conversationId)
                    .putString(KEY_MESSAGE_ID, envelope.messageId)
                    .putString(KEY_MESSAGE_TYPE, envelope.messageType)
                    .build()
            val request =
                OneTimeWorkRequestBuilder<CrewspacePushWorker>()
                    .setInputData(data)
                    .addTag(crewspacePushOwnerWorkTag(ownerId))
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10,
                        TimeUnit.SECONDS,
                    )
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                crewspacePushUniqueWorkName(ownerId, envelope.messageId),
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        internal fun cancelForOwner(
            context: Context,
            ownerId: String,
        ) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag(crewspacePushOwnerWorkTag(ownerId))
        }
    }
}

object CrewspacePushNotifications {
    private const val TAG_PREFIX = "crewspace:"

    fun show(
        context: Context,
        ownerId: String,
        conversationId: String,
        messageId: String,
        senderTitle: String,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(CrewspaceMessagingService.EXTRA_CONVERSATION_ID, conversationId)
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                "$ownerId:$conversationId".hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(context, TideNodeApplication.CHAT_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_chat_notification)
                .setContentTitle(senderTitle)
                .setContentText("Neue Nachricht")
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        NotificationManagerCompat.from(context).notify(
            ownerTag(ownerId),
            messageId.hashCode(),
            notification,
        )
    }

    fun cancelForOwner(
        context: Context,
        ownerId: String,
    ) {
        if (ownerId.isBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val manager = context.getSystemService(NotificationManager::class.java)
        runCatching {
            manager.activeNotifications
                .filter { it.tag == ownerTag(ownerId) }
                .forEach { manager.cancel(it.tag, it.id) }
        }
    }

    private fun ownerTag(ownerId: String) = "$TAG_PREFIX$ownerId"
}
