package com.focusbuddy.utils;

import javafx.scene.Scene;

public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme = Theme.LIGHT;
    
    public enum Theme {
        LIGHT, DARK
    }
    
    private ThemeManager() {}
    
    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    public void applyTheme(Scene scene, Theme theme) {
        scene.getStylesheets().clear();
        
        switch (theme) {
            case LIGHT:
                scene.getStylesheets().add(getClass().getResource("/css/light-theme.css").toExternalForm());
                break;
            case DARK:
                scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                break;
        }
        
        this.currentTheme = theme;
    }
    
    public void toggleTheme(Scene scene) {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(scene, newTheme);
    }
    
    public Theme getCurrentTheme() {
        return currentTheme;
    }
}
