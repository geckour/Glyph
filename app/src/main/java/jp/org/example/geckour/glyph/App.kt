package jp.org.example.geckour.glyph

import android.app.Application
import android.graphics.Bitmap
import com.facebook.stetho.Stetho
import com.squareup.moshi.Moshi
import io.realm.Realm
import io.realm.RealmConfiguration
import io.realm.RealmList
import jp.org.example.geckour.glyph.adapter.MoshiBitmapAdapter
import jp.org.example.geckour.glyph.db.DBInitialData.sequences
import jp.org.example.geckour.glyph.db.DBInitialData.shapers
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import timber.log.Timber
import timber.log.Timber.DebugTree


class App : Application() {

    companion object {
        var scale: Float = -1f
        val moshi: Moshi by lazy {
            Moshi.Builder()
                    .add(Bitmap::class.java, MoshiBitmapAdapter())
                    .add(AppJsonAdapterFactory.INSTANCE)
                    .build()
        }
    }

    override fun onCreate() {
        super.onCreate()

        //if (BuildConfig.DEBUG) {
        Timber.plant(DebugTree())
        //}
        Stetho.initializeWithDefaults(this)

        Realm.init(this)
        injectInitialDBData()

        if (BuildConfig.DEBUG) {
            //injectDummyDBData()
        }
    }

    private fun injectInitialDBData() {
        RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .initialData { realm ->
                    shapers.forEachIndexed { i, shaper ->
                        realm.createObject(Shaper::class.java, i).apply {
                            name = shaper.first.displayName
                            dots = shaper.second
                        }
                    }

                    sequences.forEachIndexed { i, sequence ->
                        realm.createObject(Sequence::class.java, i).apply {
                            size = sequence.size
                            message =
                                    sequence.mapTo(RealmList()) {
                                        realm.where(Shaper::class.java)
                                                .equalTo("name", it.displayName)
                                                .findFirstAsync()
                                    }
                        }
                    }
                }.build().apply { Realm.setDefaultConfiguration(this) }
    }

    private fun injectDummyDBData() {
        Realm.getDefaultInstance().let { realm ->
            realm.where(Shaper::class.java)
                    .findAll()
                    .toList()
                    .apply {
                        realm.executeTransaction {
                            this.forEach {
                                it.examCount = it.id + 1
                                it.correctCount = 1L
                            }
                        }
                    }
            realm.where(Sequence::class.java)
                    .findAll()
                    .toList()
                    .apply {
                        realm.executeTransaction {
                            this.forEach {
                                it.examCount = it.id + 1
                                it.correctCount = 1L
                            }
                        }
                    }
        }
    }
}