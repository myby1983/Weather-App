package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val email: String,
    val displayName: String,
    val photoUrl: String?,
    val idToken: String?,
    val loginTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM user_sessions LIMIT 1")
    fun getActiveSession(): Flow<UserSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clearSession()

    @Query("SELECT * FROM favorite_locations ORDER BY timestamp DESC")
    fun getFavoriteLocations(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavoriteLocation(location: FavoriteLocation)

    @Delete
    suspend fun deleteFavoriteLocation(location: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE name = :name")
    suspend fun deleteFavoriteLocationByName(name: String)
}

@Database(entities = [UserSession::class, FavoriteLocation::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
