package ui.component.state

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Loaded<out T>(val data: T) : UiState<T>
}
