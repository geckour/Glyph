package jp.org.example.geckour.glyph.ui.model

import com.squareup.moshi.Json

data class SkuDetail(
        val productId: String,

        val type: String,

        val price: String,

        @Json(name = "price_amount_micros")
        val priceInMicros: String,

        @Json(name = "price_currency_code")
        val priceCode: String,

        val title: String,

        val description: String
)