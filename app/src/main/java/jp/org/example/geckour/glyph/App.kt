package jp.org.example.geckour.glyph

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.content.res.ResourcesCompat
import com.facebook.stetho.Stetho
import com.google.android.gms.analytics.GoogleAnalytics
import com.google.android.gms.analytics.Tracker
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import jp.org.example.geckour.glyph.db.DBInitialData.sequences
import jp.org.example.geckour.glyph.db.DBInitialData.shapers
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import timber.log.Timber.DebugTree
import timber.log.Timber
import java.util.HashMap


class App: Application() {

    companion object {
        val version = Build.VERSION.SDK_INT
        lateinit var sp: SharedPreferences
        var coda: Typeface? = null
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

        Realm.init(this)

        sp = PreferenceManager.getDefaultSharedPreferences(this)
        coda = ResourcesCompat.getFont(this, R.font.coda_regular)

        injectInitialData()
    }

    private var mTrackers = HashMap<TrackerName, Tracker>()

    @Synchronized
    internal fun getTracker(trackerId: TrackerName): Tracker? {
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

    private fun injectInitialData() {
        RealmConfiguration.Builder().initialData { realm ->
            shapers.forEachIndexed { i, shaper ->
                realm.createObject(Shaper::class.java, i).apply {
                    name = shaper.first.displayName
                    dots = shaper.second
                }
            }

            sequences.forEachIndexed { i, sequence ->
                realm.createObject(Sequence::class.java, i).apply {
                    size = sequence.size
                    message = sequence.mapTo(RealmList()) { realm.where(Shaper::class.java).equalTo("name", it.displayName).findFirstAsync() }
                }
            }
        }.build().apply {
            Realm.setDefaultConfiguration(this)
        }
    }
}