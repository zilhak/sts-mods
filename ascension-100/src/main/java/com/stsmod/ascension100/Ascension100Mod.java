package com.stsmod.ascension100;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModButton;
import basemod.ModPanel;
import basemod.interfaces.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

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

    // Track current ascension level across patches
    public static int currentAscensionLevel = 0;

    // Config for settings
    public static SpireConfig config;
    public static SpireConfig progressConfig;  // Separate config for actual clear progress

    // UI state for ascension level editor
    private static ModLabel currentLevelLabel;
    private static ModLabel characterNameLabel;
    private static int editingLevel = 1;
    private static int selectedCharacterIndex = 0;
    private static com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass[] CHARACTER_CLASSES;
    private static String[] CHARACTER_NAMES;

    // Badge image path
    private static final String BADGE_IMAGE = "images/badge.png";

    // Localization paths
    private static final String UI_STRINGS = "localization/eng/UIStrings.json";
    private static final String POWER_STRINGS = "localization/eng/PowerStrings.json";
    private static final String CARD_STRINGS = "localization/eng/CardStrings.json";

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

        // Initialize config
        try {
            config = new SpireConfig(MOD_ID, "config");

            // Initialize progress tracking config
            progressConfig = new SpireConfig(MOD_ID, "progress");
            logger.info("Config loaded");

        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }

        logger.info("Ascension 100 mod loaded - extends ascension to level 100");
    }

    /**
     * Called after BaseMod finishes initialization
     */
    @Override
    public void receivePostInitialize() {
        logger.info("Setting up mod panel");

        // Create mod settings panel
        ModPanel settingsPanel = new ModPanel();

        // ========================================
        // Initialize Character List (including mod characters)
        // ========================================
        CHARACTER_CLASSES = com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.values();
        CHARACTER_NAMES = new String[CHARACTER_CLASSES.length];

        for (int i = 0; i < CHARACTER_CLASSES.length; i++) {
            try {
                com.megacrit.cardcrawl.characters.AbstractPlayer player =
                    com.megacrit.cardcrawl.core.CardCrawlGame.characterManager.getCharacter(CHARACTER_CLASSES[i]);
                if (player != null) {
                    CHARACTER_NAMES[i] = player.getLocalizedCharacterName();
                } else {
                    CHARACTER_NAMES[i] = CHARACTER_CLASSES[i].name();
                }
            } catch (Exception e) {
                CHARACTER_NAMES[i] = CHARACTER_CLASSES[i].name();
                logger.warn("Could not get localized name for " + CHARACTER_CLASSES[i].name());
            }
        }
        logger.info("Loaded " + CHARACTER_CLASSES.length + " characters (including mods)");

        // ========================================
        // Ascension Level Editor
        // ========================================

        // Section title
        ModLabel editorTitle = new ModLabel(
                "Manual Ascension Level Editor:",
                350.0f, 680.0f,
                Settings.GOLD_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(editorTitle);

        // Initialize with first character's level
        editingLevel = getCurrentCharacterLevel();

        // Layout positions
        float baseX = 350.0f;
        float charY = 570.0f;
        float levelY = 490.0f;
        float labelMarginY = 55.0f;
        float labelMarginX = 40.0f;

        // Character selector row: < Character Name >
        ModButton prevCharButton = new ModButton(
                baseX, charY,
                settingsPanel,
                (button) -> {
                    selectedCharacterIndex = (selectedCharacterIndex - 1 + CHARACTER_CLASSES.length) % CHARACTER_CLASSES.length;
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(prevCharButton);

        ModLabel prevCharLabel = new ModLabel(
                "<",
                baseX + labelMarginX + 10.0f, charY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(prevCharLabel);

        characterNameLabel = new ModLabel(
                CHARACTER_NAMES[selectedCharacterIndex],
                baseX + labelMarginX + 80.0f, charY + labelMarginY,
                Settings.GOLD_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(characterNameLabel);

        ModButton nextCharButton = new ModButton(
                baseX + 400.0f, charY,
                settingsPanel,
                (button) -> {
                    selectedCharacterIndex = (selectedCharacterIndex + 1) % CHARACTER_CLASSES.length;
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(nextCharButton);

        ModLabel nextCharLabel = new ModLabel(
                ">",
                baseX + labelMarginX + 410.0f, charY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(nextCharLabel);

        // Level adjustment row: << < [Level] > >>
        ModButton decreaseLargeButton = new ModButton(
                baseX, levelY,
                settingsPanel,
                (button) -> {
                    editingLevel = Math.max(0, editingLevel - 10);
                    applyToCurrentCharacter();
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(decreaseLargeButton);

        ModLabel decreaseLargeLabel = new ModLabel(
                "<<",
                baseX + labelMarginX, levelY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(decreaseLargeLabel);

        ModButton decreaseButton = new ModButton(
                baseX + 70.0f, levelY,
                settingsPanel,
                (button) -> {
                    editingLevel = Math.max(0, editingLevel - 1);
                    applyToCurrentCharacter();
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(decreaseButton);

        ModLabel decreaseLabel = new ModLabel(
                "<",
                baseX + labelMarginX + 80.0f, levelY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(decreaseLabel);

        currentLevelLabel = new ModLabel(
                "" + editingLevel,
                baseX + labelMarginX + 140.0f, levelY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(currentLevelLabel);

        ModButton increaseButton = new ModButton(
                baseX + 220.0f, levelY,
                settingsPanel,
                (button) -> {
                    editingLevel = Math.min(MAX_ASCENSION, editingLevel + 1);
                    applyToCurrentCharacter();
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(increaseButton);

        ModLabel increaseLabel = new ModLabel(
                ">",
                baseX + labelMarginX + 230.0f, levelY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(increaseLabel);

        ModButton increaseLargeButton = new ModButton(
                baseX + 290.0f, levelY,
                settingsPanel,
                (button) -> {
                    editingLevel = Math.min(MAX_ASCENSION, editingLevel + 10);
                    applyToCurrentCharacter();
                    updateCharacterUI();
                }
        );
        settingsPanel.addUIElement(increaseLargeButton);

        ModLabel increaseLargeLabel = new ModLabel(
                ">>",
                baseX + labelMarginX + 290.0f, levelY + labelMarginY,
                Settings.CREAM_COLOR,
                settingsPanel,
                (label) -> {}
        );
        settingsPanel.addUIElement(increaseLargeLabel);

        // Try to load badge image
        Texture badgeTexture = null;
        try {
            String badgePath = getModPath(BADGE_IMAGE);
            if (Gdx.files.internal(badgePath).exists()) {
                badgeTexture = ImageMaster.loadImage(badgePath);
                logger.info("Badge image loaded successfully");
            } else {
                logger.warn("Badge image not found at: " + badgePath + " - creating placeholder");
                // Create a simple 1x1 white texture as placeholder
                com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
                pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
                pixmap.fill();
                badgeTexture = new Texture(pixmap);
                pixmap.dispose();
            }
        } catch (Exception e) {
            logger.error("Failed to load badge image, creating fallback", e);
            // Create fallback texture
            com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
            pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
            pixmap.fill();
            badgeTexture = new Texture(pixmap);
            pixmap.dispose();
        }

        // Always register badge (now with guaranteed non-null texture)
        BaseMod.registerModBadge(
                badgeTexture,
                MOD_NAME,
                AUTHOR,
                DESCRIPTION,
                settingsPanel
        );
        logger.info("Mod badge registered with settings panel");

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

        // Load Power strings
        String powerStringsPath = getModPath(POWER_STRINGS.replace("eng", language));
        if (!Gdx.files.internal(powerStringsPath).exists()) {
            logger.warn("Power strings file not found: " + powerStringsPath);
            logger.info("Falling back to English power strings");
            powerStringsPath = getModPath(POWER_STRINGS);
        }

        BaseMod.loadCustomStringsFile(
                PowerStrings.class,
                powerStringsPath
        );

        // Load Card strings
        String cardStringsPath = getModPath(CARD_STRINGS.replace("eng", language));
        if (!Gdx.files.internal(cardStringsPath).exists()) {
            logger.warn("Card strings file not found: " + cardStringsPath);
            logger.info("Falling back to English card strings");
            cardStringsPath = getModPath(CARD_STRINGS);
        }

        BaseMod.loadCustomStringsFile(
                CardStrings.class,
                cardStringsPath
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

    /**
     * Get current ascension level for selected character
     */
    private static int getCurrentCharacterLevel() {
        try {
            com.megacrit.cardcrawl.characters.AbstractPlayer player =
                com.megacrit.cardcrawl.core.CardCrawlGame.characterManager.getCharacter(
                    CHARACTER_CLASSES[selectedCharacterIndex]
                );
            if (player != null && player.getPrefs() != null) {
                return player.getPrefs().getInteger("ASCENSION_LEVEL", 1);
            }
        } catch (Exception e) {
            logger.error("Failed to get character level", e);
        }
        return 1;
    }

    /**
     * Update UI to show selected character's current level
     */
    private static void updateCharacterUI() {
        editingLevel = getCurrentCharacterLevel();

        if (characterNameLabel != null) {
            characterNameLabel.text = CHARACTER_NAMES[selectedCharacterIndex];
        }

        if (currentLevelLabel != null) {
            currentLevelLabel.text = "" + editingLevel;
        }

        logger.info("UI updated for " + CHARACTER_NAMES[selectedCharacterIndex] + ": Level " + editingLevel);
    }

    /**
     * Apply current editing level to selected character immediately
     */
    private static void applyToCurrentCharacter() {
        try {
            com.megacrit.cardcrawl.characters.AbstractPlayer player =
                com.megacrit.cardcrawl.core.CardCrawlGame.characterManager.getCharacter(
                    CHARACTER_CLASSES[selectedCharacterIndex]
                );

            if (player != null && player.getPrefs() != null) {
                player.getPrefs().putInteger("ASCENSION_LEVEL", editingLevel);
                player.getPrefs().flush();
                logger.info("Applied level " + editingLevel + " to " + CHARACTER_NAMES[selectedCharacterIndex]);
            }
        } catch (Exception e) {
            logger.error("Failed to apply level to character", e);
        }
    }

    /**
     * Apply the selected ascension level to all characters
     */
    private static void applyAscensionLevelToAllCharacters(int level) {
        logger.info("Applying ascension level " + level + " to all characters...");

        try {
            com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass[] allClasses =
                com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass.values();

            for (com.megacrit.cardcrawl.characters.AbstractPlayer.PlayerClass playerClass : allClasses) {
                try {
                    com.megacrit.cardcrawl.characters.AbstractPlayer player =
                        com.megacrit.cardcrawl.core.CardCrawlGame.characterManager.getCharacter(playerClass);

                    if (player != null) {
                        com.megacrit.cardcrawl.helpers.Prefs prefs = player.getPrefs();
                        if (prefs != null) {
                            prefs.putInteger("ASCENSION_LEVEL", level);
                            prefs.flush();
                            logger.info("Set " + playerClass.name() + " to ascension " + level);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Failed to set ascension level for " + playerClass.name(), e);
                }
            }

            logger.info("All characters set to ascension " + level + "! Please restart the game.");

        } catch (Exception e) {
            logger.error("Failed to apply ascension level", e);
        }
    }
}
