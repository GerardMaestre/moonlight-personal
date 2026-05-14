package com.limelight.ui.components;

/**
 * Shared empty state content to keep visuals and copy consistent.
 */
public final class EmptyStateModel {
    private final String title;
    private final String description;
    private final ActionButtonModel actionButton;

    public EmptyStateModel(String title, String description, ActionButtonModel actionButton) {
        this.title = title;
        this.description = description;
        this.actionButton = actionButton;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ActionButtonModel getActionButton() {
        return actionButton;
    }
}
