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

interface MovieNotesRepository {
	fun getAllItemsStream(): Flow<List<MovieNote>>

	fun getAllItemsStreamByMovieId(movieId: String): Flow<List<MovieNote>>

	fun getItemStream(id: String): Flow<MovieNote?>

	suspend fun insertItem(item: MovieNote)

	suspend fun deleteItem(item: MovieNote)

	suspend fun updateItem(item: MovieNote)
}

class MovieNotesRepositoryImpl(private val movieNotesDao: MovieNotesDao) : MovieNotesRepository {
	override fun getAllItemsStream(): Flow<List<MovieNote>> = movieNotesDao.getAllItems()

	override fun getAllItemsStreamByMovieId(
		movieId: String
	): Flow<List<MovieNote>> = movieNotesDao.getAllItemsByMovieId(movieId)

	override fun getItemStream(id: String): Flow<MovieNote?> = movieNotesDao.getItem(id)

	override suspend fun insertItem(item: MovieNote) = movieNotesDao.insert(item)

	override suspend fun deleteItem(item: MovieNote) = movieNotesDao.delete(item)

	override suspend fun updateItem(item: MovieNote) = movieNotesDao.update(item)
}

@Database(entities = [MovieNote::class], version = 1)
abstract class MovieNotesDatabase : RoomDatabase() {
	abstract fun dao(): MovieNotesDao

	companion object {
		@Volatile
		private var Instance: MovieNotesDatabase? = null
		fun getDatabase(context: Context): MovieNotesDatabase {
			return Instance ?: synchronized(this) {
				Room.databaseBuilder(
					context,
					MovieNotesDatabase::class.java,
					"movie_notes"
				).build()
			}.also { Instance = it }
		}
	}
}

@Dao
interface MovieNotesDao {
	@Insert
	suspend fun insert(item: MovieNote)

	@Update
	suspend fun update(item: MovieNote)

	@Delete
	suspend fun delete(item: MovieNote)

	@Query("SELECT * from movie_notes WHERE movie_id = :id")
	fun getItem(id: String): Flow<MovieNote>

	@Query("SELECT * from movie_notes ORDER BY time_of_creation DESC")
	fun getAllItems(): Flow<List<MovieNote>>

	@Query("SELECT * FROM movie_notes WHERE movie_id = :movieId ORDER BY time_of_creation DESC")
	fun getAllItemsByMovieId(movieId: String): Flow<List<MovieNote>>
}

@Entity(tableName = "movie_notes")
data class MovieNote(
	@PrimaryKey val uuid: String,
	@ColumnInfo(name = "movie_id") val movieId: String,
	@ColumnInfo(name = "text") val text: String,
	@ColumnInfo(name = "time_of_creation") val timeOfCreation: Long
)