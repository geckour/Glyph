package jp.org.example.geckour.glyph.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import jp.org.example.geckour.glyph.adapter.StatsFragmentRecyclerAdapter
import jp.org.example.geckour.glyph.databinding.FragmentStatisticsBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.ui.model.Statistics
import jp.org.example.geckour.glyph.util.parse

class StatsSequenceFragment : Fragment() {

    companion object {
        fun createInstance(): StatsSequenceFragment =
                StatsSequenceFragment()
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

        val splitList: List<List<Statistics>> = List(4) {
            getSequenceStatistics(it + 2).sortedBy { it.sequenceData.name }
        }

        adapter.addItems(splitList.reversed().flatten())
    }

    private fun getSequenceStatistics(difficulty: Int): List<Statistics> {
        if ((difficulty in 2..5).not()) return emptyList()

        return realm.where(Sequence::class.java)
                .equalTo("size", difficulty)
                .findAll()
                .toList()
                .map { sequence ->
                    sequence.message.map { it.parse() }.let {
                        Statistics(
                                Statistics.Data(sequence.id,
                                        it.joinToString("  ") { it.name },
                                        sequence.correctCount, sequence.examCount, null),
                                it.map {
                                    Statistics.Data(it.id,
                                            it.name,
                                            it.correctCount, it.examCount, null)
                                }
                        )
                    }
                }
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }
}