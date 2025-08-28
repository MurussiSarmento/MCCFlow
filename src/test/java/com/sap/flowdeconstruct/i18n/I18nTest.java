package com.sap.flowdeconstruct.i18n;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

/**
 * Test class for I18n language switching functionality
 */
public class I18nTest {
    
    @Test
    public void testLocaleChangeListener() {
        // Test that locale change listener is called
        final boolean[] listenerCalled = {false};
        final Locale[] receivedLocale = {null};
        
        I18n.LocaleChangeListener testListener = new I18n.LocaleChangeListener() {
            @Override
            public void onLocaleChanged(Locale newLocale) {
                listenerCalled[0] = true;
                receivedLocale[0] = newLocale;
            }
        };
        
        I18n.addChangeListener(testListener);
        
        // Change locale
        Locale spanishLocale = new Locale("es");
        I18n.setLocale(spanishLocale);
        
        // Verify listener was called
        assertTrue(listenerCalled[0], "Locale change listener should be called");
        assertEquals(spanishLocale, receivedLocale[0], "Correct locale should be passed to listener");
        
        // Clean up
        I18n.removeChangeListener(testListener);
    }
    
    @Test
    public void testLocaleSwithching() {
        // Test that locale changes
        Locale originalLocale = I18n.getLocale();
        
        // Set to Spanish
        Locale spanishLocale = new Locale("es");
        I18n.setLocale(spanishLocale);
        assertEquals(spanishLocale, I18n.getLocale(), "Locale should change to Spanish");
        
        // Set to Portuguese
        Locale portugueseLocale = new Locale("pt", "BR");
        I18n.setLocale(portugueseLocale);
        assertEquals(portugueseLocale, I18n.getLocale(), "Locale should change to Portuguese");
        
        // Set to English
        Locale englishLocale = new Locale("en");
        I18n.setLocale(englishLocale);
        assertEquals(englishLocale, I18n.getLocale(), "Locale should change to English");
        
        // Restore original locale
        I18n.setLocale(originalLocale);
    }
    
    @Test
    public void testTranslationKeysExist() {
        // Test that essential keys exist in all locales
        String[] testKeys = {
            "app.name",
            "menu.file", 
            "settings.title",
            "settings.language",
            "dialog.ok",
            "dialog.cancel"
        };
        
        Locale[] testLocales = {
            new Locale("en"),
            new Locale("es"), 
            new Locale("pt", "BR")
        };
        
        for (Locale locale : testLocales) {
            I18n.setLocale(locale);
            for (String key : testKeys) {
                String translation = I18n.t(key);
                assertNotNull(translation, "Translation for key '" + key + "' should not be null for locale " + locale);
                assertFalse(translation.isEmpty(), "Translation for key '" + key + "' should not be empty for locale " + locale);
                // Translation should not be the same as the key (unless fallback)
                if (!translation.equals(key)) {
                    assertTrue(translation.length() > 0, "Translation should have content");
                }
            }
        }
    }
}