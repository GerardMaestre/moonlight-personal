package com.limelight.ui.components;

/**
 * Shared model for primary/secondary action buttons.
 */
public final class ActionButtonModel {
    private final String label;
    private final Runnable action;
    private final boolean primary;

    public ActionButtonModel(String label, Runnable action, boolean primary) {
        this.label = label;
        this.action = action;
        this.primary = primary;
    }

    public String getLabel() {
        return label;
    }

    public Runnable getAction() {
        return action;
    }

    public boolean isPrimary() {
        return primary;
    }
}
