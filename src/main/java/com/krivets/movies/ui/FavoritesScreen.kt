@file:OptIn(ExperimentalMaterial3Api::class)

package com.krivets.movies.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.krivets.movies.data.Container
import com.krivets.movies.data.Favorite
import com.krivets.movies.data.FavoritesRepository
import com.krivets.movies.data.MVIViewModel
import com.krivets.movies.data.subscribe
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
	navController: NavController<Destinations>,
	vm: Container<FavoritesScreenState, FavoritesScreenIntent, FavoritesScreenEvent>
) {
	val state by vm.subscribe { }

	val favorites by state.favorites.collectAsState(initial = emptyList())
	val favoritesListIsEmpty by remember { derivedStateOf { favorites.isEmpty() } }

	val selectedMovies by remember { derivedStateOf { state.selectedMovies } }
	val isInSelectionMode by remember { derivedStateOf { state.selectedMovies.isNotEmpty() } }

	var displayExitConfirmationDialog by remember { mutableStateOf(false) }

	BackHandler(enabled = isInSelectionMode) {
		displayExitConfirmationDialog = true
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					if (isInSelectionMode)
						Text(text = "Выбрано ${selectedMovies.size} фильмов")
					else
						Text(text = "Любимые")
				},
				navigationIcon = {
					if (navController.backstack.entries.size > 1) {
						IconButton(
							onClick = {
								if (isInSelectionMode)
									displayExitConfirmationDialog = true
								else
									navController.pop()
							}
						) {
							Icon(
								imageVector = Icons.Default.ArrowBack,
								contentDescription = "Назад"
							)
						}
					}
				},
				actions = {
					if (isInSelectionMode) {
						IconButton(
							onClick = {
								vm.intent(FavoritesScreenIntent.RemoveSelectedItems)
							}
						) {
							Icon(
								Icons.Default.Delete,
								contentDescription = "Удалить выбранное"
							)
						}

						IconButton(
							onClick = { vm.intent(FavoritesScreenIntent.CleanListOfSelectedMovies) }
						) {
							Icon(
								Icons.Default.Clear,
								contentDescription = "Очистить выбранное"
							)
						}
					}
				}
			)
		}
	) { scaffoldPadding ->
		if (favoritesListIsEmpty) {
			Box(
				Modifier.fillMaxSize(),
				contentAlignment = Alignment.Center
			) {
				Text(text = "Пусто :( Нужно что-то лайкнуть")
			}
		}

		Column(Modifier.padding(scaffoldPadding)) {
			LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				items(favorites) {
					val isSelected by remember {
						derivedStateOf { selectedMovies.contains(it) }
					}

					val animatedBackground by animateColorAsState(
						if (isSelected)
							MaterialTheme.colorScheme.secondaryContainer
						else
							Color.Transparent,
						label = ""
					)

					MovieItem(
						Modifier
							.padding(horizontal = 16.dp)
							.clip(RoundedCornerShape(8.dp))
							.background(animatedBackground)
							.combinedClickable(
								onLongClick = {
									vm.intent(FavoritesScreenIntent.SwitchSelectionForMovieItem(it))
								},
								onClick = {
									if (isInSelectionMode)
										vm.intent(
											FavoritesScreenIntent.SwitchSelectionForMovieItem(
												it
											)
										)
									else
										navController.navigate(Destinations.Movie(it.movieId))
								}
							),
						name = it.title,
						imageUrl = it.imageUrl
					)
				}
			}
		}
	}

	if (displayExitConfirmationDialog) AlertDialog(
		onDismissRequest = { displayExitConfirmationDialog = false },
		title = { Text(text = "Вы действительно хотите выйти?")},
		text = { Text(text = "Выход из экрана очистит список выбранных фильмов.") },
		confirmButton = {
			Button(
				onClick = {
					vm.intent(FavoritesScreenIntent.CleanListOfSelectedMovies)
					displayExitConfirmationDialog = false
					navController.pop()
				}
			) {
				Text(text = "Да, выйти")
			}
		},
		dismissButton = {
			FilledTonalButton(onClick = { displayExitConfirmationDialog = false }) {
				Text(text = "Нет, остаться")
			}
		}
	)
}

sealed class FavoritesScreenIntent {
	data object LoadFavorites : FavoritesScreenIntent()
	data class SwitchSelectionForMovieItem(val favoriteMovie: Favorite) : FavoritesScreenIntent()
	data object CleanListOfSelectedMovies : FavoritesScreenIntent()
	data object RemoveSelectedItems: FavoritesScreenIntent()
}

sealed class FavoritesScreenEvent

data class FavoritesScreenState(
	val favorites: Flow<List<Favorite>>,
	val selectedMovies: List<Favorite>
) {
	companion object {
		val initial = FavoritesScreenState(emptyFlow(), emptyList())
	}
}

class FavoritesViewModel(
	private val initial: FavoritesScreenState,
	private val favoritesRepository: FavoritesRepository
) : MVIViewModel<FavoritesScreenState, FavoritesScreenIntent, FavoritesScreenEvent>(initial) {
	override suspend fun reduce(intent: FavoritesScreenIntent) {
		when (intent) {
			is FavoritesScreenIntent.LoadFavorites -> state {
				this.copy(favorites = favoritesRepository.getAllItemsStream())
			}

			is FavoritesScreenIntent.SwitchSelectionForMovieItem -> state {
				if (this.selectedMovies.contains(intent.favoriteMovie)) {
					this.copy(selectedMovies = this.selectedMovies - intent.favoriteMovie)
				} else {
					this.copy(selectedMovies = this.selectedMovies + intent.favoriteMovie)
				}
			}

			is FavoritesScreenIntent.CleanListOfSelectedMovies -> state {
				this.copy(selectedMovies = emptyList())
			}

			is FavoritesScreenIntent.RemoveSelectedItems -> state {
				viewModelScope.launch {
					this@state.selectedMovies.forEach {
						favoritesRepository.deleteItem(it)
					}
				}

				this.copy(selectedMovies = emptyList())
			}
		}
	}

	override fun CoroutineScope.onSubscribe() {
		Log.d("Favorites MVI VM", "Subscribed")

		intent(FavoritesScreenIntent.LoadFavorites)
	}

	override fun onCleared() {
		Log.d("favorites MVI VM", "Unsubscribed")
	}
}