package jp.org.example.geckour.glyph.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import jp.org.example.geckour.glyph.ui.StatsSequenceFragment
import jp.org.example.geckour.glyph.ui.StatsShaperFragment

class StatsFragmentPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment =
        when (position) {
            0 -> StatsShaperFragment.createInstance()

            1 -> StatsSequenceFragment.createInstance()

            else -> throw IllegalStateException()
        }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence =
            when (position) {
                0 -> "Shaper"

                1 -> "Sequence"

                else -> ""
            }
}