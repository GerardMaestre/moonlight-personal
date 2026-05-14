package com.limelight.ui.components;

/**
 * Shared top bar configuration for non-critical UI screens.
 */
public final class TopBarConfig {
    private final String title;
    private final Runnable navigationAction;

    public TopBarConfig(String title, Runnable navigationAction) {
        this.title = title;
        this.navigationAction = navigationAction;
    }

    public String getTitle() {
        return title;
    }

    public Runnable getNavigationAction() {
        return navigationAction;
    }
}
