package dev.lucasnlm.antimine.preferences

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import dev.lucasnlm.antimine.R
import dev.lucasnlm.antimine.core.cloud.CloudSaveManager
import dev.lucasnlm.antimine.core.models.Analytics
import dev.lucasnlm.antimine.isAndroidTv
import dev.lucasnlm.antimine.language.LanguageSelectorActivity
import dev.lucasnlm.antimine.themes.ThemeActivity
import dev.lucasnlm.antimine.ui.ThematicActivity
import dev.lucasnlm.antimine.ui.ext.toAndroidColor
import dev.lucasnlm.antimine.ui.ext.toInvertedAndroidColor
import dev.lucasnlm.antimine.ui.repository.IThemeRepository
import dev.lucasnlm.external.IAnalyticsManager
import kotlinx.android.synthetic.main.activity_preferences.*
import org.koin.android.ext.android.inject

class PreferencesActivity :
    ThematicActivity(R.layout.activity_preferences),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val preferenceRepository: IPreferencesRepository by inject()
    private val themeRepository: IThemeRepository by inject()
    private val cloudSaveManager by inject<CloudSaveManager>()

    @XmlRes
    private var currentTabXml: Int = R.xml.gameplay_preferences

    private fun getColorListCompat(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked),
        )

        val typedValue = TypedValue()
        theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)

        val colors = intArrayOf(
            typedValue.data.toAndroidColor(),
            themeRepository.getTheme().palette.background.toInvertedAndroidColor(160)
        )

        return ColorStateList(states, colors)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        bindToolbar(preferenceRepository.hasCustomizations())

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        navView.apply {
            setBackgroundColor(themeRepository.getTheme().palette.background)
            val colorList = getColorListCompat()
            itemIconTintList = colorList
            itemTextColor = colorList
            setOnNavigationItemSelectedListener {
                currentTabXml = R.xml.gameplay_preferences
                when (it.itemId) {
                    R.id.gameplay -> placePreferenceFragment(R.xml.gameplay_preferences)
                    R.id.appearance -> placePreferenceFragment(R.xml.appearance_preferences)
                    R.id.general -> placePreferenceFragment(R.xml.general_preferences)
                }
                true
            }
        }

        placePreferenceFragment(R.xml.gameplay_preferences)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()

        cloudSaveManager.uploadSave()

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun placePreferenceFragment(@XmlRes targetPreferences: Int) {
        supportFragmentManager.apply {
            popBackStack()

            findFragmentByTag(PrefsFragment.TAG)?.let { it ->
                beginTransaction().apply {
                    remove(it)
                    commitAllowingStateLoss()
                }
            }

            beginTransaction().apply {
                replace(R.id.preference_fragment, PrefsFragment.newInstance(targetPreferences), PrefsFragment.TAG)
                commitAllowingStateLoss()
            }
        }
    }

    private fun bindToolbar(hasCustomizations: Boolean) {
        if (hasCustomizations) {
            section.bind(
                text = R.string.settings,
                startButton = R.drawable.back_arrow,
                startDescription = R.string.back,
                startAction = {
                    finish()
                },
                endButton = R.drawable.delete,
                endDescription = R.string.delete_all,
                endAction = {
                    preferenceRepository.reset()
                    placePreferenceFragment(currentTabXml)
                    bindToolbar(false)
                }
            )
        } else {
            section.bind(
                text = R.string.settings,
                startButton = R.drawable.back_arrow,
                startDescription = R.string.back,
                startAction = {
                    finish()
                }
            )
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        bindToolbar(preferenceRepository.hasCustomizations())
    }

    class PrefsFragment : PreferenceFragmentCompat() {
        private val analyticsManager: IAnalyticsManager by inject()

        private var targetPreferences: Int = 0

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            // Load the preferences from an XML resource
            targetPreferences = arguments?.getInt(TARGET_PREFS, 0) ?: 0
            if (targetPreferences != 0) {
                addPreferencesFromResource(targetPreferences)
            }

            if (requireContext().isAndroidTv()) {
                listOf(
                    PreferenceKeys.PREFERENCE_USE_HELP,
                    PreferenceKeys.PREFERENCE_LONG_PRESS_TIMEOUT,
                    PreferenceKeys.PREFERENCE_TOUCH_SENSIBILITY,
                    PreferenceKeys.PREFERENCE_VIBRATION,
                    PreferenceKeys.PREFERENCE_SHOW_WINDOWS,
                    PreferenceKeys.PREFERENCE_OPEN_DIRECTLY,
                    SELECT_THEME_PREFS
                ).forEach {
                    findPreference<Preference>(it)?.isVisible = false
                }
            }

            findPreference<Preference>(SELECT_THEME_PREFS)?.setOnPreferenceClickListener {
                analyticsManager.sentEvent(Analytics.OpenSettings)
                Intent(context, ThemeActivity::class.java).apply {
                    startActivity(this)
                }
                true
            }

            findPreference<Preference>(SELECT_LANGUAGE)?.setOnPreferenceClickListener {
                analyticsManager.sentEvent(Analytics.OpenSelectLanguage)
                Intent(context, LanguageSelectorActivity::class.java).apply {
                    startActivity(this)
                }
                true
            }
        }

        companion object {
            val TAG = PrefsFragment::class.simpleName

            private const val TARGET_PREFS = "target_prefs"
            private const val SELECT_THEME_PREFS = "preference_select_theme"
            private const val SELECT_LANGUAGE = "preference_select_language"

            fun newInstance(targetPreferences: Int): PrefsFragment {
                val args = Bundle().apply {
                    putInt(TARGET_PREFS, targetPreferences)
                }

                return PrefsFragment().apply {
                    arguments = args
                }
            }
        }
    }
}
