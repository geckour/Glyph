package jp.org.example.geckour.glyph.activity

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.preference.*
import com.android.vending.billing.IInAppBillingService

import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.App.Companion.sp
import jp.org.example.geckour.glyph.R
import org.json.JSONObject
import timber.log.Timber
import java.util.*

class Pref : PreferenceActivity() {

    companion object {
        private val tag: String = this::class.java.simpleName
        fun createIntent(activity: Activity): Intent =
                Intent(activity, Pref::class.java)

        internal var min: Int = 0
        internal var max: Int = 0
        internal var minLevelPref: EditTextPreference? = null
        internal var maxLevelPref: EditTextPreference? = null
        internal var donatePref: PreferenceScreen? = null
        internal val skuList = ArrayList<String>()
        internal var mService: IInAppBillingService? = null
        internal val mServiceConn = object: ServiceConnection {
            override fun onServiceDisconnected(name: ComponentName) {
                mService = null
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                mService = IInAppBillingService.Stub.asInterface(service)
            }
        }

        fun isMinLevelChanged(preference: Preference, newValue: Any): Boolean {
            val editTextPreference = preference as EditTextPreference
            var level: Int
            var isException = false
            try {
                level = Integer.parseInt(newValue.toString())
            } catch (e: Exception) {
                isException = true
                Timber.d("Can't translate minimum level into Integer. min:$min, max:$max")
                sp.edit().putString("min_level", min.toString()).apply()
                level = min
            }

            val maxLevel = Integer.parseInt(sp.getString("max_level", "0"))
            if (-1 < level && level < 9) {
                if (level > maxLevel) {
                    sp.edit().putString("min_level", maxLevel.toString()).apply()
                    editTextPreference.text = maxLevel.toString()
                    preference.summary = maxLevel.toString()
                    sp.edit().putString("max_level", level.toString()).apply()
                    maxLevelPref?.text = level.toString()
                    maxLevelPref?.summary = level.toString()
                    return false
                } else {
                    return if (isException) {
                        preference.summary = min.toString()
                        false
                    } else {
                        preference.summary = newValue.toString()
                        true
                    }
                }
            } else {
                if (level > maxLevel) {
                    sp.edit().putString("min_level", maxLevel.toString()).apply()
                    preference.summary = maxLevel.toString()
                } else {
                    sp.edit().putString("min_level", "0").apply()
                    preference.summary = "0"
                }
                return false
            }
        }

        fun isMaxLevelChanged(preference: Preference, newValue: Any): Boolean {
            val editTextPreference = preference as EditTextPreference
            var level: Int
            var isException = false
            try {
                level = Integer.parseInt(newValue.toString())
            } catch (e: Exception) {
                isException = true
                Timber.d("Can't translate maximum level into Integer. min:$min, max:$max")
                sp.edit().putString("max_level", max.toString()).apply()
                level = max
            }

            val minLevel = Integer.parseInt(sp.getString("min_level", "0"))
            if (-1 < level && level < 9) {
                if (level < minLevel) {
                    sp.edit().putString("min_level", level.toString()).apply()
                    minLevelPref?.text = level.toString()
                    minLevelPref?.summary = level.toString()
                    sp.edit().putString("max_level", minLevel.toString()).apply()
                    editTextPreference.text = minLevel.toString()
                    preference.summary = minLevel.toString()
                    return false
                } else {
                    return if (isException) {
                        preference.summary = max.toString()
                        false
                    } else {
                        preference.summary = newValue.toString()
                        true
                    }
                }
            } else {
                if (level < minLevel) {
                    sp.edit().putString("max_level", minLevel.toString()).apply()
                    preference.summary = minLevel.toString()
                } else {
                    sp.edit().putString("max_level", "8").apply()
                    preference.summary = "8"
                }
                return false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sp = PreferenceManager.getDefaultSharedPreferences(this)
        try {
            min = Integer.parseInt(sp.getString("min_level", "0"))
            max = Integer.parseInt(sp.getString("max_level", "8"))
        } catch (e: Exception) {
            min = 0
            max = 8
            Timber.d("Can't translate level into Integer. min:$min, max:$max")
        }

        fragmentManager.beginTransaction().replace(android.R.id.content, PrefFragment()).commit()

        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND")
        serviceIntent.`package` = "com.android.vending"
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE)

        val t: Tracker? = (application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())

    }

    override fun onDestroy() {
        super.onDestroy()
        if (mService != null) {
            unbindService(mServiceConn)
        }
    }

    class PrefFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            //設定画面を追加
            addPreferencesFromResource(R.xml.preferences)
            //preferences.xml内のmin_levelが変更されたかをListen
            minLevelPref = findPreference("min_level") as EditTextPreference
            minLevelPref?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->
                        isMinLevelChanged(preference, newValue)
                    }
            minLevelPref?.summary = minLevelPref?.text

            //preferences.xml内のmax_levelが変更されたかをListen
            maxLevelPref = findPreference("max_level") as EditTextPreference
            maxLevelPref?.onPreferenceChangeListener =
                    Preference.OnPreferenceChangeListener { preference, newValue ->
                        isMaxLevelChanged(preference, newValue)
                    }
            maxLevelPref?.summary = maxLevelPref?.text
            //preferences.xml内のdonateがクリックされたかをListen
            donatePref = findPreference("donate") as PreferenceScreen
            donatePref?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val sku = "donate"

                skuList.add(sku)
                val querySkus = Bundle()
                querySkus.putStringArrayList("ITEM_ID_LIST", skuList)
                val skuDetails: Bundle? = mService?.getSkuDetails(3, (activity as Pref).packageName, "inapp", querySkus)
                val response = skuDetails?.getInt("RESPONSE_CODE")

                if (response == 0) {
                    val responseList = skuDetails?.getStringArrayList("DETAILS_LIST")
                    var isMatchSku = false
                    responseList?.forEach {
                        val obj = JSONObject(it)
                        val productName = obj.getString("productId")
                        if (productName == sku) isMatchSku = true
                    }
                    if (isMatchSku) {
                        val buyIntentBundle: Bundle? = mService?.getBuyIntent(3, (activity as Pref).packageName, sku, "inapp", "")
                        val pendingIntent = buyIntentBundle?.getParcelable<PendingIntent>("BUY_INTENT")
                        (activity as Pref).startIntentSenderForResult(pendingIntent?.intentSender, 1001, Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0))
                    }
                }
                /*
                        val buyIntentBundle: Bundle? = mService?.getBuyIntent(3, pref!!.packageName, "android.test.purchased", "inapp", "")
                        val pendingIntent = buyIntentBundle?.getParcelable<PendingIntent>("BUY_INTENT")
                        pref!!.startIntentSenderForResult(pendingIntent?.intentSender, 1001, Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0))
                        */
                true
            }
        }
    }
}