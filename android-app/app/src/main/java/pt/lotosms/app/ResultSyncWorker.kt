package pt.lotosms.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

private const val API_BASE_URL = "https://loto-ai.a-miguel-reis.workers.dev"

class ResultSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("$API_BASE_URL/results/latest").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                connection.disconnect()
                return@withContext Result.retry()
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(body)

            if (!json.has("drawId")) return@withContext Result.success()

            val drawId = json.optString("drawId")
            val game = json.optString("game", "result")
            val title = json.optString("title", game)
            val message = json.optString("message")

            if (drawId.isNotBlank() && message.isNotBlank()) {
                Automation.onNewDraw(
                    applicationContext,
                    DrawPayload(drawId, game, title, message)
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK = "lotosms-result-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ResultSyncWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun runNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<ResultSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
