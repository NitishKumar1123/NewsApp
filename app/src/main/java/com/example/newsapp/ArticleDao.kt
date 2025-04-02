// ArticleDao.kt
package com.example.newsapp.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.newsapp.ArticleEntity

@Dao

interface ArticleDao {
    @Query("SELECT * FROM articles")
    suspend fun getAllArticles(): List<ArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)
}