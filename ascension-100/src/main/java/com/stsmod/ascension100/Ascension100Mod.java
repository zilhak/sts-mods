package com.stsmod.ascension100;

import basemod.BaseMod;
import basemod.ModPanel;
import basemod.interfaces.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

/**
 * Main mod class for Ascension 100
 * Extends the base game's Ascension mode from 20 to 100 levels
 */
@SpireInitializer
public class Ascension100Mod implements
        PostInitializeSubscriber,
        EditStringsSubscriber {

    private static final Logger logger = LogManager.getLogger(Ascension100Mod.class.getName());

    // Mod ID and configuration
    public static final String MOD_ID = "ascension100";
    public static final String MOD_NAME = "Ascension 100";
    private static final String AUTHOR = "YourName";
    private static final String DESCRIPTION = "Extends Ascension mode to level 100";

    // Maximum ascension level
    public static final int MAX_ASCENSION = 100;

    // Badge image path
    private static final String BADGE_IMAGE = "images/badge.png";

    // Localization paths
    private static final String UI_STRINGS = "localization/eng/UIStrings.json";

    /**
     * Required by @SpireInitializer
     * Called by ModTheSpire during initialization
     */
    public static void initialize() {
        logger.info("Initializing " + MOD_NAME);
        Ascension100Mod mod = new Ascension100Mod();
        logger.info(MOD_NAME + " initialization complete");
    }

    /**
     * Constructor - Subscribe to BaseMod hooks
     */
    public Ascension100Mod() {
        logger.info("Subscribing to BaseMod hooks");

        // Subscribe to BaseMod events
        BaseMod.subscribe(this);

        // TODO: Implement ascension level extension
        // Need to patch game code to extend max ascension from 20 to 100
        // Requires finding the actual methods/fields that control ascension limits
        logger.info("Ascension 100 mod loaded (functionality not yet implemented)");
    }

    /**
     * Called after BaseMod finishes initialization
     */
    @Override
    public void receivePostInitialize() {
        logger.info("Setting up mod panel");

        // Create mod settings panel
        ModPanel settingsPanel = new ModPanel();

        // Try to load badge image
        Texture badgeTexture = null;
        try {
            String badgePath = getModPath(BADGE_IMAGE);
            if (Gdx.files.internal(badgePath).exists()) {
                badgeTexture = ImageMaster.loadImage(badgePath);
                logger.info("Badge image loaded successfully");
            } else {
                logger.warn("Badge image not found at: " + badgePath + " - skipping badge registration");
            }
        } catch (Exception e) {
            logger.error("Failed to load badge image - skipping badge registration", e);
        }

        // Only register badge if image was loaded successfully
        if (badgeTexture != null) {
            BaseMod.registerModBadge(
                    badgeTexture,
                    MOD_NAME,
                    AUTHOR,
                    DESCRIPTION,
                    settingsPanel
            );
            logger.info("Mod badge registered");
        } else {
            logger.info("Mod loaded without badge image");
        }

        logger.info("Mod panel setup complete");
    }

    /**
     * Load localization strings
     */
    @Override
    public void receiveEditStrings() {
        logger.info("Loading localization strings");

        // Load UI strings based on game language
        String language = getLangString();
        String localizationPath = getModPath(UI_STRINGS.replace("eng", language));

        // Check if localization file exists, fallback to English if not
        if (!Gdx.files.internal(localizationPath).exists()) {
            logger.warn("Localization file not found: " + localizationPath);
            logger.info("Falling back to English localization");
            language = "eng";
            localizationPath = getModPath(UI_STRINGS.replace("eng", language));
        }

        // Load UI strings
        BaseMod.loadCustomStringsFile(
                UIStrings.class,
                localizationPath
        );

        logger.info("Localization strings loaded for language: " + language);
    }

    /**
     * Get language string for current game settings
     */
    private static String getLangString() {
        // Use string comparison to avoid enum compatibility issues
        Settings.GameLanguage lang = Settings.language;

        if (lang == Settings.GameLanguage.KOR) return "kor";
        if (lang == Settings.GameLanguage.ZHS) return "zhs";
        if (lang == Settings.GameLanguage.ZHT) return "zht";
        if (lang == Settings.GameLanguage.JPN) return "jpn";
        if (lang == Settings.GameLanguage.FRA) return "fra";
        if (lang == Settings.GameLanguage.DEU) return "deu";
        if (lang == Settings.GameLanguage.ITA) return "ita";
        if (lang == Settings.GameLanguage.PTB) return "ptb";
        if (lang == Settings.GameLanguage.RUS) return "rus";
        if (lang == Settings.GameLanguage.POL) return "pol";

        // Try to handle Spanish variants safely
        String langName = lang.name();
        if (langName.contains("ESP") || langName.contains("SPA")) {
            return "esp";
        }

        // Default to English for unsupported languages
        return "eng";
    }

    /**
     * Helper to get full path for mod resources
     */
    private static String getModPath(String resource) {
        return MOD_ID + "Resources/" + resource;
    }

    /**
     * Make ID with mod prefix
     */
    public static String makeID(String id) {
        return MOD_ID + ":" + id;
    }

    /**
     * Log info message
     */
    public static void log(String message) {
        logger.info(message);
    }

    /**
     * Log error message
     */
    public static void error(String message) {
        logger.error(message);
    }
}
