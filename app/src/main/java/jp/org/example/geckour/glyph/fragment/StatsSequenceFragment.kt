package jp.org.example.geckour.glyph.fragment

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.analytics.HitBuilders
import com.google.android.gms.analytics.Tracker
import io.realm.Realm
import io.realm.Sort
import jp.org.example.geckour.glyph.App
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.activity.StatsActivity
import jp.org.example.geckour.glyph.databinding.FragmentStatisticsBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.fragment.adapter.StatsFragmentRecyclerAdapter
import jp.org.example.geckour.glyph.fragment.model.Statistics
import jp.org.example.geckour.glyph.util.parse

class StatsSequenceFragment : Fragment() {

    companion object {
        private val tag: String = StatsSequenceFragment::class.java.simpleName
        fun createInstance(): StatsSequenceFragment =
                StatsSequenceFragment()
    }

    private lateinit var binding: FragmentStatisticsBinding
    private lateinit var adapter: StatsFragmentRecyclerAdapter
    private val realm: Realm = Realm.getDefaultInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = StatsFragmentRecyclerAdapter((activity as StatsActivity).bitmap)

        val t: Tracker? = (activity.application as App).getDefaultTracker()
        t?.setScreenName(StatsSequenceFragment.tag)
        t?.send(HitBuilders.ScreenViewBuilder().build())
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_statistics, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.adapter = adapter
        realm.where(Sequence::class.java).findAllSorted("id", Sort.ASCENDING).toList()
                .map { sequence ->
                    sequence.message.map { it.parse() }.let {
                        Statistics(
                                Statistics.Data(
                                        sequence.id,
                                        it.foldIndexed("") { i, name, shaper ->
                                            if (i == 0) shaper.name else "$name  ${shaper.name}"
                                        },
                                        sequence.correctCount,
                                        sequence.examCount
                                ),
                                it.map { Statistics.Data(it.id, it.name, it.correctCount, it.examCount) }
                        )
                    }
                }.let { adapter.addItems(it) }
    }

    override fun onDestroy() {
        super.onDestroy()

        realm.close()
    }
}