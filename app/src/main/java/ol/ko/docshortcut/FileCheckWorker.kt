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
        val fileUrisSettings = FileUrisSettings(applicationContext)
        fileUrisSettings.allUriPrefs().collect { uris ->
            uris.forEach { pair ->
                pair.first?.let { appWidgetId ->
                    val savedUriString = pair.second
                    savedUriString?.let {
                        val isCurrentlyValid = savedUriString.uriString.uriFileExists(applicationContext)
                        Log.d(TAG, "$appWidgetId: $isCurrentlyValid")
                        if (isCurrentlyValid != it.lastIsValid) {
                            ShortcutWidgetUtils.updateAppWidget(applicationContext, AppWidgetManager.getInstance(applicationContext), appWidgetId, savedUriString.uriString, isCurrentlyValid)
                        }
                    }
                }
            }
        }
        return Result.success()
    }
}