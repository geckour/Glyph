package jp.org.example.geckour.glyph.fragment.model

data class Statistics(
        val sequenceData: Data,
        val individualData: List<Data>
) {
    data class Data(
            val id: Long,
            val name: String,
            val correctCount: Long,
            val totalCount: Long
    )
}