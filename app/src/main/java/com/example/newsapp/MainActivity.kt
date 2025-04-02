package com.example.newsapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.compose.rememberImagePainter
import com.example.newsapp.db.ArticleDao
import com.example.newsapp.db.ArticleDatabase
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var articleDao: ArticleDao
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val GEOAPIFY_API_KEY = "d31d13edd6d3447dbb8fee1eb9e33c98"
        private const val NEWS_API_KEY = "2aaf3b43868a413a9dda8bd8257f4a66"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = ArticleDatabase.getDatabase(applicationContext)
        articleDao = database.articleDao()

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request location permissions using lifecycleScope
        lifecycleScope.launch {
            requestLocationPermissions()
        }

        setContent {
            NewsApp(articleDao)
        }
    }

    private suspend fun requestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fetchCurrentLocation()
        }
    }

    private suspend fun fetchCurrentLocation(): Location {
        return suspendCancellableCoroutine { continuation ->
            val context = this
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Handle permission denied
                continuation.resumeWithException(SecurityException("Location permission not granted"))
            } else {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            continuation.resume(it)
                        } ?: continuation.resumeWithException(NullPointerException("Location is null"))
                    }
                    .addOnFailureListener { e ->
                        // Handle failure to get location
                        continuation.resumeWithException(e)
                    }
            }
        }
    }

    private suspend fun fetchCityName(latitude: Double, longitude: Double): String {
        return suspendCancellableCoroutine { continuation ->
            lifecycleScope.launch {
                try {
                    val geoapifyUrl =
                        "https://api.geoapify.com/v1/geocode/reverse?lat=$latitude&lon=$longitude&apiKey=$GEOAPIFY_API_KEY"
                    val response = URL(geoapifyUrl).readText()
                    val cityName = parseCityName(response)
                    continuation.resume(cityName)
                } catch (e: Exception) {
                    // Handle JSON parsing error or network error
                    Log.e("ParseCityName", "Error parsing city name", e)
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    private fun parseCityName(response: String): String {
        val jsonObject = Gson().fromJson(response, JsonObject::class.java)
        val featuresArray = jsonObject.getAsJsonArray("features")
        return if (featuresArray.size() > 0) {
            val firstFeature = featuresArray.get(0).asJsonObject
            val properties = firstFeature.getAsJsonObject("properties")
            properties.get("county").asString
        } else {
            // No features found
            ""
        }
    }

    private suspend fun fetchNewsByCity(context: Context, articleDao: ArticleDao, city: String): List<Article> {
        val encodedCity = withContext(Dispatchers.IO) {
            URLEncoder.encode(city, "UTF-8")
        }
        val newsApiUrl = "https://newsapi.org/v2/everything?q=$encodedCity&apiKey=$NEWS_API_KEY"
        return suspendCancellableCoroutine { continuation ->
            lifecycleScope.launch {
                try {
                    val url = URL(newsApiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000 // Adjust timeout as needed
                    connection.readTimeout = 10000 // Adjust timeout as needed

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val newsResponse = Gson().fromJson(response, NewsResponse::class.java)
                        continuation.resume(newsResponse.articles)
                    } else {
                        // Handle unsuccessful response (e.g., log error, show message, etc.)
                        Log.e("FetchNews", "Unsuccessful response: ${connection.responseCode}")
                        continuation.resume(emptyList())
                    }
                } catch (e: Exception) {
                    // Handle network error or other exceptions
                    Log.e("FetchNews", "Error in fetching news", e)
                    showToast(context, "Error in fetching news")
                    continuation.resume(emptyList())
                }
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                lifecycleScope.launch {
                    fetchCurrentLocation()
                }
            } else {
                // Handle permission denied
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                }
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null) {
                    return networkInfo.isConnected &&
                            (networkInfo.type == ConnectivityManager.TYPE_WIFI ||
                                    networkInfo.type == ConnectivityManager.TYPE_MOBILE ||
                                    networkInfo.type == ConnectivityManager.TYPE_ETHERNET)
                }
            }
        }
        return false
    }

    @Composable
    fun NewsApp(articleDao: ArticleDao) {
        val context = LocalContext.current
        var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
        var articlesToSave by remember { mutableStateOf<List<Article>>(emptyList()) }
        var showToastMessage by remember { mutableStateOf(false) }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fetch News Button
                Button(onClick = {
                    lifecycleScope.launch {
                        if (isInternetConnected(context)) {
                            val location = fetchCurrentLocation()
//                            print(latitude,longitude)
                            val cityName = fetchCityName(location.latitude, location.longitude)
                            print(cityName)
                            articles = fetchNewsByCity(context, articleDao, cityName)
                            if (articles.isEmpty()) {
                                showToast(context, "No articles available")
                            }
                        } else {
                            articles = loadNewsOffline(articleDao)
                            if (articles.isEmpty()) {
                                showToast(context, "No articles available offline")
                            } else {
                                showToast(context, "Showing offline news")
                            }
                        }
                    }
                }) {
                    Text(text = "Fetch News")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Save News Button
                Button(onClick = {
                    articlesToSave = articles
                    showToastMessage = true
                }) {
                    Text(text = "Save News")
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Share News Button
                Button(onClick = {
                    if (articles.isNotEmpty()) {
                        shareArticle(context, articles.first())
                    }
                }) {
                    Text(text = "Share")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display articles if available
            if (articles.isNotEmpty()) {
                DisplayArticles(articles)
            }

            // Show toast message when articles are saved
            if (showToastMessage) {
                Toast.makeText(context, "Articles saved successfully!", Toast.LENGTH_SHORT).show()
                showToastMessage = false
            }
        }
    }

    private suspend fun loadNewsOffline(articleDao: ArticleDao): List<Article> {
        val articleEntities = articleDao.getAllArticles()
        return articleEntities.map { articleEntity ->
            // Properly initialize Source object
            val source = Source(name = articleEntity.sourceName ?: "")
            // Create Article instance
            Article(
                source = source,
                author = articleEntity.author,
                title = articleEntity.title,
                description = articleEntity.description,
                url = articleEntity.url,
                imageUrl = articleEntity.imageUrl,
                publishedAt = articleEntity.publishedAt,
                content = articleEntity.content
            )
        }
    }

    @Composable
    fun DisplayArticles(articles: List<Article>) {
        LazyColumn {
            itemsIndexed(articles) { _, article ->
                ArticleItem(article)
            }
        }
    }

    @Composable
    fun ArticleItem(article: Article) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                article.source?.name?.let { sourceName ->
                    Text(
                        text = "Source: $sourceName",
                        style = TextStyle(fontWeight = FontWeight.Bold)
                    )
                }
                Text(text = "Author: ${article.author}")
                Text(
                    text = "Title: ${article.title}",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Text(text = "Description: ${article.description}")
                article.url?.let { url ->
                    Text(
                        text = "URL: $url",
                        color = Color.Blue,
                        modifier = Modifier.clickable { openUrlInBrowser(url) }
                    )
                }
                if (!article.imageUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberImagePainter(article.imageUrl),
                        contentDescription = "Article Image",
                        modifier = Modifier.size(200.dp)
                    )
                }
                Text(text = "Published At: ${article.publishedAt}")
                Text(text = "Content: ${article.content}")
            }
        }
    }

    private fun openUrlInBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(this@MainActivity, intent, null)
    }
}

// Define function to share article
fun shareArticle(context: Context, article: Article) {
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, article.title)
    shareIntent.putExtra(Intent.EXTRA_TEXT, article.url)
    val chooserIntent = Intent.createChooser(shareIntent, "Share article via")
    context.startActivity(chooserIntent)
}
