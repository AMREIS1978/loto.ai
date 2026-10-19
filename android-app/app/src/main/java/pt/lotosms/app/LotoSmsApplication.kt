package pt.lotosms.app

import android.app.Application

class LotoSmsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ResultSyncWorker.schedule(this)
        ResultSyncWorker.runNow(this)
    }
}
