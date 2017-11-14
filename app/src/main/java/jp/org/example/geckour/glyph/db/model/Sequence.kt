package jp.org.example.geckour.glyph.db.model

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


open class Sequence(
        @PrimaryKey
        var id: Long = 0,

        var size: Int = 0,

        var message: RealmList<Shaper> = RealmList()
): RealmObject()