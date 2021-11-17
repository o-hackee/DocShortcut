package ol.ko.docshortcut

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.collect
import ol.ko.docshortcut.ShortcutWidgetUtils.uriFileExists
import java.util.concurrent.TimeUnit

class FileCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    companion object {
        private const val TAG = "OLKO"

        private const val workName = "fileCheckWorker"
        private const val repeatIntervalInDays = 1L
        private const val flexIntervalInHours = 1L

        fun start(context: Context) {

            val checkFilesWorkRequest =
                PeriodicWorkRequestBuilder<FileCheckWorker>(
                    repeatIntervalInDays, TimeUnit.DAYS,
                    flexIntervalInHours, TimeUnit.HOURS
                )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiresBatteryNotLow(true)
                            .build()
                    )
                    .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.REPLACE,
                checkFilesWorkRequest
            )
        }

        fun stop(context: Context) =
            WorkManager.getInstance(context).cancelUniqueWork(workName)
    }

    override suspend fun doWork(): Result {
        FileUrisSettings(applicationContext).allUriPrefs().collect { uris ->
            uris.forEach {
                it.first?.let { appWidgetId ->
                    val fileUriString = it.second
                    val isValid = fileUriString?.uriFileExists(applicationContext) ?: false
                    Log.d(TAG, "$appWidgetId: $isValid")
                    // TODO don't repeat yourself: either request update instead of direct call or directly but only if the validity changed
                    // TODO rename fileX -> fileY -> fileX wouldn't be resolved as content uri are actually kind of links (recheck on real phone)
                    // TODO on the real device it actually works smoothly!!
                    ShortcutWidgetUtils.updateAppWidget(applicationContext, AppWidgetManager.getInstance(applicationContext), appWidgetId, fileUriString, isValid)
                }
            }
        }
        return Result.success()
    }
}