package com.stsmod.relics;

import basemod.BaseMod;
import basemod.ModPanel;
import basemod.interfaces.*;
import basemod.helpers.RelicType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.stsmod.relics.relics.ExampleRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for Custom Relics
 * Adds unique relics to Slay the Spire
 */
@SpireInitializer
public class CustomRelicsMod implements
        PostInitializeSubscriber,
        EditRelicsSubscriber,
        EditStringsSubscriber {

    private static final Logger logger = LogManager.getLogger(CustomRelicsMod.class.getName());

    // Mod ID and configuration
    public static final String MOD_ID = "customrelics";
    public static final String MOD_NAME = "Custom Relics";
    private static final String AUTHOR = "YourName";
    private static final String DESCRIPTION = "Adds unique custom relics";

    // Asset paths
    private static final String BADGE_IMAGE = "images/badge.png";
    private static final String RELIC_IMAGES = "images/relics/";

    // Localization paths
    private static final String RELIC_STRINGS = "localization/eng/RelicStrings.json";

    /**
     * Required by @SpireInitializer
     * Called by ModTheSpire during initialization
     */
    public static void initialize() {
        logger.info("Initializing " + MOD_NAME);
        CustomRelicsMod mod = new CustomRelicsMod();
        logger.info(MOD_NAME + " initialization complete");
    }

    /**
     * Constructor - Subscribe to BaseMod hooks
     */
    public CustomRelicsMod() {
        logger.info("Subscribing to BaseMod hooks");

        // Subscribe to BaseMod events
        BaseMod.subscribe(this);
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
     * Add custom relics to the game
     */
    @Override
    public void receiveEditRelics() {
        logger.info("Adding custom relics");

        // Add each relic to the game
        // BaseMod.addRelic() makes the relic available in runs
        // RelicType.SHARED makes it available for all characters
        BaseMod.addRelic(new ExampleRelic(), RelicType.SHARED);

        // Add more relics here:
        // BaseMod.addRelic(new AnotherRelic(), RelicType.RED);    // Ironclad only
        // BaseMod.addRelic(new RareRelic(), RelicType.GREEN);     // Silent only
        // BaseMod.addRelic(new BossRelic(), RelicType.BLUE);      // Defect only
        // BaseMod.addRelic(new ShopRelic(), RelicType.PURPLE);    // Watcher only

        logger.info("Custom relics added");
    }

    /**
     * Load localization strings
     */
    @Override
    public void receiveEditStrings() {
        logger.info("Loading localization strings");

        // Load relic strings based on game language
        String language = getLangString();
        String localizationPath = getModPath(RELIC_STRINGS.replace("eng", language));

        // Check if localization file exists, fallback to English if not
        if (!Gdx.files.internal(localizationPath).exists()) {
            logger.warn("Localization file not found: " + localizationPath);
            logger.info("Falling back to English localization");
            language = "eng";
            localizationPath = getModPath(RELIC_STRINGS.replace("eng", language));
        }

        // Load relic strings
        BaseMod.loadCustomStringsFile(
                RelicStrings.class,
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
    public static String getModPath(String resource) {
        return MOD_ID + "Resources/" + resource;
    }

    /**
     * Helper to load relic image
     */
    public static Texture getRelicTexture(String fileName) {
        return ImageMaster.loadImage(getModPath(RELIC_IMAGES + fileName));
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
