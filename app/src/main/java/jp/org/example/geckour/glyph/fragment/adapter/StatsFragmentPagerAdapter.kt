package jp.org.example.geckour.glyph.fragment.adapter

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import jp.org.example.geckour.glyph.fragment.StatsSequenceFragment
import jp.org.example.geckour.glyph.fragment.StatsShaperFragment

class StatsFragmentPagerAdapter(fragmentManager: FragmentManager): FragmentPagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment? =
        when (position) {
            0 -> StatsShaperFragment.createInstance()

            1 -> StatsSequenceFragment.createInstance()

            else -> null
        }

    override fun getCount(): Int = 2

    override fun getPageTitle(position: Int): CharSequence =
            when (position) {
                0 -> "Shaper"

                1 -> "Sequence"

                else -> ""
            }
}