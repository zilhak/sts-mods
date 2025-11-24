package com.stsmod.ascension100.patches.levels;

/**
 * Ascension Level 39: Reduced maximum health
 *
 * 최대 체력이 감소합니다.
 *
 * 게임 시작시 최대 체력이 추가로 5 감소합니다.
 *
 * IMPLEMENTATION NOTE:
 * This level's HP modification is implemented in PlayerHPInitPatch.java
 * along with Level 24 and Level 32 to ensure correct execution order.
 *
 * @see com.stsmod.ascension100.patches.levels.PlayerHPInitPatch
 */
public class Level39 {
    // Implementation in PlayerHPInitPatch.java
}
