# Local News Tracker App

## Overview
Local News Tracker is an Android application that provides location-based news updates. The app utilizes Jetpack Compose for UI, Room Database for offline storage, and APIs to fetch local and world news.

## Features
- **Location-Based News:** Fetches local news using the Fused Location Provider API.
- **Offline Mode:** Uses Room Database to store news articles for offline access.
- **Multiple API Support:** Allows users to switch between local and world news APIs.
- **Search Functionality:** Users can search for news by location or domain.
- **Settings Management:** Persistent user preferences for API selection and filters.
- **Jetpack Compose UI:** Uses modern UI components like LazyColumn and FloatingActionButton.

## App Flow
### 1. App Launch & Initialization
- Initializes core components in `MainActivity.kt`.
- Loads user preferences from `AppSettings.kt`.
- Requests location permissions if not granted.

### 2. Location Handling
- Uses Fused Location Provider to fetch the user's location.
- Implements reverse geocoding to derive city, state, and country.
- Allows manual location entry if location services fail.

### 3. API Selection & Data Fetching
- Supports **Local News API** (`NewsApi.kt`) and **World News API** (`WorldNewsApi.kt`).
- Users select API preferences in `SettingsActivity.kt`.
- Fetches news articles using `NewsApiRoutes.kt` or `WorldNewsApiRoutes.kt`.

### 4. UI Rendering (Jetpack Compose)
- **TopAppBar:** Displays the app title and navigation options.
- **LazyColumn:** Dynamically lists news articles.
- **FloatingActionButton:** Quick access to settings and search.
- **ModalDrawerSheet:** Navigation drawer for switching news sources and accessing settings.

### 5. Data Persistence & Offline Functionality
- Uses Room Database (`NewsItemDatabase.kt`) to store news articles.
- If offline, retrieves previously stored news.
- News data updates on every API fetch.

### 6. Asynchronous Operations
- Uses Kotlin Coroutines for:
  - Fetching news from APIs.
  - Storing and retrieving data from the Room database.
  - Ensuring smooth, non-blocking operations.

### 7. Search Functionality
- Users can search by:
  - **Current Location** (uses GPS data).
  - **City, State, Country** (manual input).
  - **News Domains** (e.g., ABP News, Times of India).
- API calls are triggered based on selected filters.

### 8. Error Handling
- **Network Connectivity Check:** Uses `ConnectivityManager` to check internet availability before making API requests.
- **Graceful Degradation:** Displays error messages and fetches offline data in case of API failures.

### 9. Settings Screen
- Users can:
  - Customize news sources.
  - Switch between Local and World News APIs.
  - Enable/disable location-based news fetching.
- Settings are saved persistently using `AppSettings.kt`.

### 10. Navigation & User Interaction
- Jetpack Compose Navigation enables smooth transitions between:
  - Local News
  - World News
  - Settings Screen
- Managed through `Navigationitem.kt`.

---

## ER Diagram Description
### Entities
- **User**: Represents app users.
- **News Item**: Stores fetched news articles.
- **API Source**: Represents Local and World News APIs.
- **Location**: User’s location (city, state, country).
- **Settings**: Stores user preferences (API selection, filters).
- **Database**: Room database storing news articles for offline access.

### Relationships
- Users **choose** an API source.
- API Source **provides** multiple News Items.
- Users **set preferences** in Settings.
- Users are **associated with** a Location.
- Locations **filter** News Items.
- News Items are **stored in** the Database.

---

## Role in Project
**"My role in the project was to design and implement data persistence and offline functionality using Room Database to ensure that users can access previously fetched news articles even when offline."**

## Why We Chose Room for Data Persistence
- **Improved User Experience:** Ensures news is accessible even offline.
- **Performance Optimization:** Reduces API calls by caching news locally.
- **Persistent Data:** Stored news articles remain accessible across app sessions.
- **Efficiency:** Room provides SQLite database management with compile-time checks and LiveData support.

## Technical Implementation
### a. Room Database Setup
1. **NewsItemEntity**: Defines the schema for storing news articles.
2. **NewsItemDao**: Contains database queries (`@Query`, `@Insert`, `@Delete`).
3. **NewsItemDatabase**: Manages database instances using the Singleton pattern.

### b. Data Caching Strategy
- Checks internet availability before fetching news.
- Fetches news from the API if online, stores results in Room.
- Retrieves cached news from Room if offline.

### c. Handling Offline Access
- If no internet, queries the Room database instead of making API calls.
- Displays cached news articles until the user goes online.

### d. Auto-Update Local Data
- Replaces outdated news with fresh articles upon new API fetches.
- Implements data expiration to clear old news periodically.

## Tools and Libraries Used
- **Room Database:** Abstraction over SQLite for structured data storage.
- **Jetpack Compose:** Modern UI toolkit for dynamic rendering.
- **Kotlin Coroutines:** Handles background API calls and database operations.
- **Fused Location Provider API:** Retrieves user location efficiently.
- **Retrofit:** Manages API calls for news fetching.
- **ConnectivityManager:** Monitors network connectivity for smart data handling.

## Challenges & Solutions
- **Data Synchronization:** Ensured smooth sync between API and local data.
  - **Solution:** Implemented an expiration mechanism to clear outdated articles.
- **Offline Handling:** Prevented crashes when switching between online and offline modes.
  - **Solution:** Implemented fallback mechanisms and user notifications.

## Benefits of Using Room Database
✅ **Offline Availability**: Users can read news without internet.  
✅ **Reduced Data Consumption**: API calls minimized through caching.  
✅ **Improved Performance**: Faster access to locally stored data.  

## Conclusion
The implementation of Room Database significantly enhanced the app’s functionality. Users can now access news seamlessly regardless of network conditions, ensuring reliability and efficiency in news delivery.

---

## Installation & Setup
### Prerequisites
- Android Studio (Latest Version)
- Kotlin Support Enabled

### Steps to Run the App
1. Clone the repository:
   ```sh
   git clone https://github.com/NitishKumar1123/NewsApp.git
   ```
2. Open the project in Android Studio.
3. Sync dependencies and build the project.
4. Run the app on an emulator or physical device.

### Permissions Required
- **Internet Access**: To fetch news from APIs.
- **Location Access**: To provide location-based news.

## Future Enhancements
🚀 **Push Notifications**: Alert users about breaking news.  
🚀 **Dark Mode Support**: Improve UI experience in low-light conditions.  
🚀 **More API Integrations**: Expand news sources for broader coverage.  

---

## License
This project is licensed under the MIT License - see the LICENSE file for details.
