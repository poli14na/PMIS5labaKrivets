package com.krivets.movies.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
	fun getAllItemsStream(): Flow<List<Favorite>>

	fun getItemStream(uuid: String): Flow<Favorite?>

	suspend fun insertItem(item: Favorite)

	suspend fun deleteItem(item: Favorite)

	suspend fun updateItem(item: Favorite)
}

class FavoritesRepositoryImpl(private val favoritesDao: FavoritesDao) : FavoritesRepository {
	override fun getAllItemsStream(): Flow<List<Favorite>> = favoritesDao.getAllItems()

	override fun getItemStream(uuid: String): Flow<Favorite?> = favoritesDao.getItem(uuid)

	override suspend fun insertItem(item: Favorite) = favoritesDao.insert(item)

	override suspend fun deleteItem(item: Favorite) = favoritesDao.delete(item)

	override suspend fun updateItem(item: Favorite) = favoritesDao.update(item)
}

@Database(entities = [Favorite::class], version = 1)
abstract class FavoritesDatabase : RoomDatabase() {
	abstract fun dao(): FavoritesDao

	companion object {
		@Volatile
		private var Instance: FavoritesDatabase? = null
		fun getDatabase(context: Context): FavoritesDatabase {
			return Instance ?: synchronized(this) {
				Room.databaseBuilder(
					context,
					FavoritesDatabase::class.java,
					"favorites"
				).build()
			}.also { Instance = it }
		}
	}
}

@Dao
interface FavoritesDao {
	@Insert
	suspend fun insert(item: Favorite)

	@Update
	suspend fun update(item: Favorite)

	@Delete
	suspend fun delete(item: Favorite)

	@Query("SELECT * from favorites WHERE movieId = :uuid")
	fun getItem(uuid: String): Flow<Favorite?>

	@Query("SELECT * from favorites ORDER BY movieId ASC")
	fun getAllItems(): Flow<List<Favorite>>
}

@Entity(tableName = "favorites")
data class Favorite(
	@PrimaryKey val movieId: String,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo(name = "imageUrl") val imageUrl: String
)