package com.b1gbr0ther.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.*

object LocaleHelper {
    
    private const val SELECTED_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"
    
    // Define supported languages - easy to extend
    enum class SupportedLanguage(val code: String, val displayName: String, val nativeDisplayName: String) {
        ENGLISH("en", "English", "English"),
        DUTCH("nl", "Dutch", "Nederlands"),
        FRENCH("fr", "French", "Français"),
        // Easy to add more languages here:
        // GERMAN("de", "German", "Deutsch"),
        // SPANISH("es", "Spanish", "Español")
    }
    
    fun getSupportedLanguages(): List<SupportedLanguage> {
        return SupportedLanguage.values().toList()
    }
    
    fun getCurrentLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        return sharedPreferences.getString(SELECTED_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    fun getCurrentLanguageDisplayName(context: Context): String {
        val currentLang = getCurrentLanguage(context)
        return getSupportedLanguages().find { it.code == currentLang }?.nativeDisplayName ?: "English"
    }
    
    fun setLocale(context: Context, languageCode: String): Context {
        // Validate that the language is supported
        val isSupported = getSupportedLanguages().any { it.code == languageCode }
        val finalLanguageCode = if (isSupported) languageCode else DEFAULT_LANGUAGE
        
        val sharedPreferences = context.getSharedPreferences("B1gBr0therSettings", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(SELECTED_LANGUAGE, finalLanguageCode)
        editor.apply()
        
        return updateResources(context, finalLanguageCode)
    }
    
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    fun onAttach(context: Context): Context {
        val language = getCurrentLanguage(context)
        return updateResources(context, language)
    }
    
    fun isDutch(context: Context): Boolean {
        return getCurrentLanguage(context) == SupportedLanguage.DUTCH.code
    }
    
    fun isEnglish(context: Context): Boolean {
        return getCurrentLanguage(context) == SupportedLanguage.ENGLISH.code
    }
    
    // Helper method to get language by code
    fun getLanguageByCode(code: String): SupportedLanguage? {
        return getSupportedLanguages().find { it.code == code }
    }
    
    // Helper method to get next language (for cycling through languages)
    fun getNextLanguage(context: Context): SupportedLanguage {
        val currentLang = getCurrentLanguage(context)
        val supportedLangs = getSupportedLanguages()
        val currentIndex = supportedLangs.indexOfFirst { it.code == currentLang }
        val nextIndex = (currentIndex + 1) % supportedLangs.size
        return supportedLangs[nextIndex]
    }
} 