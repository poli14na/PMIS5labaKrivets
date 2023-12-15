package com.krivets.movies.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.pmorozova.movies.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.compat.ScopeCompat
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.ParametersDefinition
import java.util.UUID

interface Container<S, I, E> {
	fun CoroutineScope.subscribe(onEvent: suspend (E) -> Unit)
	val states: StateFlow<S>
	fun intent(intent: I)
}

abstract class MVIViewModel<S, I, E>(initial: S) : ViewModel(), Container<S, I, E> {
	private val _states = MutableStateFlow(initial)
	private val _events = Channel<E>()

	final override val states = _states.asStateFlow()
	final override fun intent(intent: I) { viewModelScope.launch { reduce(intent) } }
	final override fun CoroutineScope.subscribe(onEvent: suspend (E) -> Unit) {
		onSubscribe()

		launch {
			for (event in _events) {
				onEvent(event)
			}
		}
	}

	protected fun event(event: E) = viewModelScope.launch { _events.send(event) }
	protected fun state(block: S.() -> S) = viewModelScope.launch { _states.value = _states.value.block() }
	protected open fun CoroutineScope.onSubscribe() = Unit
	protected abstract suspend fun reduce(intent: I)
}

@Composable
fun <S, A> Container<S, *, A>.subscribe(onEvent: suspend (A) -> Unit): State<S> { // 1
	val state = states.collectAsStateWithLifecycle()
	val lifecycle = LocalLifecycleOwner.current
	LaunchedEffect(Unit) {
		withContext(Dispatchers.Main.immediate) {
			lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
				subscribe(onEvent)
			}
		}
	}
	return state
}

@Composable
inline fun <reified T, S, I, E> container(
	noinline params: ParametersDefinition? = null,
): Container<S, I, E> where T : Container<S, I, E>, T : ViewModel = ScopeCompat.getViewModel(
	scope = GlobalContext.get().getOrCreateScope<App>(UUID.randomUUID().toString()),
	owner = LocalViewModelStoreOwner.current!!,
	clazz = T::class.java,
	parameters = params
)
