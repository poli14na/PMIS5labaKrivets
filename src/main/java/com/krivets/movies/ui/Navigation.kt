package com.krivets.movies.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krivets.movies.data.container
import dev.olshevski.navigation.reimagined.NavBackHandler
import dev.olshevski.navigation.reimagined.NavHost
import dev.olshevski.navigation.reimagined.navigate
import dev.olshevski.navigation.reimagined.rememberNavController
import kotlinx.parcelize.Parcelize
import org.koin.core.parameter.parametersOf

@Composable
fun Navigation() {
	val navController = rememberNavController<Destinations>(
		startDestination = Destinations.Home
	)
	val currentDestination by remember {
		derivedStateOf {
			navController.backstack.entries.last().destination
		}
	}

	// state hoisting?
	val homeScreenVm = viewModel<HomeViewModel>()
	val searchScreenVm = viewModel<SearchViewModel>()
	val favoritesViewModel = container<FavoritesViewModel, _, _, _> { parametersOf(FavoritesScreenState.initial) }

	NavBackHandler(navController)

	Scaffold(
		bottomBar = {
			NavigationBar {
				NavigationBarItem(
					selected = currentDestination == Destinations.Home,
					onClick = { navController.navigate(Destinations.Home)},
					icon = {
						Icon(
							imageVector = Icons.Default.Home,
							contentDescription = "Домашняя страница"
						)
					}
				)

				NavigationBarItem(
					selected = currentDestination == Destinations.Search,
					onClick = { navController.navigate(Destinations.Search) },
					icon = {
						Icon(
							imageVector = Icons.Default.Search,
							contentDescription = "Поиск"
						)
					}
				)

				NavigationBarItem(
					selected = currentDestination == Destinations.Favorites,
					onClick = {  navController.navigate(Destinations.Favorites) },
					icon = {
						Icon(
							imageVector = Icons.Default.Favorite,
							contentDescription = "Любимые"
						)
					}
				)
			}
		},
	) { paddingValues ->
		NavHost(
			navController,
			modifier = Modifier.padding(paddingValues)
		) {destination ->
			when (destination) {
				is Destinations.Home -> HomeScreen(navController, homeScreenVm)
				is Destinations.Search -> SearchScreen(navController, searchScreenVm)
				is Destinations.Favorites -> FavoritesScreen(navController, favoritesViewModel)
				is Destinations.Movie -> MovieScreen(id = destination.id, navController)
			}
		}
	}
}

sealed class Destinations(val name: String) : Parcelable {
	@Parcelize
	data object Home : Destinations("Домашняя страница")

	@Parcelize
	data object Search : Destinations("Поиск")

	@Parcelize
	data object Favorites : Destinations("Любимые")

	@Parcelize
	data class Movie(val id: String) : Destinations("")
}