package com.krivets.movies.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pmorozova.movies.data.MovieRepository
import com.pmorozova.movies.data.TitleInfo
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
	navController: NavController<Destinations>,
	vm: HomeViewModel
) {
	val movieItems by remember { derivedStateOf { vm.movies } }

	val genres by remember { derivedStateOf { vm.genres } }
	val selectedGenre by remember { derivedStateOf { vm.currentSelectedGenre } }
	val noGenreIsSelected by remember { derivedStateOf { selectedGenre == null } }

	val movieLists by remember { derivedStateOf { vm.movieLists } }
	val selectedList by remember { derivedStateOf { vm.currentSelectedList } }
	val noListIsSelected by remember { derivedStateOf { selectedList == null } }

	val pullToRefreshState = rememberPullToRefreshState()
	
	LaunchedEffect(pullToRefreshState.isRefreshing) {
		if (pullToRefreshState.isRefreshing) {
			vm.refreshMovies()
				.onSuccess { pullToRefreshState.endRefresh() }
				.onFailure {
					delay(3000)
					pullToRefreshState.endRefresh()
				}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = "Дом") },
				navigationIcon = {
					if (navController.backstack.entries.size > 1) {
						IconButton(onClick = { navController.pop() }) {
							Icon(
								imageVector = Icons.Default.ArrowBack,
								contentDescription = "Назад"
							)
						}
					}
				}
			)
		},
		modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)
	) { scaffoldPadding ->
		Box(Modifier.padding(scaffoldPadding)) {
			Column {
				Row(
					Modifier.padding(start = 16.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Box {
						var dropDownMenuIsDisplayed by remember { mutableStateOf(false) }
						val chipText = if (noGenreIsSelected) "Жанры" else selectedGenre!!

						FilterChip(
							selected = !noGenreIsSelected,
							onClick = { dropDownMenuIsDisplayed = true },
							label = { Text(text = chipText)},
							trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
						)

						DropdownMenu(
							expanded = dropDownMenuIsDisplayed,
							onDismissRequest = { dropDownMenuIsDisplayed = false }
						) {
							genres.forEach {
								DropdownMenuItem(
									text = { Text(text = it) },
									onClick = {
										vm.changeGenre(it)
										dropDownMenuIsDisplayed = false
									}
								)
							}
						}
					}

					if (!noGenreIsSelected) IconButton(
						onClick = { vm.clearCurrentGenre() },
						modifier = Modifier.size(24.dp)
					) {
						Icon(
							Icons.Default.Clear,
							contentDescription = "Очистить фильтр жанров"
						)
					}

					Spacer(modifier = Modifier.width(8.dp))

					Box {
						var dropDownMenuIsDisplayed by remember { mutableStateOf(false) }
						val chipText = if (noListIsSelected) "Списки" else selectedList!!

						FilterChip(
							selected = !noListIsSelected,
							onClick = { dropDownMenuIsDisplayed = true },
							label = { Text(text = chipText)},
							trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
						)

						DropdownMenu(
							expanded = dropDownMenuIsDisplayed,
							onDismissRequest = { dropDownMenuIsDisplayed = false }
						) {
							movieLists.forEach {
								DropdownMenuItem(
									text = { Text(text = it) },
									onClick = {
										vm.changeMovieList(it)
										dropDownMenuIsDisplayed = false
									}
								)
							}
						}
					}

					if (!noListIsSelected) IconButton(
						onClick = { vm.clearCurrentMovieList() },
						modifier = Modifier.size(24.dp)
					) {
						Icon(
							Icons.Default.Clear,
							contentDescription = "Очистить фильтр жанров"
						)
					}
				}
				LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)){
					itemsIndexed(movieItems, key = { index, item -> item.id }) { index, item ->
						val title = item.titleText.text
						val id = item.id

						MovieItem(
							modifier = Modifier
								.padding(horizontal = 16.dp)
								.clickable { navController.navigate(Destinations.Movie(id)) },
							name = title,
							imageUrl = item.primaryImage?.url
						)

						LaunchedEffect(Unit) {
							if (index + 1 == movieItems.size) vm.loadMoreMovies()
						}
					}

					item {
						Row(
							Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.Center
						) {
							CircularProgressIndicator()
						}
					}
				}
			}

			PullToRefreshContainer(
				state = pullToRefreshState,
				Modifier.align(Alignment.TopCenter)
			)
		}
	}
}

class HomeViewModel : ViewModel() {
	private val movieRepository = GlobalContext.get().get<MovieRepository>()
	private var loadedPage = 0
	val movies = mutableStateListOf<TitleInfo>()

	val genres = mutableStateListOf<String>()
	var currentSelectedGenre: String? by mutableStateOf(null)

	val movieLists = mutableStateListOf<String>()
	var currentSelectedList: String? by mutableStateOf(null)

	init {
		viewModelScope.launch {
			launch { loadMovies() }
			launch { loadGenres() }
			launch { loadMovieLists() }
		}
	}

	suspend fun refreshMovies() = movieRepository.getTitlesPage(
		page = loadedPage + 1,
		genre = currentSelectedGenre,
		movieList = currentSelectedList
	).onSuccess {
		movies.clear()
		movies.addAll(it.results)
		loadedPage = 0

		viewModelScope.launch {
			launch { loadGenres() }
			launch { loadMovieLists() }
		}
	}


	private suspend fun loadMovies() {
		movieRepository.getTitlesPage(
			page = loadedPage + 1,
			genre = currentSelectedGenre,
			movieList = currentSelectedList
		).onSuccess {
			movies.addAll(it!!.results)
			loadedPage++
		}
	}

	private suspend fun loadGenres() = movieRepository.getGenres().onSuccess {
		genres.clear()

		it.results.forEach {
			if (it != null) { genres.add(it) }
		}
	}

	private suspend fun loadMovieLists() = movieRepository.getMovieLists().onSuccess {
		movieLists.clear()
		it.results.forEach {
			movieLists.add(it)
		}
	}

	suspend fun loadMoreMovies() {
		loadMovies()
	}

	fun changeGenre(newGenre: String) {
		resetLoadedMoviesList()
		currentSelectedGenre = newGenre
		viewModelScope.launch { loadMovies() }
	}

	fun changeMovieList(newList: String) {
		resetLoadedMoviesList()
		currentSelectedList = newList
		viewModelScope.launch { loadMovies() }
	}

	fun clearCurrentGenre() {
		resetLoadedMoviesList()
		currentSelectedGenre = null
		viewModelScope.launch { loadMovies() }
	}

	fun clearCurrentMovieList() {
		resetLoadedMoviesList()
		currentSelectedList = null
		viewModelScope.launch { loadMovies() }
	}

	private fun resetLoadedMoviesList() {
		loadedPage = 0
		movies.clear()
	}
}