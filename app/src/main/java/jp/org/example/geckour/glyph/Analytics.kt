package jp.org.example.geckour.glyph

import android.app.Application

import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker

import java.util.HashMap

class Analytics : Application() {

    enum class TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER // Tracker used by all ecommerce transactions from a company.
    }

    internal var mTrackers = HashMap<TrackerName, Tracker>()

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
