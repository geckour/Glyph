package jp.org.example.geckour.glyph.db.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class Shaper(
        @PrimaryKey
        var id: Long = 0,

        var name: String = "",

        var dots: List<Int> = listOf()
): RealmObject()