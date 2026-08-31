package org.aventyrs.core.ability;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.TemporaryEffect;

/**
 * An ability the holder must actively spend Pontos de Ação/Magia to trigger, lasting a fixed
 * number of Rodadas. This is the general active-ability contract used by character-acquired
 * abilities such as {@link FocusAbility#CONCENTRACAO_PROFUNDA}.
 */
public interface ActiveAbility {
    String getDescription();

    /** Pontos de Ação spent to trigger this ability's activated state. */
    int getActionPointCost();

    /** Pontos de Magia spent to trigger this ability's activated state. */
    int getMagicPointCost();

    /** How many Rodadas the activated state lasts once triggered. */
    int getDurationInRounds();

    /**
     * The {@link TemporaryEffect} this ability grants once activated, computed from the
     * character's own current stats and not yet applied to any sheet.
     */
    TemporaryEffect resolveEffect(Character character);
}
