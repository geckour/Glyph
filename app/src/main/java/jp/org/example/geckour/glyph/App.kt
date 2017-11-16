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

    private var tracker: Tracker? = null

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(DebugTree())
        }
        Stetho.initializeWithDefaults(this)

        Realm.init(this)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder().deleteRealmIfMigrationNeeded().build())

        sp = PreferenceManager.getDefaultSharedPreferences(this)
        coda = ResourcesCompat.getFont(this, R.font.coda_regular)

        injectInitialData()
    }

    @Synchronized
    internal fun getDefaultTracker(): Tracker? =
            tracker ?: GoogleAnalytics.getInstance(this).newTracker(R.xml.global_tracker)

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