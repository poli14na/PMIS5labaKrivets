@file:OptIn(ExperimentalMaterial3Api::class)

package com.krivets.movies.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krivets.movies.data.SearchHistoryItem
import com.krivets.movies.data.SearchRepository
import com.krivets.movies.data.TitleInfo
import dev.olshevski.navigation.reimagined.NavController
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.pop
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import java.util.UUID

@Composable
fun SearchScreen(
	navController: NavController<Destinations>,
	vm: SearchViewModel
) {
	val searchQuery by remember { derivedStateOf { vm.searchQuery } }
	val searchQueryIsEmpty by remember { derivedStateOf { searchQuery.isEmpty() } }

	val searchResults by remember { derivedStateOf { vm.searchResults } }
	val searchHistory by vm.successfulSearchQueriesHistory.collectAsState(initial = emptyList())

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(text = "Поиск") },
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
		}
	) { scaffoldPadding ->
		Column(Modifier.padding(scaffoldPadding)) {
			TextField(
				value = searchQuery,
				onValueChange = { vm.updateSearchQuery(it)},
				modifier = Modifier.fillMaxWidth(),
				placeholder = {
					Text("Введите запрос")
				},
				trailingIcon = {
					if (!searchQueryIsEmpty) IconButton(onClick = { vm.clearSearchQueryAndResults() }) {
						Icon(
							Icons.Default.Clear,
							contentDescription = "Очистить поиск"
						)
					}
				}
			)

			LazyColumn(
				verticalArrangement = Arrangement.spacedBy(8.dp)
			) {
				if (searchQueryIsEmpty) {
					items(searchHistory) {
						Row(
							Modifier
								.fillMaxWidth()
								.clickable { vm.updateSearchQuery(it.title) },
							horizontalArrangement = Arrangement.SpaceBetween,
							verticalAlignment = Alignment.CenterVertically
						) {
							Row(verticalAlignment = Alignment.CenterVertically) {
								Icon(Icons.Default.Refresh, contentDescription = null)

								Text(
									it.title,
									style = MaterialTheme.typography.bodyLarge
								)
							}

							IconButton(onClick = { vm.removeASearchHistoryItem(it) }) {
								Icon(
									Icons.Default.Clear,
									contentDescription = "Удалить"
								)
							}
						}
					}
				} else {
					itemsIndexed(searchResults) { index, item ->
						val title = item.titleText.text
						val imageUrl = item.primaryImage?.url

						MovieItem(
							Modifier
								.padding(horizontal = 16.dp)
								.clickable { navController.navigate(Destinations.Movie(item.id)) }
							,
							name = title,
							imageUrl = imageUrl
						)

						LaunchedEffect(Unit) {
							if (index + 1 == searchResults.size) vm.loadMoreMovies()
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
		}
	}

}

class SearchViewModel : ViewModel() {
	val searchRepository = GlobalContext.get().get<SearchRepository>()

	var searchQuery by mutableStateOf("")
	var loadedPage = 0
	val searchResults = mutableStateListOf<TitleInfo>()

	val successfulSearchQueriesHistory = searchRepository.getAllItemsStream()

	fun updateSearchQuery(newText: String) {
		searchQuery = newText
		viewModelScope.launch {
			searchRepository.getSearchPage(searchQuery)?.let {
				if (it.results.isNotEmpty()) {
					searchResults.clear()
					searchResults.addAll(it.results)

					loadedPage = 0
					searchRepository.insertItem(
						SearchHistoryItem(
							id = UUID.randomUUID().toString(),
							title = newText,
							timeOfCreation = System.currentTimeMillis()
						)
					)
				}
			}
		}
	}

	fun clearSearchQueryAndResults() {
		searchQuery = ""
		searchResults.clear()
	}

	suspend fun loadMoreMovies() {
		searchRepository.getSearchPage(searchQuery, page = loadedPage + 1)?.let {
			searchResults.addAll(it.results)
			loadedPage++
		}
	}

	fun removeASearchHistoryItem(item: SearchHistoryItem) {
		viewModelScope.launch {
			searchRepository.deleteItem(item)
		}
	}
}