package com.sap.flowdeconstruct.i18n;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.prefs.Preferences;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class I18n {
    private static final String BUNDLE_BASE = "messages";
    private static final String PREF_NODE = "com.sap.flowdeconstruct.i18n";
    private static final String PREF_LANG = "lang";
    private static final String PREF_COUNTRY = "country";

    // Custom Control to read .properties as UTF-8 (Java 8 compatible)
    private static final ResourceBundle.Control UTF8_CONTROL = new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                                        boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    bundle = new PropertyResourceBundle(reader);
                }
            }
            return bundle;
        }
    };

    private static volatile Locale currentLocale = Locale.getDefault();
    private static volatile ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale, UTF8_CONTROL);
    private static final List<LocaleChangeListener> listeners = new CopyOnWriteArrayList<>();

    private I18n() {}

    public static void initFromPreferences() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String lang = prefs.get(PREF_LANG, null);
        String country = prefs.get(PREF_COUNTRY, null);
        if (lang != null && !lang.isEmpty()) {
            Locale l = (country != null && !country.isEmpty()) ? new Locale(lang, country) : new Locale(lang);
            setLocale(l);
        } else {
            // Ensure bundle aligns with default locale
            setLocale(Locale.getDefault());
        }
    }

    public static void setLocale(Locale locale) {
        if (locale == null) return;
        currentLocale = locale;
        bundle = ResourceBundle.getBundle(BUNDLE_BASE, currentLocale, UTF8_CONTROL);
        // Persist
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        prefs.put(PREF_LANG, locale.getLanguage());
        prefs.put(PREF_COUNTRY, locale.getCountry());
        // Notify listeners
        for (LocaleChangeListener l : listeners) {
            try { l.onLocaleChanged(currentLocale); } catch (Exception ignored) {}
        }
    }

    public static Locale getLocale() {
        return currentLocale;
    }

    public static String t(String key, Object... args) {
        try {
            String pattern = bundle.getString(key);
            if (args == null || args.length == 0) return pattern;
            return new MessageFormat(pattern, currentLocale).format(args);
        } catch (MissingResourceException e) {
            // Fallback to key itself
            if (args == null || args.length == 0) return key;
            return key + " " + Arrays.toString(args);
        }
    }

    public static void addChangeListener(LocaleChangeListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public static void removeChangeListener(LocaleChangeListener listener) {
        if (listener != null) listeners.remove(listener);
    }

    public interface LocaleChangeListener {
        void onLocaleChanged(Locale newLocale);
    }
}