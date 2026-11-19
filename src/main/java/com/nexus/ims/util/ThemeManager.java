package com.nexus.ims.util;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Centralized 3-Mode Theme Engine for NEXUS IMS.
 * Supports LIGHT, DARK, and SYSTEM (detecting Windows 10/11 AppsUseLightTheme).
 * Persists user preference and smoothly animates look-and-feel transitions via FlatLaf.
 */
public final class ThemeManager {

    public enum ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    public record BadgeColors(Color background, Color foreground, Color border) {
    }

    private static final String PREF_KEY_THEME = "nexus.ims.theme.mode";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private static volatile ThemeMode currentMode = ThemeMode.LIGHT;
    private static volatile boolean darkActive = false;

    private ThemeManager() {
    }

    /**
     * Initializes the saved theme mode and configures FlatLaf UI defaults on startup.
     */
    public static void init() {
        String saved = PREFS.get(PREF_KEY_THEME, ThemeMode.SYSTEM.name());
        ThemeMode mode;
        try {
            mode = ThemeMode.valueOf(saved);
        } catch (IllegalArgumentException ex) {
            mode = ThemeMode.LIGHT;
        }
        currentMode = mode;
        darkActive = resolveDark(mode);
        applyLookAndFeel(darkActive, false);
    }

    public static ThemeMode getMode() {
        return currentMode;
    }

    public static boolean isDark() {
        return darkActive;
    }

    /**
     * Switches the active theme mode, persists it, animates the FlatLaf transition,
     * and notifies all registered UI views/listeners.
     */
    public static void setMode(ThemeMode mode) {
        if (mode == null) {
            return;
        }
        boolean targetDark = resolveDark(mode);
        boolean modeChanged = (currentMode != mode);
        boolean darkChanged = (darkActive != targetDark);

        currentMode = mode;
        PREFS.put(PREF_KEY_THEME, mode.name());

        if (!modeChanged && !darkChanged) {
            return;
        }

        darkActive = targetDark;
        Runnable applyTask = () -> {
            boolean snapshotShown = false;
            try {
                FlatAnimatedLafChange.showSnapshot();
                snapshotShown = true;
            } catch (Exception ignored) {
                // Headless or early startup fallback
            }
            try {
                applyLookAndFeel(darkActive, true);
                notifyListeners();
            } finally {
                if (snapshotShown) {
                    FlatAnimatedLafChange.hideSnapshotWithAnimation();
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            applyTask.run();
        } else {
            SwingUtilities.invokeLater(applyTask);
        }
    }

    public static void addThemeListener(Runnable listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void removeThemeListener(Runnable listener) {
        LISTENERS.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : LISTENERS) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // Keep updating remaining components even if a disposed view threw
            }
        }
    }

    /**
     * Resolves whether dark mode should be active for the given ThemeMode.
     * For SYSTEM on Windows, queries HKCU\...\Personalize AppsUseLightTheme.
     */
    public static boolean resolveDark(ThemeMode mode) {
        if (mode == ThemeMode.DARK) {
            return true;
        }
        if (mode == ThemeMode.LIGHT) {
            return false;
        }
        return isWindowsSystemDark();
    }

    private static boolean isWindowsSystemDark() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return false;
        }
        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"
            );
            pb.redirectErrorStream(true);
            process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("AppsUseLightTheme")) {
                        String trimmed = line.trim().toLowerCase(Locale.ROOT);
                        if (trimmed.endsWith("0x0") || trimmed.endsWith(" 0")) {
                            return true;
                        }
                        if (trimmed.endsWith("0x1") || trimmed.endsWith(" 1")) {
                            return false;
                        }
                    }
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // Fall back safely to Light mode on error or restricted environments
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }

    private static void applyLookAndFeel(boolean dark, boolean updateUi) {
        try {
            if (dark) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }

            Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
            UIManager.put("defaultFont", uiFont);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.arc", 8);
            UIManager.put("Button.arc", 8);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("CheckBox.arc", 4);
            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track", bgCanvas());
            UIManager.put("ScrollBar.thumb", borderStrong());

            UIManager.put("Panel.background", bgCanvas());
            UIManager.put("OptionPane.background", bgSurface());
            UIManager.put("OptionPane.messageForeground", textPrimary());
            UIManager.put("Label.foreground", textPrimary());

            UIManager.put("TextField.background", bgSurface());
            UIManager.put("TextField.foreground", textPrimary());
            UIManager.put("TextField.caretForeground", textPrimary());
            UIManager.put("PasswordField.background", bgSurface());
            UIManager.put("PasswordField.foreground", textPrimary());
            UIManager.put("TextArea.background", bgSurface());
            UIManager.put("TextArea.foreground", textPrimary());
            UIManager.put("ComboBox.background", bgSurface());
            UIManager.put("ComboBox.foreground", textPrimary());
            UIManager.put("ComboBox.selectionBackground", accentPrimary());
            UIManager.put("ComboBox.selectionForeground", Color.WHITE);

            UIManager.put("PopupMenu.background", bgSurface());
            UIManager.put("PopupMenu.foreground", textPrimary());
            UIManager.put("MenuItem.background", bgSurface());
            UIManager.put("MenuItem.foreground", textPrimary());
            UIManager.put("MenuItem.selectionBackground", bgHover());
            UIManager.put("MenuItem.selectionForeground", textPrimary());

            UIManager.put("Table.background", bgSurface());
            UIManager.put("Table.foreground", textPrimary());
            UIManager.put("Table.rowHeight", 38);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.alternateRowColor", bgSurfaceAlt());
            UIManager.put("Table.selectionBackground", bgSelectedRow());
            UIManager.put("Table.selectionForeground", dark ? Color.WHITE : textPrimary());
            UIManager.put("Table.gridColor", borderSubtle());
            UIManager.put("TableHeader.height", 40);
            UIManager.put("TableHeader.background", bgSurfaceAlt());
            UIManager.put("TableHeader.foreground", textSecondary());
            UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));
            UIManager.put("TableHeader.separatorColor", borderSubtle());
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);

            if (updateUi) {
                FlatLaf.updateUI();
            }
        } catch (Exception ignored) {
            // Keep default UI on unexpected LaF errors
        }
    }

    public static Icon modeIcon(ThemeMode mode, int size, Color color) {
        if (mode == ThemeMode.DARK) {
            return IconFactory.moon(size, color);
        }
        if (mode == ThemeMode.SYSTEM) {
            return IconFactory.systemTheme(size, color);
        }
        return IconFactory.sun(size, color);
    }

    // -------------------------------------------------------------------------
    // High-Contrast Semantic Color Tokens
    // -------------------------------------------------------------------------

    public static Color bgCanvas() {
        return darkActive ? new Color(0x0F172A) : new Color(0xF8FAFC);
    }

    public static Color bgSurface() {
        return darkActive ? new Color(0x1E293B) : new Color(0xFFFFFF);
    }

    public static Color bgSurfaceAlt() {
        return darkActive ? new Color(0x273549) : new Color(0xF1F5F9);
    }

    public static Color bgHover() {
        return darkActive ? new Color(0x2B3B54) : new Color(0xEFF6FF);
    }

    public static Color bgSelectedRow() {
        return darkActive ? new Color(0x1E3A8A) : new Color(0xDBEAFE);
    }

    public static Color bgHeader() {
        return darkActive ? new Color(0x1E293B) : new Color(0xFFFFFF);
    }

    public static Color statusBarBg() {
        return darkActive ? new Color(0x0B1120) : new Color(0xFFFFFF);
    }

    public static Color bgSidebar() {
        return darkActive ? new Color(0x090E1A) : new Color(0x0F172A);
    }

    public static Color sidebarHover() {
        return darkActive ? new Color(0x162033) : new Color(0x1E293B);
    }

    public static Color sidebarActive() {
        return darkActive ? new Color(0x2563EB) : new Color(0x1D4ED8);
    }

    public static Color sidebarText() {
        return new Color(0xCBD5E1);
    }

    public static Color sidebarTextMuted() {
        return new Color(0x94A3B8);
    }

    public static Color sidebarTextActive() {
        return Color.WHITE;
    }

    public static Color sidebarDivider() {
        return darkActive ? new Color(0x1E293B) : new Color(0x1E293B);
    }

    public static Color borderSubtle() {
        return darkActive ? new Color(0x334155) : new Color(0xE2E8F0);
    }

    public static Color borderStrong() {
        return darkActive ? new Color(0x475569) : new Color(0xCBD5E1);
    }

    public static Color textPrimary() {
        return darkActive ? new Color(0xF1F5F9) : new Color(0x0F172A);
    }

    public static Color textSecondary() {
        return darkActive ? new Color(0x94A3B8) : new Color(0x64748B);
    }

    public static Color textMuted() {
        return darkActive ? new Color(0x64748B) : new Color(0x94A3B8);
    }

    public static Color accentPrimary() {
        return darkActive ? new Color(0x3B82F6) : new Color(0x2563EB);
    }

    public static Color accentPrimaryHover() {
        return darkActive ? new Color(0x60A5FA) : new Color(0x1D4ED8);
    }

    public static Color accentSecondary() {
        return darkActive ? new Color(0xA78BFA) : new Color(0x7C3AED);
    }

    public static Color accentWarning() {
        return darkActive ? new Color(0xF59E0B) : new Color(0xD97706);
    }

    public static Color accentSuccess() {
        return darkActive ? new Color(0x34D399) : new Color(0x059669);
    }

    public static Color accentDanger() {
        return darkActive ? new Color(0xF87171) : new Color(0xDC2626);
    }

    public static Color accentInfo() {
        return darkActive ? new Color(0x38BDF8) : new Color(0x0284C7);
    }

    public static Color btnSecondaryBg() {
        return darkActive ? new Color(0x273549) : new Color(0xF1F5F9);
    }

    public static Color btnSecondaryFg() {
        return darkActive ? new Color(0xE2E8F0) : new Color(0x1E293B);
    }

    public static Color alertBannerBg() {
        return darkActive ? new Color(0x431407) : new Color(0xFFF7ED);
    }

    public static Color alertBannerBorder() {
        return darkActive ? new Color(0xC2410C) : new Color(0xFDBA74);
    }

    public static Color alertBannerText() {
        return darkActive ? new Color(0xFDBA74) : new Color(0x9A3412);
    }

    public static Color chartTrackBg() {
        return darkActive ? new Color(0x273549) : new Color(0xF1F5F9);
    }

    public static Color chartGridLine() {
        return darkActive ? new Color(0x334155) : new Color(0xE2E8F0);
    }

    // -------------------------------------------------------------------------
    // High-Contrast Badge Color Pairs
    // -------------------------------------------------------------------------

    public static BadgeColors badgeSuccess() {
        return darkActive
                ? new BadgeColors(new Color(0x064E3B), new Color(0x6EE7B7), new Color(0x059669))
                : new BadgeColors(new Color(0xDCFCE7), new Color(0x166534), new Color(0x86EFAC));
    }

    public static BadgeColors badgeDanger() {
        return darkActive
                ? new BadgeColors(new Color(0x7F1D1D), new Color(0xFCA5A5), new Color(0xDC2626))
                : new BadgeColors(new Color(0xFEE2E2), new Color(0x991B1B), new Color(0xFCA5A5));
    }

    public static BadgeColors badgeWarning() {
        return darkActive
                ? new BadgeColors(new Color(0x78350F), new Color(0xFCD34D), new Color(0xD97706))
                : new BadgeColors(new Color(0xFEF3C7), new Color(0x92400E), new Color(0xFCD34D));
    }

    public static BadgeColors badgeInfo() {
        return darkActive
                ? new BadgeColors(new Color(0x1E3A8A), new Color(0x93C5FD), new Color(0x3B82F6))
                : new BadgeColors(new Color(0xDBEAFE), new Color(0x1E40AF), new Color(0x93C5FD));
    }

    public static BadgeColors badgePurple() {
        return darkActive
                ? new BadgeColors(new Color(0x4C1D95), new Color(0xDDD6FE), new Color(0x8B5CF6))
                : new BadgeColors(new Color(0xF3E8FF), new Color(0x6B21A8), new Color(0xD8B4FE));
    }

    public static BadgeColors badgeNeutral() {
        return darkActive
                ? new BadgeColors(new Color(0x334155), new Color(0xCBD5E1), new Color(0x475569))
                : new BadgeColors(new Color(0xF1F5F9), new Color(0x475569), new Color(0xCBD5E1));
    }

    // -------------------------------------------------------------------------
    // Geometry & Animation Helpers
    // -------------------------------------------------------------------------

    /**
     * Smoothly interpolates RGBA channels between c1 (t=0) and c2 (t=1).
     */
    public static Color lerpColor(Color c1, Color c2, float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        int r = Math.round(c1.getRed() + (c2.getRed() - c1.getRed()) * clamped);
        int g = Math.round(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * clamped);
        int b = Math.round(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * clamped);
        int a = Math.round(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }

    /**
     * Creates an EmptyBorder that mirrors leading/trailing insets cleanly in RTL mode.
     */
    public static Border createDirectionalBorder(int top, int leading, int bottom, int trailing, boolean rtl) {
        return rtl
                ? BorderFactory.createEmptyBorder(top, trailing, bottom, leading)
                : BorderFactory.createEmptyBorder(top, leading, bottom, trailing);
    }

    /**
     * Creates a MatteBorder that mirrors leading/trailing strokes cleanly in RTL mode.
     */
    public static Border createDirectionalMatteBorder(
            int top, int leading, int bottom, int trailing, Color color, boolean rtl) {
        return rtl
                ? BorderFactory.createMatteBorder(top, trailing, bottom, leading, color)
                : BorderFactory.createMatteBorder(top, leading, bottom, trailing, color);
    }
}
