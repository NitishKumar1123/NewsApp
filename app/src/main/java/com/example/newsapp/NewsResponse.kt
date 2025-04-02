// NewsResponse.kt
package com.example.newsapp

data class NewsResponse(
    val status: String?,
    val totalResults: Int?,
    val articles: List<Article>
)

