package com.limelight.ui.components;

/**
 * Shared list/grid cell contract for reusable adapters.
 */
public final class UiCellModel {
    private final String title;
    private final String subtitle;
    private final Runnable clickAction;

    public UiCellModel(String title, String subtitle, Runnable clickAction) {
        this.title = title;
        this.subtitle = subtitle;
        this.clickAction = clickAction;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Runnable getClickAction() {
        return clickAction;
    }
}
