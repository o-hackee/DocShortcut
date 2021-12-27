package ol.ko.docshortcut.work

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ol.ko.docshortcut.ShortcutAppWidget
import ol.ko.docshortcut.utils.ContentResolverUtils
import ol.ko.docshortcut.utils.FileUrisDataStore
import ol.ko.docshortcut.utils.FileUrisRepository
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
        val fileUrisRepository = FileUrisRepository(FileUrisDataStore.getInstance(applicationContext))
        val uris = fileUrisRepository.allUriPrefs()
//        println("uris: $uris")
        uris.forEach { pair ->
            pair.first?.let { appWidgetId ->
                val savedUriString = pair.second
                savedUriString?.let {
                    val isCurrentlyValid = ContentResolverUtils.uriFileExists(applicationContext.contentResolver, savedUriString.uriString)
                    Log.d(TAG, "$appWidgetId: $isCurrentlyValid")
//                    println("$appWidgetId: $isCurrentlyValid")
                    if (isCurrentlyValid != it.lastIsValid) {
                        ShortcutAppWidget.updateAppWidget(
                            applicationContext,
                            AppWidgetManager.getInstance(applicationContext),
                            appWidgetId,
                            savedUriString.uriString,
                            isCurrentlyValid
                        )
                    }
                }
            }
        }
//        println("return doWork")
        return Result.success()
    }
}