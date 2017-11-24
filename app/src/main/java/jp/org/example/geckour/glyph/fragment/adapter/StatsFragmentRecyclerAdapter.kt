package jp.org.example.geckour.glyph.fragment.adapter

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.realm.Realm
import jp.org.example.geckour.glyph.R
import jp.org.example.geckour.glyph.databinding.ItemStatsSequenceBinding
import jp.org.example.geckour.glyph.databinding.ItemStatsShaperBinding
import jp.org.example.geckour.glyph.db.model.Sequence
import jp.org.example.geckour.glyph.db.model.Shaper
import jp.org.example.geckour.glyph.fragment.model.Statistics
import jp.org.example.geckour.glyph.util.getMutableImageWithShaper
import jp.org.example.geckour.glyph.util.parse

class StatsFragmentRecyclerAdapter(private val bitmap: Bitmap): RecyclerView.Adapter<StatsFragmentRecyclerAdapter.ViewHolder>() {

    enum class StatsType(val rawValue: Int) {
        SHAPER(0),
        SEQUENCE(1)
    }

    private val items: ArrayList<Statistics> = ArrayList()
    private val realm: Realm = Realm.getDefaultInstance()

    inner class ViewHolder: RecyclerView.ViewHolder {
        constructor(binding: ItemStatsShaperBinding): super(binding.root) {
            shaperBinding = binding
        }
        constructor(binding: ItemStatsSequenceBinding): super(binding.root) {
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
                                    binding.shaperImg.setImageBitmap(
                                            bitmap.getMutableImageWithShaper(it.parse(), 0.2f)
                                    )
                                }
                    }
                }

                StatsType.SEQUENCE -> {
                    sequenceBinding?.let{ binding ->
                        binding.rootIndividual.tag = false
                        binding.data = data.sequenceData
                        val bitmaps: ArrayList<Bitmap> = ArrayList()
                        listOf(
                                binding.sequenceImg3,
                                binding.sequenceImg4,
                                binding.sequenceImg5
                        ).forEach {
                            it.setImageBitmap(null)
                            it.visibility = View.GONE
                        }
                        realm.where(Sequence::class.java)
                                .equalTo("id", data.sequenceData.id)
                                .findFirst()
                                ?.let {
                                    it.message.toList().map { it.parse() }.forEachIndexed { i, shaper ->
                                        bitmaps.add(bitmap.getMutableImageWithShaper(shaper, 0.2f))

                                        when (i) {
                                            0 -> binding.sequenceImg1.setImageBitmap(bitmaps.last())

                                            1 -> binding.sequenceImg2.setImageBitmap(bitmaps.last())

                                            2 -> binding.sequenceImg3.apply {
                                                setImageBitmap(bitmaps.last())
                                                visibility = View.VISIBLE
                                            }

                                            3 -> binding.sequenceImg4.apply {
                                                setImageBitmap(bitmaps.last())
                                                visibility = View.VISIBLE
                                            }

                                            4 -> binding.sequenceImg5.apply {
                                                setImageBitmap(bitmaps.last())
                                                visibility = View.VISIBLE
                                            }
                                        }
                                    }
                                }
                        binding.root.setOnClickListener { injectIndividualData(data.individualData, bitmaps) }
                    }
                }
            }
        }

        private fun injectIndividualData(data: List<Statistics.Data>, bitmaps: List<Bitmap>) {
            sequenceBinding?.rootIndividual?.apply {
                tag = !(tag as Boolean)
                if (tag as Boolean) {
                    data.forEachIndexed { i, d ->
                        val binding = DataBindingUtil.inflate<ItemStatsShaperBinding>(LayoutInflater.from(this.context), R.layout.item_stats_shaper, this, false)
                        binding.data = d
                        binding.shaperImg.setImageBitmap(bitmaps[i])
                        if (i == data.lastIndex) binding.divider.visibility = View.GONE
                        addView(binding.root)
                    }
                    visibility = View.VISIBLE
                } else {
                    removeAllViews()
                    visibility = View.GONE
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            if (items[position].individualData.isEmpty()) 0 else 1

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder? =
            parent?.let {
                when (viewType) {
                    StatsType.SHAPER.rawValue -> ViewHolder(DataBindingUtil.inflate<ItemStatsShaperBinding>(LayoutInflater.from(it.context), R.layout.item_stats_shaper, it, false))
                    StatsType.SEQUENCE.rawValue -> ViewHolder(DataBindingUtil.inflate<ItemStatsSequenceBinding>(LayoutInflater.from(it.context), R.layout.item_stats_sequence, it, false))
                    else -> null
                }
            }

    override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        val item = items[position]
        holder?.injectData(item, if (item.individualData.isEmpty()) StatsType.SHAPER else StatsType.SEQUENCE)
    }

    override fun getItemCount(): Int =
            items.size

    fun addItems(items: List<Statistics>) {
        val last = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(last, items.size)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        super.onDetachedFromRecyclerView(recyclerView)

        realm.close()
    }
}