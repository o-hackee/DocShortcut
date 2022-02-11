package ol.ko.docshortcut.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ol.ko.docshortcut.GlanceWidgetUtils
import java.util.concurrent.TimeUnit

class FileCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    companion object {
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
        GlanceWidgetUtils.updateWidgets(applicationContext)
        return Result.success()
    }
}