package ol.ko.docshortcut

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

class FileCheckWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {
    companion object {
        const val workName = "fileCheckWorker"
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
    }

    override suspend fun doWork(): Result {
        FileUrisSettings(applicationContext).allUriPrefs().collect { uris ->
            uris.forEach {
                Log.d("OLKO", it)
            }
        }
        return Result.success()
    }
}