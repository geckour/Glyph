package jp.org.example.geckour.glyph.ui

import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v7.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.vending.billing.IInAppBillingService
import com.squareup.moshi.Moshi
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.FragmentPreferenceBinding
import jp.org.example.geckour.glyph.ui.model.SkuDetail
import jp.org.example.geckour.glyph.util.*

class PrefFragment : Fragment() {

    companion object {
        val tag: String = PrefFragment::class.java.simpleName
        fun newInstance(): PrefFragment = PrefFragment()
    }

    private lateinit var binding: FragmentPreferenceBinding
    private lateinit var sharedPreferences: SharedPreferences

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName) {
            billingService = null
        }

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            billingService = IInAppBillingService.Stub.asInterface(service)
        }
    }

    private var billingService: IInAppBillingService? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentPreferenceBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)

        val serviceIntent =
                Intent("com.android.vending.billing.InAppBillingService.BIND").apply {
                    `package` = "com.android.vending"
                }
        activity?.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        binding.elementMode.apply {
            val default = 0
            var fault = false
            val type =
                    if (sharedPreferences.contains(Key.GAME_MODE.name)) {
                        try {
                            sharedPreferences.getInt(Key.GAME_MODE.name, default)
                        } catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sharedPreferences.edit().putInt(Key.GAME_MODE.name, default).apply()

            value = type.toString()
            root.setOnClickListener { showModePicker() }
            summary = getString(R.string.summary_pref_game_mode, HintType.values()[type].displayName)
        }

        binding.elementVibration.apply {
            elementWidget?.widgetSwitch?.apply {
                visibility = View.VISIBLE

                isChecked = sharedPreferences.getBooleanValue(Key.VIBRATE)

                setOnCheckedChangeListener { _, bool ->
                    sharedPreferences.edit().putBoolean(Key.VIBRATE.name, bool).apply()
                }
            }
        }

        binding.elementCountHack.apply {
            elementWidget?.widgetSwitch?.apply {
                visibility = View.VISIBLE

                isChecked = sharedPreferences.getBooleanValue(Key.SHOW_COUNT)

                setOnCheckedChangeListener { _, bool ->
                    sharedPreferences.edit().putBoolean(Key.SHOW_COUNT.name, bool).apply()
                }
            }
        }

        binding.elementLevelMin.apply {
            val default = 0
            var fault = false
            val min =
                    if (sharedPreferences.contains(Key.LEVEL_MIN.name)) {
                        try {
                            sharedPreferences.getInt(Key.LEVEL_MIN.name, default)
                        } catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sharedPreferences.edit().putInt(Key.LEVEL_MIN.name, default).apply()

            value = min.toString()
            summary = getString(R.string.summary_pref_minimum_level, min)
            root.setOnClickListener { showLevelPicker(LevelPickDialogFragment.LevelType.MIN) }
        }

        binding.elementLevelMax.apply {
            val default = 8
            var fault = false
            val max =
                    if (sharedPreferences.contains(Key.LEVEL_MAX.name)) {
                        try {
                            sharedPreferences.getInt(Key.LEVEL_MAX.name, default)
                        } catch (e: Exception) {
                            fault = true
                            default
                        }
                    } else {
                        fault = true
                        default
                    }
            if (fault) sharedPreferences.edit().putInt(Key.LEVEL_MAX.name, default).apply()

            value = max.toString()
            summary = getString(R.string.summary_pref_minimum_level, max)
            root.setOnClickListener { showLevelPicker(LevelPickDialogFragment.LevelType.MAX) }
        }

        binding.elementDonate.root.setOnClickListener { onClickDonate() }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (billingService != null) {
            activity?.unbindService(serviceConnection)
        }
    }

    private fun showModePicker() {
        ModePickDialogFragment.newInstance(
                try {
                    binding.elementMode.value?.toInt() ?: 0
                } catch (e: NumberFormatException) {
                    0
                }
        ).apply {
            onConfirm = { onModeChanged(it) }
        }.show(activity?.supportFragmentManager, ModePickDialogFragment.tag)
    }

    private fun onModeChanged(type: Int) {
        binding.elementMode.apply {
            value = type.toString()
            summary = getString(R.string.summary_pref_game_mode, HintType.values()[type].displayName)
        }
        sharedPreferences.edit().putInt(Key.GAME_MODE.name, type).apply()
    }

    private fun showLevelPicker(type: LevelPickDialogFragment.LevelType) {
        LevelPickDialogFragment.newInstance(
                type,
                try {
                    binding.elementLevelMin.value?.toInt()
                } catch (e: NumberFormatException) {
                    null
                },
                try {
                    binding.elementLevelMax.value?.toInt()
                } catch (e: NumberFormatException) {
                    null
                }
        ).apply {
            onConfirm = {
                when (type) {
                    LevelPickDialogFragment.LevelType.MIN -> onMinLevelChanged(it)
                    LevelPickDialogFragment.LevelType.MAX -> onMaxLevelChanged(it)
                }
            }
        }.show(activity?.supportFragmentManager, LevelPickDialogFragment.tag)
    }


    private fun onMinLevelChanged(newValue: Int) {
        val max = try {
            binding.elementLevelMax.value?.toInt() ?: 8
        } catch (e: NumberFormatException) {
            8
        }

        when {
            newValue < 0 -> 0
            newValue > 8 -> 0
            else -> newValue
        }.let { value ->
            if (value > max) {
                binding.elementLevelMin.apply {
                    this.value = max.toString()
                    summary = getString(R.string.summary_pref_minimum_level, max)
                }
                binding.elementLevelMax.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_maximum_level, value)
                }

                sharedPreferences.edit()
                        .putInt(Key.LEVEL_MIN.name, max)
                        .putInt(Key.LEVEL_MAX.name, value)
                        .apply()
            } else {
                binding.elementLevelMin.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_minimum_level, value)
                }

                sharedPreferences.edit()
                        .putInt(Key.LEVEL_MIN.name, value)
                        .apply()
            }
        }
    }


    private fun onMaxLevelChanged(newValue: Int) {
        val min = try {
            binding.elementLevelMin.value?.toInt() ?: 0
        } catch (e: NumberFormatException) {
            0
        }

        when {
            newValue < 0 -> 0
            newValue > 8 -> 0
            else -> newValue
        }.let { value ->
            if (value < min) {
                binding.elementLevelMax.apply {
                    this.value = min.toString()
                    summary = getString(R.string.summary_pref_maximum_level, min)
                }
                binding.elementLevelMin.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_minimum_level, value)
                }

                sharedPreferences.edit()
                        .putInt(Key.LEVEL_MAX.name, min)
                        .putInt(Key.LEVEL_MIN.name, value)
                        .apply()
            } else {
                binding.elementLevelMax.apply {
                    this.value = value.toString()
                    summary = getString(R.string.summary_pref_maximum_level, value)
                }

                sharedPreferences.edit()
                        .putInt(Key.LEVEL_MAX.name, value)
                        .apply()
            }
        }
    }

    private fun onClickDonate() {
        billingService?.let {
            ui {
                val type = "inapp"
                val key = "donate"
                val sku =
                        it.getSkuDetails(3, activity?.packageName, type, Bundle().apply {
                            putStringArrayList(
                                    "ITEM_ID_LIST",
                                    ArrayList(listOf(key)))
                        }).let {
                            if (it.getInt("RESPONSE_CODE") == 0) {
                                it.getStringArrayList("DETAILS_LIST").map {
                                    Moshi.Builder().build()
                                            .adapter(SkuDetail::class.java)
                                            .fromJson(it)
                                }
                            } else listOf()
                        }.firstOrNull() ?: return@ui

                val purchasedSkus =
                        it.getPurchases(3,
                                activity?.packageName, type, null
                        ).getStringArrayList("INAPP_PURCHASE_ITEM_LIST")

                if (purchasedSkus.contains(sku.productId).not()) {
                    val pendingIntent: PendingIntent =
                            it.getBuyIntent(3,
                                    activity?.packageName, key, type, null)?.let {
                                if (it.containsKey("RESPONSE_CODE")
                                        && it.getInt("RESPONSE_CODE") == 0)
                                    it.getParcelable("BUY_INTENT")
                                else null
                            } ?: return@ui

                    activity?.startIntentSender(pendingIntent.intentSender,
                            Intent(), 0, 0, 0)
                }
            }
        }
    }
}