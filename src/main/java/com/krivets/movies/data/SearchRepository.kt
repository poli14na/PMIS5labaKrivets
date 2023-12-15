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
import com.pmorozova.movies.NetworkClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow

class SearchRepository(
	networkClient: NetworkClient,
	private val searchHistoryDao: SearchHistoryDao
) : SearchHistoryRepository {
	private val httpClient = networkClient.httpClient

	suspend fun getSearchPage(searchQuery: String, page: Int? = null): TitlesPage? {
		val pageParameter = if (page == null) "" else "?page=$page"
		val response = httpClient.get("/titles/search/keyword/$searchQuery$pageParameter")

		return if (response.status.isSuccess()) {
			response.body()
		} else {
			null
		}
	}

	override fun getAllItemsStream(): Flow<List<SearchHistoryItem>> = searchHistoryDao.getAllItems()

	override fun getItemStream(id: String): Flow<SearchHistoryItem?> = searchHistoryDao.getItem(id)

	override suspend fun insertItem(item: SearchHistoryItem) = searchHistoryDao.insert(item)

	override suspend fun deleteItem(item: SearchHistoryItem) = searchHistoryDao.delete(item)

	override suspend fun updateItem(item: SearchHistoryItem) = searchHistoryDao.update(item)
}

interface SearchHistoryRepository {
	fun getAllItemsStream(): Flow<List<SearchHistoryItem>>

	fun getItemStream(id: String): Flow<SearchHistoryItem?>

	suspend fun insertItem(item: SearchHistoryItem)

	suspend fun deleteItem(item: SearchHistoryItem)

	suspend fun updateItem(item: SearchHistoryItem)
}

@Database(entities = [SearchHistoryItem::class], version = 1)
abstract class SearchHistoryDatabase : RoomDatabase() {
	abstract fun dao(): SearchHistoryDao

	companion object {
		@Volatile
		private var Instance: SearchHistoryDatabase? = null
		fun getDatabase(context: Context): SearchHistoryDatabase {
			return Instance ?: synchronized(this) {
				Room.databaseBuilder(
					context,
					SearchHistoryDatabase::class.java,
					"search_history"
				).build()
			}.also { Instance = it }
		}
	}
}

@Dao
interface SearchHistoryDao {
	@Insert
	suspend fun insert(item: SearchHistoryItem)

	@Update
	suspend fun update(item: SearchHistoryItem)

	@Delete
	suspend fun delete(item: SearchHistoryItem)

	@Query("SELECT * from search_history WHERE id = :id")
	fun getItem(id: String): Flow<SearchHistoryItem?>

	@Query("SELECT * from search_history ORDER BY time_of_creation DESC")
	fun getAllItems(): Flow<List<SearchHistoryItem>>
}

@Entity(tableName = "search_history")
data class SearchHistoryItem(
	@PrimaryKey val id: String,
	@ColumnInfo(name = "title") val title: String,
	@ColumnInfo("time_of_creation") val timeOfCreation: Long
)