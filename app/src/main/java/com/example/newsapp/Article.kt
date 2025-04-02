// Article.kt
package com.example.newsapp

import com.google.gson.annotations.SerializedName

data class Article(
    val source: Source?,
    val author: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    @SerializedName("urlToImage")
    val imageUrl: String?,
    @SerializedName("publishedAt")
    val publishedAt: String?,
    val content: String?
)


