package com.krivets.movies.data

import com.krivets.movies.NetworkClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

class MovieRepository(networkClient: NetworkClient) {
	private val httpClient = networkClient.httpClient

	suspend fun getTitlesPage(
		page: Int? = null,
		genre: String? = null,
		movieList: String? = null
	) = runCatching {
		val pageParameter = if (page == null) "" else "?page=$page"
		val response = httpClient.get("/titles$pageParameter") {
			if (genre != null) parameter("genre", genre)
			if (movieList != null) parameter("list", movieList)
		}

		response.body<TitlesPage>()
	}

	suspend fun getRandomTitle(): Result<TitleInfo> = runCatching {
		return@runCatching httpClient.get("/titles/random") {
			parameter("titleType", "movie")
			parameter("limit", 1)
			parameter("list", "most_pop_movies")
		}.body<RandomMovies>().results.first()
	}

	suspend fun getGenres() = runCatching {
		httpClient.get("/titles/utils/genres").body<Genres>()
	}

	suspend fun getMovieLists() = runCatching {
		httpClient.get("/titles/utils/lists").body<MovieTypeList>()
	}

	suspend fun getMovieDataById(id: String): Result<TitleInfo> = runCatching {
		val response = httpClient.get("/titles/$id")

		response.body<TitleByIdResponse>().results
	}
}

@Serializable
data class Genres(
	val results: List<String?>
)

@Serializable
data class MovieTypeList(
	val entries: Int,
	val results: List<String>
)

@Serializable
data class RandomMovies(
	val entries: Int,
	val results: List<TitleInfo>
)

@Serializable
data class TitlesPage(
	val page: Int,
	val next: String?,
	val entries: Int,
	val results: List<TitleInfo>
)

@Serializable
data class TitleByIdResponse(
	val results: TitleInfo
)

@Serializable
data class TitleInfo(
	val _id: String,
	val id: String,
	val primaryImage: ImageData?,
	val titleType: TitleType,
	val titleText: TitleText,
	val originalTitleText: TitleText,
	val releaseYear: ReleaseYear?,
	val releaseDate: ReleaseDate?
)

@Serializable
data class ImageData(
	val id: String,
	val width: Int,
	val height: Int,
	val url: String,
	val caption: ImageCaption,
	val __typename: String,
)

@Serializable
data class ImageCaption(
	val plainText: String,
	val __typename: String,
)

@Serializable
data class TitleType(
	val text: String,
	val id: String,
	val isSeries: Boolean,
	val isEpisode: Boolean,
	val __typename: String,
)

@Serializable
data class TitleText(
	val text: String,
	val __typename: String,
)

@Serializable
data class ReleaseYear(
	val year: Int,
	val endYear: String?,
	val __typename: String,
)

@Serializable
data class ReleaseDate(
	val day: Int?,
	val month: Int?,
	val year: Int,
	val __typename: String,
)
