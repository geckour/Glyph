package jp.org.example.geckour.glyph.fragment

import android.app.PendingIntent
import android.content.*
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.vending.billing.IInAppBillingService
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.PrefActivity
import jp.org.example.geckour.glyph.databinding.FragmentPreferenceBinding

class PrefFragment : Fragment() {

    companion object {
        val tag: String = PrefFragment::class.java.simpleName
        fun newInstance(): PrefFragment = PrefFragment()
    }

    private lateinit var binding: FragmentPreferenceBinding
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(activity) }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            billingService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            billingService = IInAppBillingService.Stub.asInterface(service)
        }
    }

    private var billingService: IInAppBillingService? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_preference, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val serviceIntent = Intent("com.android.vending.billing.InAppBillingService.BIND").apply { `package` = "com.android.vending" }
        activity.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        binding.elementMode?.apply {
            val default = 0
            var fault = false
            val type =
                    if (sp.contains(PrefActivity.Key.GAME_MODE.name)) {
                        try { sp.getInt(PrefActivity.Key.GAME_MODE.name, default) }
                        catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sp.edit().putInt(PrefActivity.Key.GAME_MODE.name, default).apply()

            value = type.toString()
            root.setOnClickListener { showModePicker() }
            summary = getString(R.string.summary_pref_game_mode, PrefActivity.HintType.values()[type].displayName)
        }

        binding.elementVibration?.apply {
            elementWidget?.widgetSwitch?.apply {
                val default = false
                visibility = View.VISIBLE
                isChecked =
                        sp.contains(PrefActivity.Key.VIBRATE.name).apply {
                            if (!this) sp.edit().putBoolean(PrefActivity.Key.VIBRATE.name, default).apply()
                        } && sp.getBoolean(PrefActivity.Key.VIBRATE.name, default)
                setOnCheckedChangeListener { _, bool ->
                    sp.edit().putBoolean(PrefActivity.Key.VIBRATE.name, bool).apply()
                }
            }
        }

        binding.elementCountHack?.apply {
            elementWidget?.widgetSwitch?.apply {
                val default = false
                visibility = View.VISIBLE
                isChecked =
                        sp.contains(PrefActivity.Key.SHOW_COUNT.name).apply {
                            if (!this) sp.edit().putBoolean(PrefActivity.Key.SHOW_COUNT.name, default).apply()
                        } && sp.getBoolean(PrefActivity.Key.SHOW_COUNT.name, default)
                setOnCheckedChangeListener { _, bool ->
                    sp.edit().putBoolean(PrefActivity.Key.SHOW_COUNT.name, bool).apply()
                }
            }
        }

        binding.elementLevelMin?.apply {
            val default = 0
            var fault = false
            val min =
                    if (sp.contains(PrefActivity.Key.LEVEL_MIN.name)) {
                        try { sp.getInt(PrefActivity.Key.LEVEL_MIN.name, default) }
                        catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sp.edit().putInt(PrefActivity.Key.LEVEL_MIN.name, default).apply()

            value = min.toString()
            summary = getString(R.string.summary_pref_minimum_level, min)
            root.setOnClickListener { showLevelPicker(PrefActivity.LevelType.MIN) }
        }

        binding.elementLevelMax?.apply {
            val default = 8
            var fault = false
            val max =
                    if (sp.contains(PrefActivity.Key.LEVEL_MAX.name)) {
                        try { sp.getInt(PrefActivity.Key.LEVEL_MAX.name, default) }
                        catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sp.edit().putInt(PrefActivity.Key.LEVEL_MAX.name, default).apply()

            value = max.toString()
            summary = getString(R.string.summary_pref_minimum_level, max)
            root.setOnClickListener { showLevelPicker(PrefActivity.LevelType.MAX) }
        }

        binding.elementDonate?.root?.setOnClickListener { onClickDonate() }

        val t: Tracker? = (activity.application as App).getDefaultTracker()
        t?.setScreenName(tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onDestroy() {
        super.onDestroy()

        if (billingService != null) {
            activity.unbindService(serviceConnection)
        }
    }

    private fun showModePicker() {
        ModePickDialogFragment.newInstance(
                try { binding.elementMode?.value?.toInt() ?: 0 }
                catch (e: NumberFormatException) { 0 }
        ).apply {
            onConfirm = { onModeChanged(it) }
        }.show(activity.supportFragmentManager, ModePickDialogFragment.tag)
    }

    private fun onModeChanged(type: Int) {
        binding.elementMode?.apply {
            value = type.toString()
            summary = getString(R.string.summary_pref_game_mode, PrefActivity.HintType.values()[type].displayName)
        }
        sp.edit().putInt(PrefActivity.Key.GAME_MODE.name, type).apply()
    }

    private fun showLevelPicker(type: PrefActivity.LevelType) {
        LevelPickDialogFragment.newInstance(
                type,
                try { binding.elementLevelMin?.value?.toInt() }
                catch (e: NumberFormatException) { null },
                try { binding.elementLevelMax?.value?.toInt() }
                catch (e: NumberFormatException) { null }
        ).apply {
            onConfirm = {
                when (type) {
                    PrefActivity.LevelType.MIN -> onMinLevelChanged(it)
                    PrefActivity.LevelType.MAX -> onMaxLevelChanged(it)
                }
            }
        }.show(activity.supportFragmentManager, LevelPickDialogFragment.tag)
    }


    private fun onMinLevelChanged(newValue: Int) {
        val max = try { binding.elementLevelMax?.value?.toInt() ?: 8 } catch (e: NumberFormatException) { 8 }

        when {
            newValue < 0 -> 0
            newValue > 8 -> 0
            else -> newValue
        }.let { value ->
            if (value > max) {
                binding.elementLevelMin?.apply {
                    this.value = max.toString()
                    summary = getString(R.string.summary_pref_minimum_level, max)
                }
                binding.elementLevelMax?.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_maximum_level, value)
                }

                sp.edit()
                        .putInt(PrefActivity.Key.LEVEL_MIN.name, max)
                        .putInt(PrefActivity.Key.LEVEL_MAX.name, value)
                        .apply()
            } else {
                binding.elementLevelMin?.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_minimum_level, value)
                }

                sp.edit()
                        .putInt(PrefActivity.Key.LEVEL_MIN.name, value)
                        .apply()
            }
        }
    }


    private fun onMaxLevelChanged(newValue: Int) {
        val min = try { binding.elementLevelMin?.value?.toInt() ?: 0 } catch (e: NumberFormatException) { 0 }

        when {
            newValue < 0 -> 0
            newValue > 8 -> 0
            else -> newValue
        }.let { value ->
            if (value < min) {
                binding.elementLevelMax?.apply {
                    this.value = min.toString()
                    summary = getString(R.string.summary_pref_maximum_level, min)
                }
                binding.elementLevelMin?.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_minimum_level, value)
                }

                sp.edit()
                        .putInt(PrefActivity.Key.LEVEL_MAX.name, min)
                        .putInt(PrefActivity.Key.LEVEL_MIN.name, value)
                        .apply()
            } else {
                binding.elementLevelMax?.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_maximum_level, value)
                }

                sp.edit()
                        .putInt(PrefActivity.Key.LEVEL_MAX.name, value)
                        .apply()
            }
        }
    }

    private fun onClickDonate(): Boolean {
        val key = "donate"
        val buyIntentArgs: Bundle? = billingService?.getBuyIntent(3, activity.packageName, key, "inapp", null)

        if (buyIntentArgs?.getInt("RESPONSE_CODE") == 0) {
            val pendingIntent: PendingIntent = buyIntentArgs.getParcelable("BUY_INTENT")
            activity.startIntentSender(pendingIntent.intentSender, Intent(), 0, 0, 0)
        }

        return true
    }
}