package com.limelight.ui.nonstream;

/**
 * Canonical UI state model for non-streaming screens.
 *
 * <p>Use this model in Home, PC List, Settings, and Help screens so they share the
 * same loading/empty/error/content semantics and rendering flow.
 */
public interface UiState<T> {
    final class Loading<T> implements UiState<T> {
        private final String message;

        public Loading() {
            this(null);
        }

        public Loading(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    final class Empty<T> implements UiState<T> {
        private final String title;
        private final String description;

        public Empty(String title, String description) {
            this.title = title;
            this.description = description;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }
    }

    final class Error<T> implements UiState<T> {
        private final String title;
        private final String description;
        private final Runnable retryAction;

        public Error(String title, String description, Runnable retryAction) {
            this.title = title;
            this.description = description;
            this.retryAction = retryAction;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Runnable getRetryAction() {
            return retryAction;
        }
    }

    final class Content<T> implements UiState<T> {
        private final T data;

        public Content(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }
}
