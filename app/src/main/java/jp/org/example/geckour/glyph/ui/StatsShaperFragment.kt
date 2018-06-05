package jp.org.example.geckour.glyph.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import jp.org.example.geckour.glyph.databinding.FragmentStatisticsBinding
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.ui.adapter.StatsFragmentRecyclerAdapter
import jp.org.example.geckour.glyph.ui.model.Statistics
import jp.org.example.geckour.glyph.util.parse

class StatsShaperFragment : Fragment() {

    companion object {
        fun createInstance(): StatsShaperFragment =
                StatsShaperFragment()
    }

    private lateinit var binding: FragmentStatisticsBinding
    private lateinit var adapter: StatsFragmentRecyclerAdapter
    private val realm: Realm = Realm.getDefaultInstance()

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = StatsFragmentRecyclerAdapter((activity as StatsActivity).bitmap)
        binding.recyclerView.adapter = adapter

        realm.where(Shaper::class.java).findAll().toList()
                .map {
                    it.parse().let {
                        Statistics(
                                Statistics.Data(it.id, it.name, it.correctCount, it.examCount, null),
                                listOf())
                    }
                }.let { adapter.addItems(it) }
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }
}