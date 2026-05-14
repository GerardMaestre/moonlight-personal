package com.limelight.ui.nonstream;

/**
 * Reusable renderer contract for mapping a {@link UiState} to concrete views.
 */
public interface UiStateRenderer<T> {
    void showLoading(UiState.Loading<T> state);

    void showEmpty(UiState.Empty<T> state);

    void showError(UiState.Error<T> state);

    void showContent(UiState.Content<T> state);

    default void render(UiState<T> state) {
        if (state instanceof UiState.Loading) {
            showLoading((UiState.Loading<T>) state);
        } else if (state instanceof UiState.Empty) {
            showEmpty((UiState.Empty<T>) state);
        } else if (state instanceof UiState.Error) {
            showError((UiState.Error<T>) state);
        } else if (state instanceof UiState.Content) {
            showContent((UiState.Content<T>) state);
        } else {
            throw new IllegalArgumentException("Unsupported UI state: " + state);
        }
    }
}
