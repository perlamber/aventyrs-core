package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.SkillExcellency;

public interface MovementService {

    /**
     * Total Movimento Base in UD (Unidades de Distância) available on the given Turn: {@link
     * SizeCategory#getMovementPerActionPoint()} (via {@link CharacterSizeService
     * #getEffectiveSizeCategory}, so a size-shifting ability like Sangue de Gigante is already
     * reflected) times {@link ActionPointsService#getMaxActionPoints}'s own Turn-scoped total —
     * i.e. how far the character could move if every Ponto de Ação that Turn were spent moving
     * — plus any {@link org.aventyrs.core.modifier.ModifierType#MOVEMENT} bonus found on
     * attributeAbilities, skillCompetencyAbilities (acquired <b>and</b> racial — see {@link
     * org.aventyrs.core.skill.SkillCompetencyAbility#allFor}; unlike {@link ReactionsService}/
     * {@link InitiativeService}, which predate that fix and still only scan the acquired list,
     * this newer service starts from the corrected combined one), or the unlocked {@link
     * SkillExcellency} tiers of every trained Perícia. Never negative — like Reações/Ações
     * Livres/RD/RA, this is a spendable-resource-like budget, not a signed comparative value
     * like Iniciativa.
     *
     * <p>This is the <em>permanent</em> total only. A caller wanting what's actually available
     * this Round also adds {@link CharacterSheet#getTemporaryBonus}({@code
     * ModifierType.MOVEMENT}) on top — the same combination {@code AbstractSkillInteraction}
     * already performs for {@code skillRollBonus} — since a Round-scoped grant (e.g. {@code
     * InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s +2UD while its holder's group holds
     * initiative) lives on the {@code CharacterSheet}, not the {@code Character}, the same
     * "permanent ability vs. granted-by-someone-else's-action" split every other stat here
     * already draws.
     */
    int getTotalMovement(Character character, int turnNumber);
}
