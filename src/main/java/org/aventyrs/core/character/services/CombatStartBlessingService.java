package org.aventyrs.core.character.services;

import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * The "combat just started" trigger. This core has no combat observer — nothing fires when a
 * Cena turns into a Cena de Combate — so, like {@link DefeatBlessingService} and session
 * recovery, the <b>caller drives it</b>: whoever flips the Scene to a Cena de Combate (the same
 * caller that calls {@code Scene#setCombatScene(true)}) then calls {@link
 * #applyCombatStartBlessings} on each participant.
 *
 * <p>Scans {@code combatant}'s {@code Feat}s for {@code Feat#resolveCombatStartBlessings}
 * contributions ({@code AnaoFeat#VIGOR_DO_INVERNO}) and applies each as a {@code TemporaryBonus}
 * on {@code combatant} itself — every such Blessing is {@code TargetScope#SELF}.
 */
public interface CombatStartBlessingService {

    /**
     * Grants {@code combatant} every {@link Blessing} its held Talentos produce for the start of
     * a combat, and returns them.
     */
    List<Blessing> applyCombatStartBlessings(CombatantSheet combatant);
}
