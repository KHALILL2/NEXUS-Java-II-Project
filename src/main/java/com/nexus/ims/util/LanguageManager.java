package com.nexus.ims.util;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;

/**
 * Centralized Bilingual Localization & RTL Engine for NEXUS IMS.
 * Loads UTF-8 resource bundles for English (LTR) and Arabic (RTL), persists user preference,
 * recursively updates Swing component orientation, and isolates LTR numeric/currency substrings.
 */
public final class LanguageManager {

    public enum Language {
        EN(Locale.ENGLISH, "English", false),
        AR(new Locale("ar"), "العربية", true);

        private final Locale locale;
        private final String displayName;
        private final boolean rtl;

        Language(Locale locale, String displayName, boolean rtl) {
            this.locale = locale;
            this.displayName = displayName;
            this.rtl = rtl;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isRtl() {
            return rtl;
        }
    }

    private static final String BUNDLE_BASE = "i18n/messages";
    private static final String PREF_KEY_LANG = "nexus.ims.language";
    private static final Preferences PREFS = Preferences.userNodeForPackage(LanguageManager.class);
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                        ClassLoader loader, boolean reload)
                throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream == null) {
                    return null;
                }
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    return new PropertyResourceBundle(reader);
                }
            }
        }
    };

    private static volatile Language currentLanguage = Language.EN;
    private static volatile ResourceBundle currentBundle;
    private static volatile ResourceBundle fallbackBundle;

    private LanguageManager() {
    }

    /**
     * Initializes the saved language preference and UTF-8 resource bundles at startup.
     */
    public static void init() {
        String saved = PREFS.get(PREF_KEY_LANG, Language.EN.name());
        Language lang;
        try {
            lang = Language.valueOf(saved);
        } catch (IllegalArgumentException ex) {
            lang = Language.EN;
        }
        currentLanguage = lang;
        Locale.setDefault(lang.getLocale());
        loadBundles();
        UIManager.getDefaults().setDefaultLocale(lang.getLocale());
    }

    private static void loadBundles() {
        fallbackBundle = ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH, UTF8_CONTROL);
        currentBundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLanguage.getLocale(), UTF8_CONTROL);
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static boolean isRTL() {
        return currentLanguage.isRtl();
    }

    public static ComponentOrientation getOrientation() {
        return currentLanguage.isRtl()
                ? ComponentOrientation.RIGHT_TO_LEFT
                : ComponentOrientation.LEFT_TO_RIGHT;
    }

    /**
     * Switches language live, persists choice, updates default Locale, and notifies registered listeners.
     */
    public static void setLanguage(Language language) {
        if (language == null || language == currentLanguage) {
            return;
        }
        currentLanguage = language;
        PREFS.put(PREF_KEY_LANG, language.name());
        Locale.setDefault(language.getLocale());
        UIManager.getDefaults().setDefaultLocale(language.getLocale());
        loadBundles();

        Runnable notify = LanguageManager::notifyListeners;
        if (SwingUtilities.isEventDispatchThread()) {
            notify.run();
        } else {
            SwingUtilities.invokeLater(notify);
        }
    }

    public static String get(String key) {
        if (currentBundle == null) {
            init();
        }
        try {
            if (currentBundle.containsKey(key)) {
                return currentBundle.getString(key);
            }
            if (fallbackBundle != null && fallbackBundle.containsKey(key)) {
                return fallbackBundle.getString(key);
            }
        } catch (MissingResourceException ignored) {
            // Fall through to key
        }
        return key;
    }

    public static String format(String key, Object... args) {
        String pattern = get(key);
        try {
            return MessageFormat.format(pattern, args);
        } catch (IllegalArgumentException ex) {
            return pattern;
        }
    }

    /**
     * Wraps SKUs, negative/positive numeric deltas, currency values, and phone numbers
     * with Unicode Left-to-Right Isolate markers (U+2066 ... U+2069) so signs and decimal points
     * stay ordered naturally inside RTL layouts.
     */
    public static String isolateLTR(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (!isRTL()) {
            return value;
        }
        return "\u2066" + value + "\u2069";
    }

    /**
     * Recursively applies the active ComponentOrientation across Swing hierarchies,
     * including JTable headers, JComboBox popups, and nested containers.
     */
    public static void applyOrientation(Component root) {
        if (root == null) {
            return;
        }
        ComponentOrientation orientation = getOrientation();
        applyOrientationRecursive(root, orientation);
        if (root instanceof Window window) {
            SwingUtilities.updateComponentTreeUI(window);
            window.revalidate();
            window.repaint();
        } else {
            root.revalidate();
            root.repaint();
        }
    }

    private static void applyOrientationRecursive(Component comp, ComponentOrientation orientation) {
        comp.setComponentOrientation(orientation);
        if (comp instanceof JTable table) {
            if (table.getTableHeader() != null) {
                table.getTableHeader().setComponentOrientation(orientation);
            }
        } else if (comp instanceof JComboBox<?> combo) {
            Object child = combo.getUI().getAccessibleChild(combo, 0);
            if (child instanceof JPopupMenu popup) {
                applyOrientationRecursive(popup, orientation);
            }
        }
        if (comp instanceof JComponent jComp && jComp.getComponentPopupMenu() != null) {
            applyOrientationRecursive(jComp.getComponentPopupMenu(), orientation);
        }
        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyOrientationRecursive(child, orientation);
            }
        }
    }

    public static void addLanguageListener(Runnable listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeLanguageListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // Guard against disposed windows/components
            }
        }
    }
}
