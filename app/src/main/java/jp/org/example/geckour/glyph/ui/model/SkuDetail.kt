package jp.org.example.geckour.glyph.ui.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SkuDetail(
        @Json(name = "productId")
        val productId: String,

        @Json(name = "type")
        val type: String,

        @Json(name = "price")
        val price: String,

        @Json(name = "price_amount_micros")
        val priceInMicros: String,

        @Json(name = "price_currency_code")
        val priceCode: String,

        @Json(name = "title")
        val title: String,

        @Json(name = "description")
        val description: String
)