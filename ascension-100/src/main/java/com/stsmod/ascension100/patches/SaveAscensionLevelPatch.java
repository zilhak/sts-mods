package com.stsmod.ascension100.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 게임 시작 시 현재 승천 레벨을 자동으로 저장
 * 다음 게임 시작 시 마지막 승천 레벨이 자동으로 선택됨
 */
public class SaveAscensionLevelPatch {
    private static final Logger logger = LogManager.getLogger(SaveAscensionLevelPatch.class.getName());

    @SpirePatch(
        clz = CharacterSelectScreen.class,
        method = "updateButtons"
    )
    public static class SaveOnGameStart {
        @SpirePostfixPatch
        public static void Postfix(CharacterSelectScreen __instance) {
            // Confirm 버튼이 클릭되고 게임이 시작되려는 시점
            if (__instance.confirmButton.hb.clicked &&
                __instance.isAscensionMode) {

                // 선택된 캐릭터 찾기
                for (CharacterOption option : __instance.options) {
                    if (option.selected) {
                        // 현재 승천 레벨 저장
                        option.saveChosenAscensionLevel(__instance.ascensionLevel);

                        logger.info(String.format(
                            "Saved Ascension Level %d for %s",
                            __instance.ascensionLevel,
                            option.c.chosenClass.name()
                        ));
                        break;
                    }
                }
            }
        }
    }
}
