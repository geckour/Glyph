package jp.org.example.geckour.glyph

import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.facebook.stetho.Stetho
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import timber.log.Timber.DebugTree
import timber.log.Timber
import java.util.HashMap
import kotlin.coroutines.experimental.CoroutineContext


class App: Application() {

    companion object {
        val version = Build.VERSION.SDK_INT
        lateinit var sp: SharedPreferences
    }

    enum class TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER // Tracker used by all ecommerce transactions from a company.
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        Stetho.initializeWithDefaults(this)

        sp = PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var mTrackers = HashMap<TrackerName, Tracker>()

    @Synchronized internal fun getTracker(trackerId: TrackerName): Tracker? {
        if (!mTrackers.containsKey(trackerId)) {
            val analytics = GoogleAnalytics.getInstance(this)
            val t = when(trackerId) {
                TrackerName.APP_TRACKER -> analytics.newTracker(R.xml.app_tracker)
                TrackerName.GLOBAL_TRACKER -> analytics.newTracker(R.xml.global_tracker)
                else -> analytics.newTracker(R.xml.ecommerce_tracker)
            }
            t.enableAdvertisingIdCollection(true)
            mTrackers.put(trackerId, t)
        }
        return mTrackers[trackerId]
    }
}