package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;

/**
 * Orchestrates casting a Magia, which per the rules always involves two separate rolls: the
 * Perícia that actually delivers the spell (e.g. Ataque à Distância for a ranged spell,
 * Ataque Corpo-a-Corpo for a Toque spell) compared against the target's own GD, followed by a
 * Domínio do Mana roll compared against the Magia's own GD.
 *
 * <p>TODO: no {@code Magia} entity/list exists yet, so {@code castSpell} only computes both
 * rolls' bonuses/difficultyReductions — it doesn't yet know either roll's target GD, so it
 * can't resolve success/failure for either roll. Once Magias (and their GDs) exist, this is
 * also where {@code DominioDoManaCompetencyAbility.FEITICEIRO}'s -1 GD (which reduces the
 * *delivery* roll's GD, not Domínio do Mana's own) and {@code AtaqueADistanciaExcellency
 * .LENDA}'s "bônus de conjuração de Habilidades e de Itens" should be wired in.
 */
public interface SpellCastingService {
    SpellCastingResult castSpell(CharacterSheet target, Interaction<CharacterSheet> deliveryInteraction);
}
