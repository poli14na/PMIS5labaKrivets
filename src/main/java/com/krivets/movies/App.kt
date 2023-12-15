package com.krivets.movies

import android.app.Application
import android.util.Log
import com.krivets.movies.data.FavoritesDatabase
import com.krivets.movies.data.FavoritesRepository
import com.krivets.movies.data.MovieNotesDatabase
import com.krivets.movies.data.FavoritesRepositoryImpl
import com.krivets.movies.data.MovieNotesRepository
import com.krivets.movies.data.MovieNotesRepositoryImpl
import com.krivets.movies.data.MovieRepository
import com.krivets.movies.data.SearchHistoryDatabase
import com.krivets.movies.data.SearchRepository
import com.krivets.movies.ui.FavoritesViewModel
import com.krivets.movies.ui.MovieViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App : Application() {
	override fun onCreate() {
		super.onCreate()

		startKoin {
			androidContext(this@App)
			modules(
				networkModule,
				repositoriesModule,
				movieViewModelModule,
				favoritesViewModel
			)
		}
	}
}

val networkModule = module {
	single { NetworkClient() }
}

val repositoriesModule = module {
	single { MovieRepository(networkClient = get()) }
	single<MovieNotesRepository> {
		MovieNotesRepositoryImpl(
			MovieNotesDatabase.getDatabase(context = get()).dao()
		)
	}
	single<FavoritesRepository> {
		FavoritesRepositoryImpl(
			FavoritesDatabase.getDatabase(context = get()).dao()
		)
	}
	single {
		SearchRepository(
			networkClient = get(),
			searchHistoryDao = SearchHistoryDatabase.getDatabase(context = get()).dao()
		)
	}
}

val movieViewModelModule = module {
	viewModel { parameter -> MovieViewModel(parameter.get(), get(), get(), get()) }
}

val favoritesViewModel = module {
	viewModel { parameter -> FavoritesViewModel(parameter.get(), get()) }
}

class NetworkClient {
	private val baseUrl = "https://moviesdatabase.p.rapidapi.com"
	private val apiKeyHeader = "X-RapidAPI-Key"
	private val apiKey = "2a10997096mshcde78da4d258acbp1f7b76jsn4fd8e6282260"
	private val hostHeader = "X-RapidAPI-Host"

	val httpClient = HttpClient(CIO) {
		install(ContentNegotiation) {
			json(
				json = Json {
					ignoreUnknownKeys = true
					coerceInputValues = true
				}
			)
		}

		defaultRequest {
			url(baseUrl)
			this.headers {
				header(apiKeyHeader, apiKey)
				header(hostHeader, host)
			}
		}

		install(Logging) {
			level = LogLevel.BODY
			logger = object : Logger {
				override fun log(message: String) {
					Log.d("HTTP Client", message)
				}
			}
		}
	}
}