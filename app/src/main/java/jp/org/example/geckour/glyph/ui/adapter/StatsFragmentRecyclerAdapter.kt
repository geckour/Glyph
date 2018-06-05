package jp.org.example.geckour.glyph.ui.adapter

import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import jp.org.example.geckour.glyph.databinding.ItemStatsSequenceBinding
import jp.org.example.geckour.glyph.databinding.ItemStatsShaperBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.ui.model.Statistics
import jp.org.example.geckour.glyph.util.getMutableImageWithShaper
import jp.org.example.geckour.glyph.util.parse

class StatsFragmentRecyclerAdapter(private val bitmap: Bitmap) : RecyclerView.Adapter<StatsFragmentRecyclerAdapter.ViewHolder>() {

    enum class StatsType(val rawValue: Int) {
        SHAPER(0),
        SEQUENCE(1)
    }

    private val items: ArrayList<Statistics> = ArrayList()
    private val realm: Realm = Realm.getDefaultInstance()

    inner class ViewHolder : RecyclerView.ViewHolder {

        constructor(binding: ItemStatsShaperBinding) : super(binding.root) {
            shaperBinding = binding
        }

        constructor(binding: ItemStatsSequenceBinding) : super(binding.root) {
            sequenceBinding = binding
        }

        private var shaperBinding: ItemStatsShaperBinding? = null
        private var sequenceBinding: ItemStatsSequenceBinding? = null

        fun injectData(data: Statistics, type: StatsType) {
            when (type) {
                StatsType.SHAPER -> {
                    shaperBinding?.let { binding ->
                        binding.data = data.sequenceData

                        realm.where(Shaper::class.java)
                                .equalTo("id", data.sequenceData.id)
                                .findFirst()
                                ?.let {
                                    binding.data?.bitmap =
                                            bitmap.getMutableImageWithShaper(it.parse(), 0.2f)
                                }
                    }
                }

                StatsType.SEQUENCE -> {
                    sequenceBinding?.let { binding ->
                        binding.rootIndividual.tag = false
                        binding.data = data

                        realm.where(Sequence::class.java)
                                .equalTo("id", data.sequenceData.id)
                                .findFirst()
                                ?.let {
                                    it.message.toList()
                                            .map { it.parse() }
                                            .forEachIndexed { i, shaper ->
                                                binding.data?.individualData?.get(i)?.bitmap =
                                                        bitmap.getMutableImageWithShaper(shaper, 0.2f)
                                            }
                                }
                        binding.root.setOnClickListener { injectIndividualData() }
                    }
                }
            }
        }

        private fun injectIndividualData() {
            sequenceBinding?.apply {
                rootIndividual.also {
                    it.tag = !(it.tag as? Boolean ?: false)
                    if (it.tag as Boolean) {
                        data?.individualData?.apply {
                            forEachIndexed { i, d ->
                                val binding =
                                        ItemStatsShaperBinding.inflate(LayoutInflater.from(it.context),
                                                it, false)
                                binding.data = this[i]
                                if (i == this.lastIndex) binding.divider.visibility = View.GONE
                                it.addView(binding.root)
                            }
                        }
                        it.visibility = View.VISIBLE
                    } else {
                        it.removeAllViews()
                        it.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            if (items[position].individualData.isEmpty()) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
            when (viewType) {
                StatsType.SHAPER.rawValue -> {
                    ViewHolder(ItemStatsShaperBinding.inflate(LayoutInflater.from(parent.context),
                            parent, false))
                }

                StatsType.SEQUENCE.rawValue -> {
                    ViewHolder(ItemStatsSequenceBinding.inflate(LayoutInflater.from(parent.context),
                            parent, false))
                }

                else -> throw IllegalArgumentException()
            }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[holder.adapterPosition]
        holder.injectData(item,
                if (item.individualData.isEmpty()) StatsType.SHAPER
                else StatsType.SEQUENCE)
    }

    override fun getItemCount(): Int =
            items.size

    fun addItems(items: List<Statistics>) {
        val last = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(last, items.size)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        realm.close()
    }
}