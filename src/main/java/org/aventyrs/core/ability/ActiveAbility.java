package org.aventyrs.core.ability;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;

/**
 * An ability the holder must actively spend Pontos de Ação/Magia/Vida to trigger, lasting a
 * fixed number of Rodadas. This is the general active-ability contract used by character-acquired
 * abilities such as {@link FocusAbility#CONCENTRACAO_PROFUNDA} and by a Talento-granted Poder
 * Vampírico (see {@code org.aventyrs.core.feat.VampiricoFeat} / {@code
 * org.aventyrs.core.feat.PoderVampiricoActiveAbility}).
 */
public interface ActiveAbility {
    String getDescription();

    /** Pontos de Ação spent to trigger this ability's activated state — 0 for an Ação Livre. */
    int getActionPointCost();

    /** Pontos de Magia spent to trigger this ability's activated state. */
    int getMagicPointCost();

    /**
     * Pontos de Vida spent to trigger this ability's activated state — 0 for most abilities; a
     * Poder Vampírico "consome 3PV cada". {@code ActiveAbilityService#activate} refuses to let
     * the holder spend down to 0 or below.
     */
    default int getHitPointCost() {
        return 0;
    }

    /** How many Rodadas the activated state lasts once triggered. */
    int getDurationInRounds();

    /**
     * The {@link TemporaryEffect} this ability grants once activated, computed from the
     * character's own current stats and not yet applied to any sheet. Kept for the
     * single-effect case; {@link #resolveEffects(Character)} is what {@code ActiveAbilityService}
     * actually applies.
     */
    TemporaryEffect resolveEffect(Character character);

    /**
     * Every {@link TemporaryEffect} this ability grants once activated — defaults to the one
     * {@link #resolveEffect(Character)} returns. An ability whose rules text grants more than one
     * distinct buff at once (a Poder Vampírico raising both PA and Movimento, say) overrides
     * this instead.
     */
    default List<TemporaryEffect> resolveEffects(final Character character) {
        return List.of(resolveEffect(character));
    }
}
